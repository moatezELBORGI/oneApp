package be.delomid.oneapp.mschat.mschat.service;


import be.delomid.oneapp.mschat.mschat.dto.MessageDto;
import be.delomid.oneapp.mschat.mschat.dto.SendMessageRequest;
import be.delomid.oneapp.mschat.mschat.dto.FileAttachmentDto;
import be.delomid.oneapp.mschat.mschat.exception.ChannelNotFoundException;
import be.delomid.oneapp.mschat.mschat.exception.UnauthorizedAccessException;
import be.delomid.oneapp.mschat.mschat.model.*;
import be.delomid.oneapp.mschat.mschat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ResidentRepository residentRepository;
    private final FileAttachmentRepository fileAttachmentRepository;

    @Transactional
    public MessageDto sendMessage(SendMessageRequest request, String senderId) {
        log.debug("Sending message to channel {} from user {}", request.getChannelId(), senderId);

        // Vérifier que le canal existe
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found: " + request.getChannelId()));

        // Vérifier que l'utilisateur est membre du canal et peut écrire
        validateWriteAccess(request.getChannelId(), senderId);

        // Récupérer le fichier attaché si présent
        FileAttachment fileAttachment = null;
        if (request.getFileAttachmentId() != null) {
            fileAttachment = fileAttachmentRepository.findById(request.getFileAttachmentId())
                    .orElseThrow(() -> new IllegalArgumentException("File attachment not found: " + request.getFileAttachmentId()));

            // Vérifier que l'utilisateur est le propriétaire du fichier
            if (!fileAttachment.getUploadedBy().equals(senderId)) {
                throw new IllegalArgumentException("User does not own this file attachment");
            }
        }

        // Pour les messages avec fichiers, le contenu peut être vide
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            if (fileAttachment != null) {
                // Pour les images, utiliser l'URL complète
                if (fileAttachment.getFileType() == FileType.IMAGE) {
                    content = "http://localhost:9090/api/v1/files/" + fileAttachment.getStoredFilename();
                } else {
                    content = fileAttachment.getOriginalFilename();
                }
            } else {
                throw new IllegalArgumentException("Message content or file attachment is required");
            }
        }
        
        Message message = Message.builder()
                .channel(channel)
                .senderId(senderId)
                .content(content)
                .type(request.getType())
                .replyToId(request.getReplyToId())
                .fileAttachment(fileAttachment)
                .build();

        message = messageRepository.save(message);
        log.debug("Message saved with ID: {}", message.getId());

        return convertToDto(message);
    }

    public Page<MessageDto> getChannelMessages(Long channelId, String userId, Pageable pageable) {
        log.debug("Getting messages for channel {} for user {}", channelId, userId);

        // Vérifier l'accès au canal
        validateChannelAccess(channelId, userId);

        Page<Message> messages = messageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable);
        return messages.map(this::convertToDto);
    }

    @Transactional
    public MessageDto editMessage(Long messageId, String content, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Vérifier que l'utilisateur est l'auteur du message
        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedAccessException("User can only edit their own messages");
        }

        message.setContent(content);
        message.setIsEdited(true);
        message = messageRepository.save(message);

        return convertToDto(message);
    }

    @Transactional
    public void deleteMessage(Long messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Vérifier que l'utilisateur est l'auteur du message ou admin du canal
        if (!message.getSenderId().equals(userId) && !isChannelAdmin(message.getChannel().getId(), userId)) {
            throw new UnauthorizedAccessException("User can only delete their own messages");
        }

        message.setIsDeleted(true);
        message.setContent("[Message deleted]");
        messageRepository.save(message);

        log.debug("Message {} deleted by user {}", messageId, userId);
    }

    private void validateChannelAccess(Long channelId, String userId) {
        Optional<Resident> resident=residentRepository.findByEmail(userId);

        Optional<ChannelMember> member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, resident.get().getIdUsers());

        if (member.isEmpty() || !member.get().getIsActive()) {
            throw new UnauthorizedAccessException("User does not have access to this channel");
        }
    }

    private void validateWriteAccess(Long channelId, String userId) {
        log.info("userid mn message service:"+userId);
        Optional<Resident> resident=residentRepository.findByEmail(userId);
        ChannelMember member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, resident.get().getIdUsers())
                .orElseThrow(() -> new UnauthorizedAccessException("User is not a member of this channel"));

        if (!member.getIsActive() || !member.getCanWrite()) {
            throw new UnauthorizedAccessException("User does not have write access to this channel");
        }
    }

    private boolean isChannelAdmin(Long channelId, String userId) {
        return channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .map(member -> member.getRole().name().contains("ADMIN") || member.getRole().name().contains("OWNER"))
                .orElse(false);
    }

    private MessageDto convertToDto(Message message) {
        FileAttachmentDto fileAttachmentDto = null;
        if (message.getFileAttachment() != null) {
            FileAttachment file = message.getFileAttachment();
            String baseUrl = "http://localhost:9090/api/v1/files/";

            fileAttachmentDto = FileAttachmentDto.builder()
                    .id(file.getId())
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(file.getStoredFilename())
                    .filePath(file.getFilePath())
                    .downloadUrl(baseUrl + "download/" + file.getStoredFilename())
                    .fileSize(file.getFileSize())
                    .mimeType(file.getMimeType())
                    .fileType(file.getFileType())
                    .uploadedBy(file.getUploadedBy())
                    .duration(file.getDuration())
                    .thumbnailPath(file.getThumbnailPath())
                    .thumbnailUrl(file.getThumbnailPath() != null ?
                            baseUrl + file.getThumbnailPath() : null)
                    .createdAt(file.getCreatedAt())
                    .build();
        }

        return MessageDto.builder()
                .id(message.getId())
                .channelId(message.getChannel().getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .replyToId(message.getReplyToId())
                .fileAttachment(fileAttachmentDto)
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}