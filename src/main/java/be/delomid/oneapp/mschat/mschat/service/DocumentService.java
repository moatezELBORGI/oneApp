package be.delomid.oneapp.mschat.mschat.service;

import be.delomid.oneapp.mschat.mschat.dto.CreateFolderRequest;
import be.delomid.oneapp.mschat.mschat.dto.DocumentDto;
import be.delomid.oneapp.mschat.mschat.dto.FolderDto;
import be.delomid.oneapp.mschat.mschat.dto.FolderPermissionDto;
import be.delomid.oneapp.mschat.mschat.model.*;
import be.delomid.oneapp.mschat.mschat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final ApartmentRepository apartmentRepository;
    private final ResidentRepository residentRepository;
    private final BuildingRepository buildingRepository;
    private final ResidentBuildingRepository residentBuildingRepository;
    private final FolderPermissionRepository folderPermissionRepository;

    @Value("${app.documents.base-dir:documents}")
    private String baseDocumentsDir;

    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    @Transactional
    public FolderDto createFolder(CreateFolderRequest request, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Immeuble non trouvé"));

        String userRole = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentUserRole();
        boolean isAdmin = "ADMIN".equals(userRole) || "SYNDIC".equals(userRole);

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElse(null);

        Apartment apartment = null;
        if (residentBuilding != null) {
            apartment = residentBuilding.getApartment();
        }

        if (!isAdmin && apartment == null) {
            throw new RuntimeException("Vous devez être admin ou avoir un appartement pour créer un dossier");
        }

        String cleanName = request.getName().trim();
        if (cleanName.isEmpty()) {
            throw new RuntimeException("Le nom du dossier ne peut pas être vide");
        }

        ShareType shareType = ShareType.PRIVATE;
        if (request.getShareType() != null) {
            try {
                shareType = ShareType.valueOf(request.getShareType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Type de partage invalide");
            }
        } else if (request.getIsShared() != null && request.getIsShared()) {
            shareType = ShareType.ALL_APARTMENTS;
        }

        if (shareType != ShareType.PRIVATE && !isAdmin && apartment != null) {
            shareType = ShareType.PRIVATE;
        }

        if (request.getParentFolderId() != null) {
            Folder parentFolder = folderRepository.findByIdAndBuildingId(
                    request.getParentFolderId(), buildingId)
                    .orElseThrow(() -> new RuntimeException("Dossier parent non trouvé"));

            if (folderRepository.existsByNameAndParentFolderIdAndBuildingId(
                    cleanName, request.getParentFolderId(), buildingId)) {
                throw new RuntimeException("Un dossier avec ce nom existe déjà à cet emplacement");
            }
        } else {
            if (folderRepository.existsByNameAndParentFolderIsNullAndBuildingId(
                    cleanName, buildingId)) {
                throw new RuntimeException("Un dossier avec ce nom existe déjà à la racine");
            }
        }

        String apartmentIdForPath = apartment != null ? apartment.getIdApartment() : "building_" + buildingId;
        String folderPath = buildFolderPath(apartmentIdForPath, request.getParentFolderId(), cleanName);

        try {
            Path physicalPath = Paths.get(baseDocumentsDir, folderPath);
            Files.createDirectories(physicalPath);
            log.info("Dossier physique créé: {}", physicalPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Erreur lors de la création du dossier physique: {}", folderPath, e);
            throw new RuntimeException("Échec de la création du dossier sur le système de fichiers: " + e.getMessage());
        }

        Folder parentFolder = null;
        if (request.getParentFolderId() != null) {
            parentFolder = folderRepository.findById(request.getParentFolderId())
                    .orElse(null);
        }

        boolean isShared = shareType != ShareType.PRIVATE;

        Folder folder = Folder.builder()
                .name(cleanName)
                .folderPath(folderPath)
                .parentFolder(parentFolder)
                .apartment(apartment)
                .building(building)
                .createdBy(resident.getIdUsers())
                .isShared(isShared)
                .shareType(shareType)
                .build();

        folder = folderRepository.save(folder);

        if (shareType == ShareType.SPECIFIC_APARTMENTS) {
            boolean allowUpload = request.getAllowUpload() != null ? request.getAllowUpload() : false;

            if (request.getSharedApartmentIds() != null && !request.getSharedApartmentIds().isEmpty()) {
                for (String apartmentId : request.getSharedApartmentIds()) {
                    Apartment sharedApartment = apartmentRepository.findById(apartmentId)
                            .orElse(null);
                    if (sharedApartment != null) {
                        FolderPermission permission = FolderPermission.builder()
                                .folder(folder)
                                .apartment(sharedApartment)
                                .canRead(true)
                                .canUpload(allowUpload)
                                .build();
                        folderPermissionRepository.save(permission);
                    }
                }
            }

            if (request.getSharedResidentIds() != null && !request.getSharedResidentIds().isEmpty()) {
                for (String residentId : request.getSharedResidentIds()) {
                    Resident sharedResident = residentRepository.findById(residentId)
                            .orElse(null);
                    if (sharedResident != null) {
                        FolderPermission permission = FolderPermission.builder()
                                .folder(folder)
                                .resident(sharedResident)
                                .canRead(true)
                                .canUpload(allowUpload)
                                .build();
                        folderPermissionRepository.save(permission);
                    }
                }
            }
        }

        log.info("Dossier créé: {} (ID: {}) - Type: {} - Immeuble: {}",
                folder.getName(), folder.getId(), shareType, buildingId);

        return mapToFolderDto(folder, resident.getIdUsers(), apartment != null ? apartment.getIdApartment() : null);
    }

    @Transactional(readOnly = true)
    public List<FolderDto> getRootFolders(String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElse(null);

        List<Folder> folders;
        String apartmentId = null;

        if (residentBuilding != null && residentBuilding.getApartment() != null) {
            apartmentId = residentBuilding.getApartment().getIdApartment();
            folders = folderRepository.findAccessibleRootFolders(buildingId, apartmentId, resident.getIdUsers());
        } else {
            folders = folderRepository.findAccessibleFoldersForAdminWithoutApartment(buildingId, resident.getIdUsers());
        }

        log.debug("Récupération de {} dossiers racine accessibles pour l'immeuble {}", folders.size(), buildingId);

        String finalApartmentId = apartmentId;
        return folders.stream()
                .filter(f -> f.getParentFolder() == null)
                .map(f -> mapToFolderDto(f, resident.getIdUsers(), finalApartmentId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderDto> getSubFolders(Long folderId, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElseThrow(() -> new RuntimeException("Vous n'avez pas d'appartement dans cet immeuble"));

        Apartment apartment = residentBuilding.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé dans cet immeuble");
        }

        Folder folder = folderRepository.findByIdAndBuildingAndApartment(folderId, buildingId, apartment.getIdApartment())
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé ou accès non autorisé"));

        log.debug("Récupération de {} sous-dossiers pour le dossier {} (ID: {})",
                folder.getSubFolders().size(), folder.getName(), folderId);

        return folder.getSubFolders().stream()
                .filter(subFolder -> subFolder.getIsShared() ||
                        (subFolder.getApartment() != null && subFolder.getApartment().getIdApartment().equals(apartment.getIdApartment())))
                .map(this::mapToFolderDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> getFolderDocuments(Long folderId, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElseThrow(() -> new RuntimeException("Vous n'avez pas d'appartement dans cet immeuble"));

        Apartment apartment = residentBuilding.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé dans cet immeuble");
        }

        Folder folder = folderRepository.findByIdAndBuildingAndApartment(folderId, buildingId, apartment.getIdApartment())
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé ou accès non autorisé"));

        List<Document> documents = documentRepository.findByFolderIdOrderByCreatedAtDesc(folderId);
        log.debug("Récupération de {} documents pour le dossier {} (ID: {})",
                documents.size(), folder.getName(), folderId);

        return documents.stream()
                .map(this::mapToDocumentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentDto uploadDocument(Long folderId, MultipartFile file, String description, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Immeuble non trouvé"));

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElseThrow(() -> new RuntimeException("Vous n'avez pas d'appartement dans cet immeuble"));

        Apartment apartment = residentBuilding.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé dans cet immeuble");
        }

        Folder folder = folderRepository.findByIdAndBuildingAndApartment(folderId, buildingId, apartment.getIdApartment())
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé ou accès non autorisé"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("La taille du fichier dépasse la limite autorisée (" + (maxFileSize / 1024 / 1024) + " MB)");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom du fichier est invalide");
            }

            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID().toString() + fileExtension;

            Path folderPhysicalPath = Paths.get(baseDocumentsDir, folder.getFolderPath());
            Files.createDirectories(folderPhysicalPath);

            Path filePath = folderPhysicalPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier physique sauvegardé: {}", filePath.toAbsolutePath());

            String relativePath = Paths.get(folder.getFolderPath(), storedFilename).toString();

            Document document = Document.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .filePath(relativePath)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .fileExtension(fileExtension)
                    .folder(folder)
                    .apartment(apartment)
                    .building(building)
                    .uploadedBy(resident.getIdUsers())
                    .description(description != null ? description.trim() : null)
                    .build();

            document = documentRepository.save(document);
            log.info("Document uploadé: {} (ID: {}) dans le dossier: {} pour appartement: {} (immeuble: {})",
                    originalFilename, document.getId(), folder.getName(), apartment.getIdApartment(), buildingId);

            return mapToDocumentDto(document);

        } catch (IOException e) {
            log.error("Erreur lors de l'upload du document: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de l'upload du document: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteFolder(Long folderId, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElseThrow(() -> new RuntimeException("Vous n'avez pas d'appartement dans cet immeuble"));

        Apartment apartment = residentBuilding.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé dans cet immeuble");
        }

        Folder folder = folderRepository.findByIdAndBuildingAndApartment(folderId, buildingId, apartment.getIdApartment())
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé ou accès non autorisé"));

        try {
            Path folderPath = Paths.get(baseDocumentsDir, folder.getFolderPath());
            if (Files.exists(folderPath)) {
                deleteDirectoryRecursively(folderPath);
                log.info("Dossier physique supprimé: {}", folderPath.toAbsolutePath());
            }

            folderRepository.delete(folder);
            log.info("Dossier supprimé: {} (ID: {}) pour immeuble: {}", folder.getName(), folderId, buildingId);

        } catch (IOException e) {
            log.error("Erreur lors de la suppression du dossier: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la suppression du dossier: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteDocument(Long documentId, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        Document document = documentRepository.findByIdAndBuildingId(documentId, buildingId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé ou accès non autorisé"));

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElseThrow(() -> new RuntimeException("Vous n'avez pas d'appartement dans cet immeuble"));

        Apartment apartment = residentBuilding.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé dans cet immeuble");
        }

        if (!document.getFolder().getIsShared() &&
            (document.getApartment() == null || !document.getApartment().getIdApartment().equals(apartment.getIdApartment()))) {
            throw new RuntimeException("Accès non autorisé à ce document");
        }

        try {
            Path filePath = Paths.get(baseDocumentsDir, document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Fichier physique supprimé: {}", filePath.toAbsolutePath());
            }

            documentRepository.delete(document);
            log.info("Document supprimé: {} (ID: {}) pour immeuble: {}", document.getOriginalFilename(), documentId, buildingId);

        } catch (IOException e) {
            log.error("Erreur lors de la suppression du document: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la suppression du document: " + e.getMessage());
        }
    }

    public byte[] downloadDocument(Long documentId, String email) throws IOException {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        Document document = documentRepository.findByIdAndBuildingId(documentId, buildingId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé ou accès non autorisé"));

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElseThrow(() -> new RuntimeException("Vous n'avez pas d'appartement dans cet immeuble"));

        Apartment apartment = residentBuilding.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé dans cet immeuble");
        }

        if (!document.getFolder().getIsShared() &&
            (document.getApartment() == null || !document.getApartment().getIdApartment().equals(apartment.getIdApartment()))) {
            throw new RuntimeException("Accès non autorisé à ce document");
        }

        Path filePath = Paths.get(baseDocumentsDir, document.getFilePath());
        if (!Files.exists(filePath)) {
            log.error("Fichier physique non trouvé: {}", filePath.toAbsolutePath());
            throw new RuntimeException("Fichier non trouvé sur le système de fichiers");
        }

        log.info("Téléchargement du document: {} (ID: {}) pour immeuble: {}",
                document.getOriginalFilename(), documentId, buildingId);
        return Files.readAllBytes(filePath);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> searchDocuments(String query, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        String buildingId = be.delomid.oneapp.mschat.mschat.util.SecurityContextUtil.getCurrentBuildingId();
        if (buildingId == null) {
            throw new RuntimeException("Aucun immeuble sélectionné");
        }

        ResidentBuilding residentBuilding = residentBuildingRepository
                .findByResidentIdAndBuildingId(resident.getIdUsers(), buildingId)
                .orElseThrow(() -> new RuntimeException("Vous n'avez pas d'appartement dans cet immeuble"));

        Apartment apartment = residentBuilding.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé dans cet immeuble");
        }

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<Document> documents = documentRepository.searchDocumentsByBuildingAndApartment(buildingId, apartment.getIdApartment(), query.trim());
        log.debug("Recherche '{}' a retourné {} documents pour immeuble {} et appartement {}",
                query, documents.size(), buildingId, apartment.getIdApartment());

        return documents.stream()
                .map(this::mapToDocumentDto)
                .collect(Collectors.toList());
    }

    private String buildFolderPath(String apartmentId, Long parentFolderId, String folderName) {
        if (parentFolderId != null) {
            Folder parentFolder = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            return Paths.get(parentFolder.getFolderPath(), folderName).toString();
        }
        return Paths.get("apartment_" + apartmentId, folderName).toString();
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteDirectoryRecursively(child);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.delete(path);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private FolderDto mapToFolderDto(Folder folder) {
        return mapToFolderDto(folder, null, null);
    }

    private FolderDto mapToFolderDto(Folder folder, String residentId, String apartmentId) {
        boolean canRead = checkFolderReadPermission(folder, residentId, apartmentId);
        boolean canUpload = checkFolderUploadPermission(folder, residentId, apartmentId);

        List<FolderPermissionDto> permissionDtos = folder.getPermissions().stream()
                .map(p -> FolderPermissionDto.builder()
                        .id(p.getId())
                        .apartmentId(p.getApartment() != null ? p.getApartment().getIdApartment() : null)
                        .residentId(p.getResident() != null ? p.getResident().getIdUsers() : null)
                        .canRead(p.getCanRead())
                        .canUpload(p.getCanUpload())
                        .build())
                .collect(Collectors.toList());

        return FolderDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .folderPath(folder.getFolderPath())
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
                .apartmentId(folder.getApartment() != null ? folder.getApartment().getIdApartment() : null)
                .buildingId(folder.getBuilding() != null ? folder.getBuilding().getBuildingId() : null)
                .createdBy(folder.getCreatedBy())
                .isShared(folder.getIsShared())
                .shareType(folder.getShareType().name())
                .createdAt(folder.getCreatedAt())
                .subFolderCount(folder.getSubFolders() != null ? folder.getSubFolders().size() : 0)
                .documentCount(folder.getDocuments() != null ? folder.getDocuments().size() : 0)
                .permissions(permissionDtos)
                .canRead(canRead)
                .canUpload(canUpload)
                .build();
    }

    private boolean checkFolderReadPermission(Folder folder, String residentId, String apartmentId) {
        if (folder.getCreatedBy().equals(residentId)) {
            return true;
        }

        if (folder.getShareType() == ShareType.ALL_APARTMENTS) {
            return true;
        }

        if (folder.getShareType() == ShareType.PRIVATE) {
            return folder.getApartment() != null && folder.getApartment().getIdApartment().equals(apartmentId);
        }

        if (folder.getShareType() == ShareType.SPECIFIC_APARTMENTS) {
            return folder.getPermissions().stream()
                    .anyMatch(p -> (p.getResident() != null && p.getResident().getIdUsers().equals(residentId)) ||
                                   (p.getApartment() != null && apartmentId != null && p.getApartment().getIdApartment().equals(apartmentId)));
        }

        return false;
    }

    private boolean checkFolderUploadPermission(Folder folder, String residentId, String apartmentId) {
        if (folder.getCreatedBy().equals(residentId)) {
            return true;
        }

        if (folder.getShareType() == ShareType.ALL_APARTMENTS) {
            return true;
        }

        if (folder.getShareType() == ShareType.PRIVATE) {
            return folder.getApartment() != null && folder.getApartment().getIdApartment().equals(apartmentId);
        }

        if (folder.getShareType() == ShareType.SPECIFIC_APARTMENTS) {
            return folder.getPermissions().stream()
                    .anyMatch(p -> p.getCanUpload() &&
                            ((p.getResident() != null && p.getResident().getIdUsers().equals(residentId)) ||
                             (p.getApartment() != null && apartmentId != null && p.getApartment().getIdApartment().equals(apartmentId))));
        }

        return false;
    }

    private DocumentDto mapToDocumentDto(Document document) {
        String baseUrl = "http://192.168.1.8:9090/api/v1/documents";
        return DocumentDto.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .storedFilename(document.getStoredFilename())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .fileExtension(document.getFileExtension())
                .folderId(document.getFolder().getId())
                .apartmentId(document.getApartment() != null ? document.getApartment().getIdApartment() : null)
                .buildingId(document.getBuilding() != null ? document.getBuilding().getBuildingId() : null)
                .uploadedBy(document.getUploadedBy())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .downloadUrl(baseUrl + "/" + document.getId() + "/download")
                .previewUrl(baseUrl + "/" + document.getId() + "/preview")
                .build();
    }
}
