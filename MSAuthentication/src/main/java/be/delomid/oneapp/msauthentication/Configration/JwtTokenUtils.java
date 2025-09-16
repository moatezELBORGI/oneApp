package be.delomid.oneapp.msauthentication.Configration;


 import be.delomid.oneapp.msauthentication.repositories.userrepositories.UserInfoRepository;
 import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtTokenUtils {
    private final RSAKeyRecord rsaKeyRecord;

    public String getUserName(Jwt jwtToken){
        return jwtToken.getSubject();
    }

    public String getUserNameOfUser(String jwtTokenString) {
        JwtDecoder jwtDecoder =  NimbusJwtDecoder.withPublicKey(rsaKeyRecord.rsaPublicKey()).build();
        final Jwt jwtToken = jwtDecoder.decode(jwtTokenString);

        return getUserName(jwtToken);
    }
    public boolean isTokenValid(Jwt jwtToken, UserDetails userDetails){
        final String userName = getUserName(jwtToken);
        boolean isTokenExpired = getIfTokenIsExpired(jwtToken);
        boolean isTokenUserSameAsDatabase = userName.equals(userDetails.getUsername());

        return !isTokenExpired  && isTokenUserSameAsDatabase;

    }

    private boolean getIfTokenIsExpired(Jwt jwtToken) {
        return Objects.requireNonNull(jwtToken.getExpiresAt()).isBefore(Instant.now());
    }


    private final UserInfoRepository userInfoRepository;
    public UserDetails userDetails(String userName){
        return userInfoRepository
                .findByUserName(userName)
                .map(UserInfoConfig::new)
                .orElseThrow(()-> new UsernameNotFoundException("UserName: "+userName+" does not exist"));
    }
}
