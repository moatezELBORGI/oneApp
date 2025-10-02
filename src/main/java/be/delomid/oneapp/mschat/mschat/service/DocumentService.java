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
    public FolderDto createFolder(CreateFolderRequest request, String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        if (request.getParentFolderId() != null) {
            Folder parentFolder = folderRepository.findByIdAndApartmentId(
                    request.getParentFolderId(), apartment.getId())
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));

            if (folderRepository.existsByNameAndParentFolderIdAndApartmentId(
                    request.getName(), request.getParentFolderId(), apartment.getId())) {
                throw new RuntimeException("Folder with this name already exists in this location");
            }
        } else {
            if (folderRepository.existsByNameAndParentFolderIsNullAndApartmentId(
                    request.getName(), apartment.getId())) {
                throw new RuntimeException("Folder with this name already exists in root");
            }
        }

        String folderPath = buildFolderPath(apartment.getId(), request.getParentFolderId(), request.getName());

        try {
            Path physicalPath = Paths.get(baseDocumentsDir, folderPath);
            Files.createDirectories(physicalPath);
        } catch (IOException e) {
            log.error("Error creating physical folder", e);
            throw new RuntimeException("Failed to create folder on filesystem", e);
        }

        Folder parentFolder = null;
        if (request.getParentFolderId() != null) {
            parentFolder = folderRepository.findById(request.getParentFolderId())
                    .orElse(null);
        }

        Folder folder = Folder.builder()
                .name(request.getName())
                .folderPath(folderPath)
                .parentFolder(parentFolder)
                .apartment(apartment)
                .createdBy(username)
                .build();

        folder = folderRepository.save(folder);
        log.info("Folder created: {} for apartment: {}", folder.getName(), apartment.getId());

        return mapToFolderDto(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderDto> getRootFolders(String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        List<Folder> folders = folderRepository.findByApartmentIdAndParentFolderIsNull(apartment.getId());
        return folders.stream()
                .map(this::mapToFolderDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderDto> getSubFolders(Long folderId, String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        Folder folder = folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        return folder.getSubFolders().stream()
                .map(this::mapToFolderDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> getFolderDocuments(Long folderId, String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        List<Document> documents = documentRepository.findByFolderIdOrderByCreatedAtDesc(folderId);
        return documents.stream()
                .map(this::mapToDocumentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentDto uploadDocument(Long folderId, MultipartFile file, String description, String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        Folder folder = folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID().toString() + fileExtension;

            Path folderPhysicalPath = Paths.get(baseDocumentsDir, folder.getFolderPath());
            Files.createDirectories(folderPhysicalPath);

            Path filePath = folderPhysicalPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

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
                    .uploadedBy(username)
                    .description(description)
                    .build();

            document = documentRepository.save(document);
            log.info("Document uploaded: {} to folder: {}", originalFilename, folder.getName());

            return mapToDocumentDto(document);

        } catch (IOException e) {
            log.error("Error uploading document", e);
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    @Transactional
    public void deleteFolder(Long folderId, String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        Folder folder = folderRepository.findByIdAndApartmentId(folderId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        try {
            Path folderPath = Paths.get(baseDocumentsDir, folder.getFolderPath());
            if (Files.exists(folderPath)) {
                deleteDirectoryRecursively(folderPath);
            }

            folderRepository.delete(folder);
            log.info("Folder deleted: {} for apartment: {}", folder.getName(), apartment.getId());

        } catch (IOException e) {
            log.error("Error deleting folder", e);
            throw new RuntimeException("Failed to delete folder", e);
        }
    }

    @Transactional
    public void deleteDocument(Long documentId, String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        Document document = documentRepository.findByIdAndApartmentId(documentId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        try {
            Path filePath = Paths.get(baseDocumentsDir, document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            documentRepository.delete(document);
            log.info("Document deleted: {} for apartment: {}", document.getOriginalFilename(), apartment.getId());

        } catch (IOException e) {
            log.error("Error deleting document", e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }

    public byte[] downloadDocument(Long documentId, String username) throws IOException {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        Document document = documentRepository.findByIdAndApartmentId(documentId, apartment.getId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Paths.get(baseDocumentsDir, document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on filesystem");
        }

        return Files.readAllBytes(filePath);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> searchDocuments(String query, String username) {
        Resident resident = residentRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        Apartment apartment = resident.getApartment();
        if (apartment == null) {
            throw new RuntimeException("No apartment associated with resident");
        }

        List<Document> documents = documentRepository.searchDocuments(apartment.getId(), query);
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
