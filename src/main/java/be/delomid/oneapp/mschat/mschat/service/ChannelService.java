package be.delomid.oneapp.mschat.mschat.service;


import be.delomid.oneapp.mschat.mschat.dto.ChannelDto;
import be.delomid.oneapp.mschat.mschat.dto.CreateChannelRequest;
import be.delomid.oneapp.mschat.mschat.dto.ResidentDto;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final MessageRepository messageRepository;
    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;

    @Transactional
    public ChannelDto createChannel(CreateChannelRequest request, String createdBy) {
        log.debug("Creating channel: {} by user: {}", request.getName(), createdBy);

        // Vérifications spécifiques selon le type de canal
        validateChannelCreation(request, createdBy);

        Channel channel = Channel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .buildingId(request.getBuildingId())
                .buildingGroupId(request.getBuildingGroupId())
                .createdBy(createdBy)
                .isPrivate(request.getIsPrivate())
                .build();

        channel = channelRepository.save(channel);

        // Ajouter le créateur comme propriétaire
        addChannelMember(channel, createdBy, MemberRole.OWNER);

        // Ajouter les autres membres
        if (request.getMemberIds() != null) {
            for (String memberId : request.getMemberIds()) {
                if (!memberId.equals(createdBy)) {
                    addChannelMember(channel, memberId, MemberRole.MEMBER);
                }
            }
        }

        return convertToDto(channel);
    }

    public Page<ChannelDto> getUserChannels(String userId, Pageable pageable) {
        log.debug("Getting channels for user: {}", userId);
        
        Page<Channel> channels = channelRepository.findChannelsByUserId(userId, pageable);
        return channels.map(this::convertToDto);
    }

    public ChannelDto getChannelById(Long channelId, String userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found: " + channelId));

        // Vérifier que l'utilisateur est membre du canal
        validateChannelAccess(channel, userId);

        return convertToDto(channel);
    }

    @Transactional
    public ChannelDto joinChannel(Long channelId, String userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found: " + channelId));

        // Vérifier si l'utilisateur n'est pas déjà membre
        Optional<ChannelMember> existingMember = channelMemberRepository
                .findByChannelIdAndUserId(channelId, userId);

        if (existingMember.isPresent()) {
            if (existingMember.get().getIsActive()) {
                throw new IllegalStateException("User is already a member of this channel");
            } else {
                // Réactiver le membre
                existingMember.get().setIsActive(true);
                existingMember.get().setLeftAt(null);
                channelMemberRepository.save(existingMember.get());
            }
        } else {
            addChannelMember(channel, userId, MemberRole.MEMBER);
        }

        return convertToDto(channel);
    }

    @Transactional
    public void leaveChannel(Long channelId, String userId) {
        ChannelMember member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new UnauthorizedAccessException("User is not a member of this channel"));

        member.setIsActive(false);
        member.setLeftAt(LocalDateTime.now());
        channelMemberRepository.save(member);

        log.debug("User {} left channel {}", userId, channelId);
    }

    public Optional<ChannelDto> getOrCreateOneToOneChannel(String userId1, String userId2) {
        Optional<Channel> existingChannel = channelRepository.findOneToOneChannel(userId1, userId2);
        
        if (existingChannel.isPresent()) {
            return Optional.of(convertToDto(existingChannel.get()));
        }

        // Créer un nouveau canal one-to-one
        CreateChannelRequest request = new CreateChannelRequest();
        request.setName("Direct Message");
        request.setType(ChannelType.ONE_TO_ONE);
        request.setIsPrivate(true);
        request.setMemberIds(List.of(userId2));

        return Optional.of(createChannel(request, userId1));
    }

    public List<ChannelDto> getBuildingChannels(String buildingId, String userId) {
        // Vérifier que l'utilisateur habite dans ce bâtiment
        validateUserBuildingAccess(userId, buildingId);
        
        List<Channel> channels = channelRepository.findByTypeAndBuildingId(ChannelType.BUILDING, buildingId);
        return channels.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ResidentDto> getBuildingResidents(String buildingId, String userId) {
        // Vérifier que l'utilisateur habite dans ce bâtiment
        validateUserBuildingAccess(userId, buildingId);
        
        List<Resident> residents = residentRepository.findByBuildingId(buildingId);
        return residents.stream()
                .map(this::convertResidentToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChannelDto createBuildingChannel(String buildingId, String createdBy) {
        // Vérifier que l'utilisateur habite dans ce bâtiment
        validateUserBuildingAccess(createdBy, buildingId);
        
        // Vérifier qu'il n'existe pas déjà un canal pour ce bâtiment
        List<Channel> existingChannel = channelRepository.findByTypeAndBuildingId(ChannelType.BUILDING, buildingId);
        if (!existingChannel.isEmpty() && existingChannel.get(0).getIsActive() == true) {
            throw new IllegalArgumentException("A channel for this building already exists");
        }

        CreateChannelRequest request = new CreateChannelRequest();
        request.setName("Building " + buildingId + " Chat");
        request.setDescription("Canal de discussion pour l'immeuble " + buildingId);
        request.setType(ChannelType.BUILDING);
        request.setBuildingId(buildingId);
        request.setIsPrivate(false);

        ChannelDto channel = createChannel(request, createdBy);
        
        // Ajouter automatiquement tous les résidents du bâtiment
        List<Resident> residents = residentRepository.findByBuildingId(buildingId);
        Channel channelEntity = channelRepository.findById(channel.getId()).get();
        
        for (Resident resident : residents) {
            if (!resident.getIdUsers().equals(createdBy)) {
                addChannelMember(channelEntity, String.valueOf(resident.getIdUsers()), MemberRole.MEMBER);
            }
        }

        return channel;
    }

    private void validateUserBuildingAccess(String userId, String buildingId) {
        Optional<Apartment> userApartment = apartmentRepository.findByResidentIdUsers(userId);
        if (userApartment.isEmpty() || !userApartment.get().getBuilding().getBuildingId().equals(buildingId)) {
            throw new UnauthorizedAccessException("User does not live in this building");
        }
    }

    private void validateChannelCreation(CreateChannelRequest request, String createdBy) {
        // Validation pour les canaux building
        if (request.getType() == ChannelType.BUILDING) {
            if (request.getBuildingId() == null) {
                throw new IllegalArgumentException("Building ID is required for BUILDING channels");
            }
            // Vérifier si un canal pour ce bâtiment existe déjà
            List<Channel> existing = channelRepository
                    .findByTypeAndBuildingId(ChannelType.BUILDING, request.getBuildingId());
            // Vérifier que l'utilisateur habite dans ce bâtiment
            validateUserBuildingAccess(createdBy, request.getBuildingId());
            if (!existing.isEmpty() && existing.get(0).getIsActive() == true) {
                throw new IllegalArgumentException("A channel for this building already exists");
            }
        }

        // Validation pour les canaux building group
        if (request.getType() == ChannelType.BUILDING_GROUP) {
            if (request.getBuildingGroupId() == null) {
                throw new IllegalArgumentException("Building Group ID is required for BUILDING_GROUP channels");
            }
            Optional<Channel> existing = channelRepository
                    .findByTypeAndBuildingGroupId(ChannelType.BUILDING_GROUP, request.getBuildingGroupId());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("A channel for this building group already exists");
            }
        }
    }

    private void validateChannelAccess(Channel channel, String userId) {
        Optional<ChannelMember> member = channelMemberRepository
                .findByChannelIdAndUserId(channel.getId(), userId);
        
        if (member.isEmpty() || !member.get().getIsActive()) {
            throw new UnauthorizedAccessException("User does not have access to this channel");
        }
    }

    private void addChannelMember(Channel channel, String userId, MemberRole role) {
        ChannelMember member = ChannelMember.builder()
                .channel(channel)
                .userId(userId)
                .role(role)
                .build();
        
        channelMemberRepository.save(member);
        log.debug("Added user {} as {} to channel {}", userId, role, channel.getId());
    }

    private ChannelDto convertToDto(Channel channel) {
        Long memberCount = channelMemberRepository.countActiveByChannelId(channel.getId());
        
        return ChannelDto.builder()
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .type(channel.getType())
                .buildingId(channel.getBuildingId())
                .buildingGroupId(channel.getBuildingGroupId())
                .createdBy(channel.getCreatedBy())
                .isActive(channel.getIsActive())
                .isPrivate(channel.getIsPrivate())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .memberCount(memberCount)
                .build();
    }

    private ResidentDto convertResidentToDto(Resident resident) {
        String apartmentId = null;
        String buildingId = null;

        if (resident.getApartment() != null) {
            apartmentId = resident.getApartment().getIdApartment();
            buildingId = resident.getApartment().getBuilding().getBuildingId();
        }

        return ResidentDto.builder()
                .idUsers(String.valueOf(resident.getIdUsers()))
                .fname(resident.getFname())
                .lname(resident.getLname())
                .email(resident.getEmail())
                .phoneNumber(resident.getPhoneNumber())
                .picture(resident.getPicture())
                .apartmentId(apartmentId)
                .buildingId(buildingId)
                .createdAt(resident.getCreatedAt())
                .updatedAt(resident.getUpdatedAt())
                .build();
    }
}