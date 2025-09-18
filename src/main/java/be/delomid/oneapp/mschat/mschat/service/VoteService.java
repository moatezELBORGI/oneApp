package be.delomid.oneapp.mschat.mschat.service;

import be.delomid.oneapp.mschat.mschat.dto.*;
import be.delomid.oneapp.mschat.mschat.exception.UnauthorizedAccessException;
import be.delomid.oneapp.mschat.mschat.model.*;
import be.delomid.oneapp.mschat.mschat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {
    
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final UserVoteRepository userVoteRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ResidentRepository residentRepository;
    
    @Transactional
    public VoteDto createVote(CreateVoteRequest request, String createdBy) {
        log.debug("Creating vote: {} by user: {}", request.getTitle(), createdBy);
        
        // Vérifier que le canal existe
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + request.getChannelId()));
        
        // Vérifier que l'utilisateur est admin du canal
        validateChannelAdminAccess(request.getChannelId(), createdBy);
        
        // Créer le vote
        Vote vote = Vote.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .channel(channel)
                .createdBy(createdBy)
                .voteType(request.getVoteType())
                .isAnonymous(request.getIsAnonymous())
                .endDate(request.getEndDate())
                .build();
        
        vote = voteRepository.save(vote);
        
        // Créer les options
        for (String optionText : request.getOptions()) {
            VoteOption option = VoteOption.builder()
                    .text(optionText)
                    .vote(vote)
                    .build();
            voteOptionRepository.save(option);
        }
        
        return convertToDto(vote, createdBy);
    }
    
    @Transactional
    public void submitVote(VoteRequest request, String userId) {
        Vote vote = voteRepository.findById(request.getVoteId())
                .orElseThrow(() -> new IllegalArgumentException("Vote not found: " + request.getVoteId()));
        
        // Vérifier que le vote est actif
        if (!vote.getIsActive()) {
            throw new IllegalStateException("Vote is not active");
        }
        
        // Vérifier que le vote n'est pas expiré
        if (vote.getEndDate() != null && vote.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Vote has expired");
        }
        
        // Vérifier que l'utilisateur est membre du canal
        validateChannelMemberAccess(vote.getChannel().getId(), userId);
        
        // Vérifier que l'utilisateur n'a pas déjà voté
        if (userVoteRepository.existsByVoteIdAndUserId(request.getVoteId(), userId)) {
            throw new IllegalStateException("User has already voted");
        }
        
        // Vérifier le nombre d'options selon le type de vote
        if (vote.getVoteType() == VoteType.SINGLE_CHOICE && request.getSelectedOptionIds().size() > 1) {
            throw new IllegalArgumentException("Only one option can be selected for single choice vote");
        }
        
        // Enregistrer les votes
        for (Long optionId : request.getSelectedOptionIds()) {
            VoteOption option = voteOptionRepository.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("Vote option not found: " + optionId));
            
            if (!option.getVote().getId().equals(request.getVoteId())) {
                throw new IllegalArgumentException("Vote option does not belong to this vote");
            }
            
            UserVote userVote = UserVote.builder()
                    .vote(vote)
                    .voteOption(option)
                    .userId(userId)
                    .build();
            
            userVoteRepository.save(userVote);
        }
        
        log.debug("User {} voted on vote {}", userId, request.getVoteId());
    }
    
    public List<VoteDto> getChannelVotes(Long channelId, String userId) {
        // Vérifier l'accès au canal
        validateChannelMemberAccess(channelId, userId);
        
        List<Vote> votes = voteRepository.findByChannelIdOrderByCreatedAtDesc(channelId);
        return votes.stream()
                .map(vote -> convertToDto(vote, userId))
                .collect(Collectors.toList());
    }
    
    public VoteDto getVoteById(Long voteId, String userId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found: " + voteId));
        
        // Vérifier l'accès au canal
        validateChannelMemberAccess(vote.getChannel().getId(), userId);
        
        return convertToDto(vote, userId);
    }
    
    @Transactional
    public VoteDto closeVote(Long voteId, String userId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found: " + voteId));
        
        // Vérifier que l'utilisateur est admin du canal ou créateur du vote
        if (!vote.getCreatedBy().equals(userId)) {
            validateChannelAdminAccess(vote.getChannel().getId(), userId);
        }
        
        vote.setIsActive(false);
        vote = voteRepository.save(vote);
        
        log.debug("Vote {} closed by user {}", voteId, userId);
        return convertToDto(vote, userId);
    }
    
    // Fermer automatiquement les votes expirés
    @Scheduled(fixedRate = 60000) // Toutes les minutes
    @Transactional
    public void closeExpiredVotes() {
        List<Vote> expiredVotes = voteRepository.findExpiredActiveVotes(LocalDateTime.now());
        for (Vote vote : expiredVotes) {
            vote.setIsActive(false);
            voteRepository.save(vote);
            log.debug("Vote {} automatically closed due to expiration", vote.getId());
        }
    }
    
    private void validateChannelAdminAccess(Long channelId, String userId) {
        // Récupérer l'utilisateur
        Resident user = residentRepository.findByEmail(userId)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));
        
        // Vérifier si l'utilisateur est admin du bâtiment ou super admin
        if (user.getRole() == UserRole.BUILDING_ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        
        // Vérifier si l'utilisateur est admin/owner du canal
        ChannelMember member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, user.getIdUsers())
                .orElseThrow(() -> new UnauthorizedAccessException("User is not a member of this channel"));
        
        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedAccessException("User does not have admin access to this channel");
        }
    }
    
    private void validateChannelMemberAccess(Long channelId, String userId) {
        Resident user = residentRepository.findByEmail(userId)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));
        
        ChannelMember member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, user.getIdUsers())
                .orElseThrow(() -> new UnauthorizedAccessException("User is not a member of this channel"));
        
        if (!member.getIsActive()) {
            throw new UnauthorizedAccessException("User does not have access to this channel");
        }
    }
    
    private VoteDto convertToDto(Vote vote, String userId) {
        // Calculer les statistiques des options
        List<VoteOptionDto> optionDtos = vote.getOptions().stream()
                .map(option -> {
                    Long voteCount = voteOptionRepository.countVotesByOptionId(option.getId());
                    Long totalVotes = userVoteRepository.countByVoteId(vote.getId());
                    Double percentage = totalVotes > 0 ? (voteCount.doubleValue() / totalVotes.doubleValue()) * 100 : 0.0;
                    
                    return VoteOptionDto.builder()
                            .id(option.getId())
                            .text(option.getText())
                            .voteCount(voteCount)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Vérifier si l'utilisateur a voté
        Resident user = residentRepository.findByEmail(userId).orElse(null);
        Boolean hasVoted = user != null && userVoteRepository.existsByVoteIdAndUserId(vote.getId(), user.getIdUsers());
        
        Long totalVotes = userVoteRepository.countByVoteId(vote.getId());
        
        return VoteDto.builder()
                .id(vote.getId())
                .title(vote.getTitle())
                .description(vote.getDescription())
                .channelId(vote.getChannel().getId())
                .createdBy(vote.getCreatedBy())
                .voteType(vote.getVoteType())
                .isActive(vote.getIsActive())
                .isAnonymous(vote.getIsAnonymous())
                .endDate(vote.getEndDate())
                .options(optionDtos)
                .createdAt(vote.getCreatedAt())
                .updatedAt(vote.getUpdatedAt())
                .hasVoted(hasVoted)
                .totalVotes(totalVotes)
                .build();
    }
}