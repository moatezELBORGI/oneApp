package be.delomid.oneapp.msauthentication.repositories.userrepositories;

 import be.delomid.oneapp.msauthentication.Entities.ProfileAuthorization;
 import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileAuthorizationRepository extends JpaRepository<ProfileAuthorization, ProfileAuthorization.ProfileAuthorizationId> {
}
