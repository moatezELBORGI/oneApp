package be.delomid.oneapp.msauthentication.repositories.userrepositories;

 import be.delomid.oneapp.msauthentication.Entities.UserProfile;
 import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UserProfile.UserProfileId> {
}
