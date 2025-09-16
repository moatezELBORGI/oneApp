package be.delomid.oneapp.mschat.mschat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResidentDto {
    private String idUsers;
    private String fname;
    private String lname;
    private String email;
    private String phoneNumber;
    private String picture;
    private String apartmentId;
    private String buildingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}