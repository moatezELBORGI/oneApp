package be.delomid.oneapp.msauthentication.repositories.userrepositories;

 import be.delomid.oneapp.msauthentication.Entities.UserAuthorization;
 import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthorizationRepository extends JpaRepository<UserAuthorization, UserAuthorization.UserAuthorizationId> {
}
