package be.delomid.oneapp.msauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDto {

    private UUID userId;

    private String userName;

    private String emailAddress;

    private String password;

    private String mobileNumber;

    private String firstName;

    private String lastName;
    private LocalDateTime creationDate;

}
