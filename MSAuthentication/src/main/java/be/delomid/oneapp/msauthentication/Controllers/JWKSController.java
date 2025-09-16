package be.delomid.oneapp.msauthentication.Controllers;


 import be.delomid.oneapp.msauthentication.Configration.RSAKeyRecord;
 import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JWKSController {
    private final RSAKeyRecord rsaKeyRecord;
    private final String rsaKeyId;


    @GetMapping("/.well-known/jwks.json")
    public String getJWKS() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyRecord.rsaPublicKey())
                .keyID("rsa-key")
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .build();

        return new JWKSet(rsaKey).toString();
    }
}
