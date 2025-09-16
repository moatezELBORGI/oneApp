package be.delomid.oneapp.msauthentication.Services;
import be.delomid.oneapp.msauthentication.Entities.OAuthClient;
import be.delomid.oneapp.msauthentication.repositories.publicentitiesrepositories.OAuthClientRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;


@Service
public class OAuthClientService {

    private final OAuthClientRepository clientRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public OAuthClientService(OAuthClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    // Génération d'un nouveau client
    public OAuthClient createClient(String appName, String redirectUri, String scopes) {
        String clientId = UUID.randomUUID().toString();
        String clientSecretRaw = UUID.randomUUID().toString(); // secret avant hash
        String clientSecretHashed = passwordEncoder.encode(clientSecretRaw);

        OAuthClient client = OAuthClient.builder()
                .clientId(clientId)
                .clientSecret(clientSecretHashed)
                .appName(appName)
                .redirectUri(redirectUri)
                .scopes(scopes)
                .build();

        clientRepository.save(client);

        // On retourne l’objet avec le secret en clair pour une seule fois
        client.setClientSecret(clientSecretRaw);
        return client;
    }

    // Validation d'un client
    public boolean validateClient(String clientId, String clientSecretRaw) {
        Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);
        return clientOpt.filter(oAuthClient -> passwordEncoder.matches(clientSecretRaw, oAuthClient.getClientSecret())).isPresent();

    }
}
