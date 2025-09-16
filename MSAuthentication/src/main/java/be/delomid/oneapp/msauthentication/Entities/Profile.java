package be.delomid.oneapp.msauthentication.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Profile implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID profileId;
    private String profileLabel;
    private boolean isActiveProfile;
    private LocalDateTime profileCreationDate;
    private LocalDateTime profileModifiedDate;
    @OneToMany(mappedBy = "profileAuthorizationId.profile")
    private List<ProfileAuthorization> profileAuthorizationList;

    @OneToMany(mappedBy = "userProfileId.profile")
    private List<UserProfile> userProfileList;


}
