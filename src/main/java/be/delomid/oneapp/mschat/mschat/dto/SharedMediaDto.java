package be.delomid.oneapp.mschat.mschat.dto;

import be.delomid.oneapp.mschat.mschat.model.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedMediaDto {
    private Long id;
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private FileType fileType;
    private String uploadedBy;
    private String uploaderName;
    private Integer duration;
    private String thumbnailPath;
    private LocalDateTime createdAt;
    private Long messageId;
    private String messageContent;
}
