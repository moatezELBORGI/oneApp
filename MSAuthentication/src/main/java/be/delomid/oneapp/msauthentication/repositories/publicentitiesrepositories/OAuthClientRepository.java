package be.delomid.oneapp.msauthentication.repositories.publicentitiesrepositories;

import be.delomid.oneapp.msauthentication.Entities.OAuthClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthClientRepository extends JpaRepository<OAuthClient, Long> {
    Optional<OAuthClient> findByClientId(String clientId);
}