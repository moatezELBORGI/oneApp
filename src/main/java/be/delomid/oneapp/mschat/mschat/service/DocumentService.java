package be.delomid.oneapp.mschat.mschat.service;

import be.delomid.oneapp.mschat.mschat.dto.CreateFolderRequest;
import be.delomid.oneapp.mschat.mschat.dto.DocumentDto;
import be.delomid.oneapp.mschat.mschat.dto.FolderDto;
import be.delomid.oneapp.mschat.mschat.model.Apartment;
import be.delomid.oneapp.mschat.mschat.model.Document;
import be.delomid.oneapp.mschat.mschat.model.Folder;
import be.delomid.oneapp.mschat.mschat.model.Resident;
import be.delomid.oneapp.mschat.mschat.repository.ApartmentRepository;
import be.delomid.oneapp.mschat.mschat.repository.DocumentRepository;
import be.delomid.oneapp.mschat.mschat.repository.FolderRepository;
import be.delomid.oneapp.mschat.mschat.repository.ResidentRepository;
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

    @Value("${app.documents.base-dir:documents}")
    private String baseDocumentsDir;

    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    @Transactional
    public FolderDto createFolder(CreateFolderRequest request, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        String cleanName = request.getName().trim();
        if (cleanName.isEmpty()) {
            throw new RuntimeException("Le nom du dossier ne peut pas être vide");
        }

        if (request.getParentFolderId() != null) {
            Folder parentFolder = folderRepository.findByIdAndApartmentId(
                    request.getParentFolderId(), apartment.getId())
                    .orElseThrow(() -> new RuntimeException("Dossier parent non trouvé"));

            if (folderRepository.existsByNameAndParentFolderIdAndApartmentId(
                    cleanName, request.getParentFolderId(), apartment.getId())) {
                throw new RuntimeException("Un dossier avec ce nom existe déjà à cet emplacement");
            }
        } else {
            if (folderRepository.existsByNameAndParentFolderIsNullAndApartmentId(
                    cleanName, apartment.getId())) {
                throw new RuntimeException("Un dossier avec ce nom existe déjà à la racine");
            }
        }

        String folderPath = buildFolderPath(apartment.getId(), request.getParentFolderId(), cleanName);

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

        Folder folder = Folder.builder()
                .name(cleanName)
                .folderPath(folderPath)
                .parentFolder(parentFolder)
                .apartment(apartment)
                .createdBy(resident.getIdUsers())
                .build();

        folder = folderRepository.save(folder);
        log.info("Dossier créé en base de données: {} (ID: {}) pour appartement: {}", folder.getName(), folder.getId(), apartment.getId());

        return mapToFolderDto(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderDto> getRootFolders(String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        ensureApartmentRootFolderExists(apartment);

        List<Folder> folders = folderRepository.findByApartmentIdAndParentFolderIsNull(apartment.getId());
        log.debug("Récupération de {} dossiers racine pour l'appartement {}", folders.size(), apartment.getId());

        return folders.stream()
                .map(this::mapToFolderDto)
                .collect(Collectors.toList());
    }

    private void ensureApartmentRootFolderExists(Apartment apartment) {
        try {
            String apartmentFolderPath = "apartment_" + apartment.getId();
            Path physicalPath = Paths.get(baseDocumentsDir, apartmentFolderPath);

            if (!Files.exists(physicalPath)) {
                Files.createDirectories(physicalPath);
                log.info("Dossier racine créé pour l'appartement {}: {}", apartment.getId(), physicalPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Erreur lors de la création du dossier racine de l'appartement {}", apartment.getId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<FolderDto> getSubFolders(Long folderId, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        Folder folder = folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé ou accès non autorisé"));

        log.debug("Récupération de {} sous-dossiers pour le dossier {} (ID: {})",
                folder.getSubFolders().size(), folder.getName(), folderId);

        return folder.getSubFolders().stream()
                .map(this::mapToFolderDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> getFolderDocuments(Long folderId, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        Folder folder = folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
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

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        Folder folder = folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
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
                    .uploadedBy(resident.getIdUsers())
                    .description(description != null ? description.trim() : null)
                    .build();

            document = documentRepository.save(document);
            log.info("Document uploadé: {} (ID: {}) dans le dossier: {} pour appartement: {}",
                    originalFilename, document.getId(), folder.getName(), apartment.getId());

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

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        Folder folder = folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé ou accès non autorisé"));

        try {
            Path folderPath = Paths.get(baseDocumentsDir, folder.getFolderPath());
            if (Files.exists(folderPath)) {
                deleteDirectoryRecursively(folderPath);
                log.info("Dossier physique supprimé: {}", folderPath.toAbsolutePath());
            }

            folderRepository.delete(folder);
            log.info("Dossier supprimé: {} (ID: {}) pour appartement: {}", folder.getName(), folderId, apartment.getId());

        } catch (IOException e) {
            log.error("Erreur lors de la suppression du dossier: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la suppression du dossier: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteDocument(Long documentId, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        Document document = documentRepository.findByIdAndApartmentId(documentId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Document non trouvé ou accès non autorisé"));

        try {
            Path filePath = Paths.get(baseDocumentsDir, document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Fichier physique supprimé: {}", filePath.toAbsolutePath());
            }

            documentRepository.delete(document);
            log.info("Document supprimé: {} (ID: {}) pour appartement: {}", document.getOriginalFilename(), documentId, apartment.getId());

        } catch (IOException e) {
            log.error("Erreur lors de la suppression du document: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la suppression du document: " + e.getMessage());
        }
    }

    public byte[] downloadDocument(Long documentId, String email) throws IOException {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        Document document = documentRepository.findByIdAndApartmentId(documentId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Document non trouvé ou accès non autorisé"));

        Path filePath = Paths.get(baseDocumentsDir, document.getFilePath());
        if (!Files.exists(filePath)) {
            log.error("Fichier physique non trouvé: {}", filePath.toAbsolutePath());
            throw new RuntimeException("Fichier non trouvé sur le système de fichiers");
        }

        log.info("Téléchargement du document: {} (ID: {}) pour appartement: {}",
                document.getOriginalFilename(), documentId, apartment.getId());
        return Files.readAllBytes(filePath);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> searchDocuments(String query, String email) {
        Resident resident = residentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Résident non trouvé"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("Aucun appartement associé au résident");
        }

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<Document> documents = documentRepository.searchDocuments(apartment.getId(), query.trim());
        log.debug("Recherche '{}' a retourné {} documents pour appartement {}",
                query, documents.size(), apartment.getId());

        return documents.stream()
                .map(this::mapToDocumentDto)
                .collect(Collectors.toList());
    }

    private String buildFolderPath(Long apartmentId, Long parentFolderId, String folderName) {
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
        return FolderDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .folderPath(folder.getFolderPath())
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
                .apartmentId(folder.getApartment().getId())
                .createdBy(folder.getCreatedBy())
                .createdAt(folder.getCreatedAt())
                .subFolderCount(folder.getSubFolders() != null ? folder.getSubFolders().size() : 0)
                .documentCount(folder.getDocuments() != null ? folder.getDocuments().size() : 0)
                .build();
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
                .apartmentId(document.getApartment().getId())
                .uploadedBy(document.getUploadedBy())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .downloadUrl(baseUrl + "/" + document.getId() + "/download")
                .previewUrl(baseUrl + "/" + document.getId() + "/preview")
                .build();
    }
}
