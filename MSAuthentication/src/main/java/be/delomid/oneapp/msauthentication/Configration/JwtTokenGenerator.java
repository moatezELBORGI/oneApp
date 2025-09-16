package be.delomid.oneapp.msauthentication.Configration;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenGenerator {
    private final JwtEncoder jwtEncoder;
    private final UserInfoManagerConfig userInfoManagerConfig;
    public String generateAccessToken(Authentication authentication) {

        log.info("[JwtTokenGenerator:generateAccessToken] Token Creation Started for:{}", authentication.getName());
        log.info("[JwtTokenGenerator:generateAccessToken] Token Creation Started for:{}", authentication.getName());

        Object principal = authentication.getPrincipal();

        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String string) {
            username = string;
        } else {
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
        }


        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("Messagerie Gestion immobilière (MGI)")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1 , ChronoUnit.DAYS))
                .subject(authentication.getName())
                .claim("scope", authorities)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
    public String verifyOtpToken(Authentication authentication) {

        log.info("[JwtTokenGenerator:sendAndVerifySmsToken] Token Creation Started for:{}", authentication.getName());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("Messagerie Gestion immobilière (MGI)")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(20, ChronoUnit.MINUTES))
                .subject(authentication.getName())
                .claim("scope", "VERIFY_OTP")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
