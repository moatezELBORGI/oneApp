package be.delomid.oneapp.msauthentication.Entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userId;

    private String userName;

    @Column(nullable = false, unique = true)
    private String emailAddress;

    private String pictureUrl;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String mobileNumber;

    private String firstName;

    private String lastName;
 private int tentativeConnexion=0;
     private LocalDateTime blockedUntil;

    private boolean isEnabled;

    private boolean isAccountNonExpired;

    private LocalDateTime createdAccountDate;

    @OneToMany(mappedBy = "userProfileId.userInfo")
    private List<UserProfile> userProfileList;
    @OneToMany(mappedBy = "userAuthorizationId.userInfo", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<UserAuthorization> userAuthorizationList;

    @OneToMany(mappedBy = "userInfo")
    @JsonIgnore
    private List<OtpCode> otpCodeList;
}
