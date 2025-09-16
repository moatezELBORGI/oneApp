package be.delomid.oneapp.msauthentication.Entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserProfile implements Serializable {
        @EmbeddedId
        private UserProfileId userProfileId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActiveUserProfile;

    @Embeddable
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
    public static class UserProfileId implements Serializable {
        @ManyToOne
        @JsonIgnore
        private Profile profile;
        @ManyToOne
        @JsonIgnore
        private UserInfo userInfo;


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserProfileId that = (UserProfileId) o;
            return Objects.equals(profile, that.profile) &&
                    Objects.equals(userInfo, that.userInfo)  ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(profile, userInfo);
        }

    }

}
