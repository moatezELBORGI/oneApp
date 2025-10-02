package be.delomid.oneapp.mschat.mschat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderDto {
    private Long id;
    private String name;
    private String folderPath;
    private Long parentFolderId;
    private String apartmentId;
    private String createdBy;
    private LocalDateTime createdAt;
    private Integer subFolderCount;
    private Integer documentCount;
    private List<FolderDto> subFolders;
}
