package be.delomid.oneapp.msauthentication.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileAuthorization implements Serializable {
    @EmbeddedId
    private  ProfileAuthorizationId profileAuthorizationId;
    private LocalDateTime creationDate;
    private LocalDateTime modificationDate;
    @Column(nullable = false)
    private Boolean isActiveProfileAuthorization;
    @Embeddable
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
    public static class ProfileAuthorizationId implements Serializable {
        @JsonIgnore
        @ManyToOne
        private Authorization authorization;

        @ManyToOne
        @JsonIgnore
        private Profile profile;


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProfileAuthorizationId  other = (ProfileAuthorizationId) o;
            if (!Objects.equals(authorization, other.authorization)) return false;
            return Objects.equals(profile, other.profile);
        }

        @Override
        public int hashCode() {
            int result = authorization != null ? authorization.hashCode() : 0;
            result = 31 * result + (profile != null ? profile.hashCode() : 0);
            return result;
        }
    }
}
