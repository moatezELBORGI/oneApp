package be.delomid.oneapp.mschat.mschat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderRequest {
    @NotBlank(message = "Folder name is required")
    private String name;

    private Long parentFolderId;

    private String description;
}
