package be.delomid.oneapp.mschat.mschat.service;

import be.delomid.oneapp.mschat.mschat.dto.ChannelDto;
import be.delomid.oneapp.mschat.mschat.dto.CreateChannelRequest;
import be.delomid.oneapp.mschat.mschat.dto.ResidentDto;
import be.delomid.oneapp.mschat.mschat.interceptor.JwtWebSocketInterceptor;
import be.delomid.oneapp.mschat.mschat.exception.ChannelNotFoundException;
import be.delomid.oneapp.mschat.mschat.exception.UnauthorizedAccessException;
import be.delomid.oneapp.mschat.mschat.interceptor.JwtWebSocketInterceptor;
import be.delomid.oneapp.mschat.mschat.model.*;
import be.delomid.oneapp.mschat.mschat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ResidentBuildingRepository residentBuildingRepository;

    @Transactional
    public ChannelDto createChannel(CreateChannelRequest request, String createdBy) {
        log.debug("Creating channel: {} by user: {}", request.getName(), createdBy);

        // Vérifier les permissions selon le type de canal
        if (request.getType() == ChannelType.GROUP ||
                request.getType() == ChannelType.BUILDING ||
                request.getType() == ChannelType.BUILDING_GROUP ||
                request.getType() == ChannelType.PUBLIC) {
            // Seuls les admins peuvent créer ces types de canaux
            validateAdminAccess(createdBy);
        }

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

    @Transactional
    public ChannelDto addMemberToChannel(Long channelId, String memberIdToAdd, String adminId) {
        log.debug("Adding member {} to channel {} by admin {}", memberIdToAdd, channelId, adminId);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found: " + channelId));

        // Vérifier que l'utilisateur est admin du canal
        validateChannelAdminAccess(channelId, adminId);

        // Vérifier que le membre à ajouter est du même immeuble
        validateSameBuildingAccess(adminId, memberIdToAdd);

        // Vérifier si l'utilisateur n'est pas déjà membre
        Optional<ChannelMember> existingMember = channelMemberRepository
                .findByChannelIdAndUserId(channelId, memberIdToAdd);

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
            addChannelMember(channel, memberIdToAdd, MemberRole.MEMBER);
        }

        return convertToDto(channel, adminId);
    }

    @Transactional
    public void removeMemberFromChannel(Long channelId, String memberIdToRemove, String adminId) {
        // Vérifier que l'utilisateur est admin du canal
        validateChannelAdminAccess(channelId, adminId);

        ChannelMember member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, memberIdToRemove)
                .orElseThrow(() -> new UnauthorizedAccessException("User is not a member of this channel"));

        // Ne pas permettre de supprimer le propriétaire du canal
        if (member.getRole() == MemberRole.OWNER) {
            throw new IllegalStateException("Cannot remove channel owner");
        }

        member.setIsActive(false);
        member.setLeftAt(LocalDateTime.now());
        channelMemberRepository.save(member);

        log.debug("User {} removed from channel {} by admin {}", memberIdToRemove, channelId, adminId);
    }

    public Page<ChannelDto> getUserChannels(String userId, Pageable pageable) {
        log.info("Getting channels for user: {}", userId);

        // Récupérer le bâtiment actuel depuis le JWT
        String currentBuildingId = getCurrentBuildingFromContext();

        log.info("Current building from JWT: {}", currentBuildingId);

        if (currentBuildingId == null) {
            log.warn("Building ID not found in JWT context for user: {}, using database fallback", userId);
            // Fallback: récupérer depuis la base de données
            Resident user = residentRepository.findByEmail(userId)
                    .or(() -> residentRepository.findById(userId))
                    .orElseThrow(() -> new UnauthorizedAccessException("User not found"));
            currentBuildingId = getCurrentUserBuildingId(user);
            log.info("Building ID from database: {}", currentBuildingId);
        }

        // Récupérer l'utilisateur pour obtenir son ID réel
        Resident user = residentRepository.findByEmail(userId)
                .or(() -> residentRepository.findById(userId))
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));

        // Vérifier que le buildingId est défini
        if (currentBuildingId == null) {
            log.error("Building ID is null for user: {}. User must select a building.", userId);
            throw new UnauthorizedAccessException("No building selected. Please select a building first.");
        }

        // Utiliser l'ID utilisateur réel pour la requête avec filtrage par bâtiment
        log.info("Fetching channels for userId: {} and buildingId: {}", user.getIdUsers(), currentBuildingId);
        Page<Channel> channels = channelRepository.findChannelsByUserIdAndBuilding(user.getIdUsers(), currentBuildingId, pageable);
        log.info("Found {} channels for user {} in building {}", channels.getTotalElements(), userId, currentBuildingId);
        return channels.map(channel -> convertToDto(channel, userId));
    }

    public ChannelDto getChannelById(Long channelId, String userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found: " + channelId));

        // Vérifier que l'utilisateur est membre du canal
        validateChannelAccess(channel, userId);

        return convertToDto(channel, userId);
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

        return convertToDto(channel, userId);
    }

    @Transactional
    public void leaveChannel(Long channelId, String userId) {
        Optional<Resident> resident = residentRepository.findByEmail(userId);

        ChannelMember member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, resident.get().getIdUsers())
                .orElseThrow(() -> new UnauthorizedAccessException("User is not a member of this channel"));

        member.setIsActive(false);
        member.setLeftAt(LocalDateTime.now());
        channelMemberRepository.save(member);

        log.debug("User {} left channel {}", userId, channelId);
    }

    public Optional<ChannelDto> getOrCreateOneToOneChannel(String userId1, String userId2) {
        // Récupérer les utilisateurs
        Resident user1 = residentRepository.findByEmail(userId1)
                .or(() -> residentRepository.findById(userId1))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId1));
        Resident user2 = residentRepository.findByEmail(userId2)
                .or(() -> residentRepository.findById(userId2))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId2));

        // Vérifier que les deux utilisateurs sont dans le même bâtiment ACTUEL
        validateSameBuildingAccess(userId1, userId2);

        // Utiliser les IDs réels pour la recherche
        String realUserId1 = user1.getIdUsers();
        String realUserId2 = user2.getIdUsers();

        Optional<Channel> existingChannel = channelRepository.findOneToOneChannel(realUserId1, realUserId2);

        if (existingChannel.isPresent()) {
            return Optional.of(convertToDto(existingChannel.get(), userId1));
        }

        // Créer un nouveau canal one-to-one avec le nom de l'autre utilisateur
        CreateChannelRequest request = new CreateChannelRequest();
        request.setName(user2.getFname() + " " + user2.getLname());
        request.setType(ChannelType.ONE_TO_ONE);
        request.setIsPrivate(true);
        request.setMemberIds(List.of(realUserId2));

        ChannelDto channel = createChannel(request, userId1);
        return Optional.of(channel);
    }

    public List<ChannelDto> getBuildingChannels(String buildingId, String userId) {
        // Vérifier que l'utilisateur habite dans ce bâtiment
        validateUserBuildingAccess(userId, buildingId);

        List<Channel> channels = channelRepository.findByTypeAndBuildingId(ChannelType.BUILDING, buildingId);
        return channels.stream()
                .map(channel -> convertToDto(channel, userId))
                .collect(Collectors.toList());
    }

    public List<ResidentDto> getBuildingResidents(String buildingId, String userId) {
        log.debug("Getting building residents for buildingId: {} by user: {}", buildingId, userId);

        // Récupérer le bâtiment actuel depuis le JWT
        String currentBuildingId = getCurrentBuildingFromContext();

        log.debug("Current building from JWT: {}", currentBuildingId);

        if (currentBuildingId == null) {
            // Fallback: récupérer depuis la base de données
            Resident currentUser = residentRepository.findByEmail(userId)
                    .or(() -> residentRepository.findById(userId))
                    .orElseThrow(() -> new UnauthorizedAccessException("User not found"));
            currentBuildingId = getCurrentUserBuildingId(currentUser);
        }

        if (currentBuildingId == null) {
            throw new UnauthorizedAccessException("User is not assigned to any building");
        }

        // Récupérer UNIQUEMENT les résidents du bâtiment actuel
        List<ResidentBuilding> residentBuildings = residentBuildingRepository.findActiveByBuildingId(currentBuildingId);
        List<Resident> residents = residentBuildings.stream()
                .map(ResidentBuilding::getResident)
                .distinct()
                .collect(Collectors.toList());

        log.debug("Found {} residents in building {}", residents.size(), currentBuildingId);

        return residents.stream()
                .map(this::convertResidentToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChannelDto createBuildingChannel(String buildingId, String createdBy) {
        // Vérifier que l'utilisateur habite dans ce bâtiment
        validateUserBuildingAccess(createdBy, buildingId);

        // Vérifier qu'il n'existe pas déjà un canal pour ce bâtiment
        List<Channel> existingChannels = channelRepository.findByTypeAndBuildingId(ChannelType.BUILDING, buildingId);
        if (!existingChannels.isEmpty() && existingChannels.get(0).getIsActive()) {
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
                addChannelMember(channelEntity, resident.getIdUsers(), MemberRole.MEMBER);
            }
        }

        return channel;
    }

    private String getCurrentBuildingFromContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                // Vérifier si c'est un JwtPrincipal (WebSocket)
                if (authentication.getPrincipal() instanceof JwtWebSocketInterceptor.JwtPrincipal) {
                    JwtWebSocketInterceptor.JwtPrincipal principal = (JwtWebSocketInterceptor.JwtPrincipal) authentication.getPrincipal();
                    return principal.getBuildingId();
                }
                // Sinon extraire depuis les details (HTTP)
                Object details = authentication.getDetails();
                log.debug("Authentication details type: {}", details != null ? details.getClass().getName() : "null");
                if (details instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detailsMap = (Map<String, Object>) details;
                    log.debug("Details map keys: {}", detailsMap.keySet());
                    Object buildingId = detailsMap.get("buildingId");
                    if (buildingId != null) {
                        log.info("Building ID extracted from authentication details: {}", buildingId);
                        return buildingId.toString();
                    } else {
                        log.warn("buildingId key not found in authentication details");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract building from JWT context: {}", e.getMessage());
        }
        return null;
    }

    private String getCurrentUserBuildingId(Resident user) {
        try {
            // Chercher dans les relations ResidentBuilding en priorité
            List<ResidentBuilding> userBuildings = residentBuildingRepository.findActiveByResidentId(user.getIdUsers());
            if (!userBuildings.isEmpty()) {
                return userBuildings.get(0).getBuilding().getBuildingId();
            }

            // Fallback: Si l'utilisateur a un appartement, utiliser le bâtiment de l'appartement
            if (user.getApartment() != null && user.getApartment().getBuilding() != null) {
                return user.getApartment().getBuilding().getBuildingId();
            }
        } catch (Exception e) {
            log.warn("Error getting user building ID for user {}: {}", user.getIdUsers(), e.getMessage());
        }

        return null;
    }

    private void validateUserBuildingAccess(String userId, String buildingId) {
        Resident user = residentRepository.findByEmail(userId)
                .or(() -> residentRepository.findById(userId))
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));

        // Vérifier via ResidentBuilding en priorité
        List<ResidentBuilding> userBuildings = residentBuildingRepository.findActiveByResidentId(user.getIdUsers());
        boolean hasAccessViaResidentBuilding = userBuildings.stream()
                .anyMatch(rb -> rb.getBuilding().getBuildingId().equals(buildingId));

        if (hasAccessViaResidentBuilding) {
            return;
        }

        // Fallback: Vérifier par l'appartement
        try {
            Optional<Apartment> userApartment = apartmentRepository.findByResidentIdUsers(user.getIdUsers());
            if (userApartment.isPresent() && userApartment.get().getBuilding().getBuildingId().equals(buildingId)) {
                return;
            }
        } catch (Exception e) {
            log.warn("Error checking apartment access: {}", e.getMessage());
        }

        // Si admin du bâtiment, autoriser l'accès
        if (user.getRole() == UserRole.BUILDING_ADMIN && buildingId.equals(user.getManagedBuildingId())) {
            return;
        }

        // Si super admin, autoriser l'accès
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        log.debug("Access denied for user {} to building {}", userId, buildingId);
        throw new UnauthorizedAccessException("User does not live in this building");
    }

    private void validateChannelCreation(CreateChannelRequest request, String createdBy) {
        // Validation selon le type de canal
        if (request.getType() == ChannelType.ONE_TO_ONE) {
            // Vérifier que les deux utilisateurs sont dans le même immeuble
            if (request.getMemberIds() == null || request.getMemberIds().size() != 1) {
                throw new IllegalArgumentException("ONE_TO_ONE channels require exactly one other member");
            }
            validateSameBuildingAccess(createdBy, request.getMemberIds().get(0));
        } else if (request.getType() == ChannelType.GROUP) {
            // Les admins peuvent créer des groupes
            if (request.getMemberIds() != null) {
                for (String memberId : request.getMemberIds()) {
                    validateSameBuildingAccess(createdBy, memberId);
                }
            }
        } else if (request.getType() == ChannelType.BUILDING) {
            if (request.getBuildingId() == null) {
                throw new IllegalArgumentException("Building ID is required for BUILDING channels");
            }
            // Vérifier si un canal pour ce bâtiment existe déjà
            List<Channel> existing = channelRepository
                    .findByTypeAndBuildingId(ChannelType.BUILDING, request.getBuildingId());
            // Vérifier que l'utilisateur habite dans ce bâtiment
            validateUserBuildingAccess(createdBy, request.getBuildingId());
            if (!existing.isEmpty() && existing.get(0).getIsActive()) {
                throw new IllegalArgumentException("A channel for this building already exists");
            }
        } else if (request.getType() == ChannelType.BUILDING_GROUP) {
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

    private void validateAdminAccess(String userId) {
        Resident user = residentRepository.findByEmail(userId)
                .or(() -> residentRepository.findById(userId))
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));

        if (user.getRole() != UserRole.BUILDING_ADMIN &&
                user.getRole() != UserRole.GROUP_ADMIN &&
                user.getRole() != UserRole.SUPER_ADMIN) {
            throw new UnauthorizedAccessException("Only admins can create this type of channel");
        }
    }

    private void validateChannelAdminAccess(Long channelId, String userId) {
        Resident user = residentRepository.findByEmail(userId)
                .or(() -> residentRepository.findById(userId))
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));

        // Super admin a accès à tout
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        // Vérifier si l'utilisateur est admin/owner du canal
        ChannelMember member = channelMemberRepository
                .findByChannelIdAndUserId(channelId, user.getIdUsers())
                .orElseThrow(() -> new UnauthorizedAccessException("User is not a member of this channel"));

        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN &&
                user.getRole() != UserRole.BUILDING_ADMIN) {
            throw new UnauthorizedAccessException("User does not have admin access to this channel");
        }
    }

    private void validateSameBuildingAccess(String adminId, String memberId) {
        Resident user1 = residentRepository.findByEmail(adminId)
                .or(() -> residentRepository.findById(adminId))
                .orElseThrow(() -> new UnauthorizedAccessException("User1 not found"));

        Resident user2 = residentRepository.findById(memberId)
                .or(() -> residentRepository.findByEmail(memberId))
                .orElseThrow(() -> new UnauthorizedAccessException("User2 not found"));

        // Super admin peut créer des discussions avec n'importe qui
        if (user1.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        // Utiliser le bâtiment actuel depuis le JWT pour user1
        String user1BuildingId = getCurrentBuildingFromContext();
        if (user1BuildingId == null) {
            user1BuildingId = getCurrentUserBuildingId(user1);
        }

        // Pour user2, utiliser la méthode sécurisée
        String user2BuildingId = getCurrentUserBuildingId(user2);

        if (user1BuildingId == null || user2BuildingId == null ||
                !user1BuildingId.equals(user2BuildingId)) {
            throw new UnauthorizedAccessException("Users can only create discussions with residents from the same building");
        }
    }

    private void validateChannelAccess(Channel channel, String userId) {
        Optional<Resident> user = residentRepository.findByEmail(userId);

        Optional<ChannelMember> member = channelMemberRepository
                .findByChannelIdAndUserId(channel.getId(), user.get().getIdUsers());

        if (member.isEmpty() || !member.get().getIsActive()) {
            throw new UnauthorizedAccessException("User does not have access to this channel");
        }
    }

    private void addChannelMember(Channel channel, String userId, MemberRole role) {
        // Résoudre l'ID utilisateur si c'est un email
        String realUserId = userId;
        if (userId.contains("@")) {
            Resident user = residentRepository.findByEmail(userId)
                    .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + userId));
            realUserId = user.getIdUsers();
        }

        ChannelMember member = ChannelMember.builder()
                .channel(channel)
                .userId(realUserId)
                .role(role)
                .build();

        channelMemberRepository.save(member);
        log.debug("Added user {} as {} to channel {}", realUserId, role, channel.getId());
    }

    private ChannelDto convertToDto(Channel channel) {
        return convertToDto(channel, null);
    }

    private ChannelDto convertToDto(Channel channel, String currentUserId) {
        Long memberCount = channelMemberRepository.countActiveByChannelId(channel.getId());

        String displayName = channel.getName();

        // Pour les canaux ONE_TO_ONE, afficher le nom de l'autre utilisateur
        if (channel.getType() == ChannelType.ONE_TO_ONE && currentUserId != null) {
            List<ChannelMember> members = channelMemberRepository.findActiveByChannelId(channel.getId());
            for (ChannelMember member : members) {
                if (!member.getUserId().equals(currentUserId)) {
                    Optional<Resident> otherUser = residentRepository.findById(member.getUserId());
                    if (otherUser.isPresent()) {
                        displayName = otherUser.get().getFname() + " " + otherUser.get().getLname();
                        break;
                    }
                }
            }
        }

        return ChannelDto.builder()
                .id(channel.getId())
                .name(displayName)
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

        try {
            if (resident.getApartment() != null) {
                apartmentId = resident.getApartment().getIdApartment();
                if (resident.getApartment().getBuilding() != null) {
                    buildingId = resident.getApartment().getBuilding().getBuildingId();
                }
            }
        } catch (Exception e) {
            log.warn("Error accessing apartment/building for resident {}: {}", resident.getIdUsers(), e.getMessage());
        }

        return ResidentDto.builder()
                .idUsers(resident.getIdUsers())
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