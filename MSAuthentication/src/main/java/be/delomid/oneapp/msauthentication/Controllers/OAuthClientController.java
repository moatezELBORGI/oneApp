package be.delomid.oneapp.msauthentication.Controllers;


import be.delomid.oneapp.msauthentication.Entities.OAuthClient;
import be.delomid.oneapp.msauthentication.Services.OAuthClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class OAuthClientController {

    private final OAuthClientService clientService;

    public OAuthClientController(OAuthClientService clientService) {
        this.clientService = clientService;
    }

    // Création d’un client
    @PostMapping("/register")
    public ResponseEntity<OAuthClient> registerClient(
            @RequestParam String appName,
            @RequestParam(required = false) String redirectUri,
            @RequestParam(defaultValue = "read") String scopes
    ) {
        OAuthClient client = clientService.createClient(appName, redirectUri, scopes);
        return ResponseEntity.ok(client);
    }

    // Validation (exemple endpoint /api/clients/validate)
    @PostMapping("/validate")
    public ResponseEntity<String> validateClient(
            @RequestParam String clientId,
            @RequestParam String clientSecret
    ) {
        boolean isValid = clientService.validateClient(clientId, clientSecret);
        if (isValid) {
            return ResponseEntity.ok("Client validé");
        } else {
            return ResponseEntity.status(401).body("Client non valide");
        }
    }
}
