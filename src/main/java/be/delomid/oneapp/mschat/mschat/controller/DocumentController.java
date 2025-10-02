package be.delomid.oneapp.mschat.mschat.controller;

import be.delomid.oneapp.mschat.mschat.dto.CreateFolderRequest;
import be.delomid.oneapp.mschat.mschat.dto.DocumentDto;
import be.delomid.oneapp.mschat.mschat.dto.FolderDto;
import be.delomid.oneapp.mschat.mschat.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/folders")
    public ResponseEntity<FolderDto> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {
        String username = getUserId(authentication);
        FolderDto folder = documentService.createFolder(request, username);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/folders")
    public ResponseEntity<List<FolderDto>> getRootFolders(Authentication authentication) {
        String username = getUserId(authentication);
        List<FolderDto> folders = documentService.getRootFolders(username);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/folders/{folderId}/subfolders")
    public ResponseEntity<List<FolderDto>> getSubFolders(
            @PathVariable Long folderId,
            Authentication authentication) {
        String username = getUserId(authentication);
        List<FolderDto> folders = documentService.getSubFolders(folderId, username);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/folders/{folderId}/documents")
    public ResponseEntity<List<DocumentDto>> getFolderDocuments(
            @PathVariable Long folderId,
            Authentication authentication) {
        String username = getUserId(authentication);
        List<DocumentDto> documents = documentService.getFolderDocuments(folderId, username);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/folders/{folderId}/upload")
    public ResponseEntity<DocumentDto> uploadDocument(
            @PathVariable Long folderId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        String username = getUserId(authentication);
        DocumentDto document = documentService.uploadDocument(folderId, file, description, username);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long folderId,
            Authentication authentication) {
        String username = getUserId(authentication);
        documentService.deleteFolder(folderId, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication) {
        String username = getUserId(authentication);
        documentService.deleteDocument(documentId, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication) throws IOException {
        String username = getUserId(authentication);
        byte[] fileContent = documentService.downloadDocument(documentId, username);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
    }

    @GetMapping("/{documentId}/preview")
    public ResponseEntity<byte[]> previewDocument(
            @PathVariable Long documentId,
            Authentication authentication) throws IOException {
        String username = getUserId(authentication);
        byte[] fileContent = documentService.downloadDocument(documentId, username);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentDto>> searchDocuments(
            @RequestParam String query,
            Authentication authentication) {
        String username = getUserId(authentication);
        List<DocumentDto> documents = documentService.searchDocuments(query, username);
        return ResponseEntity.ok(documents);
    }

    private String getUserId(Authentication authentication) {
        return authentication.getName();
    }
}
