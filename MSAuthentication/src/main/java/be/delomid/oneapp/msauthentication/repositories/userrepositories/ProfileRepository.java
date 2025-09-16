package be.delomid.oneapp.msauthentication.repositories.userrepositories;

 import be.delomid.oneapp.msauthentication.Entities.Profile;
 import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
}
