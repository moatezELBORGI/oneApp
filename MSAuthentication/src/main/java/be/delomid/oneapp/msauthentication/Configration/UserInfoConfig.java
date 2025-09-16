package be.delomid.oneapp.msauthentication.Configration;


 import be.delomid.oneapp.msauthentication.Entities.UserAuthorization;
 import be.delomid.oneapp.msauthentication.Entities.UserInfo;
 import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class UserInfoConfig implements UserDetails {
    private final UserInfo userInfoEntity;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (this.userInfoEntity.getUserAuthorizationList() != null) {
            for (UserAuthorization userAuthorization : this.userInfoEntity.getUserAuthorizationList()) {
                if (userAuthorization.getUserAuthorizationId().getAuthorization() != null && userAuthorization.getIsActiveUserAuthorization() ) {
                    authorities.add(new SimpleGrantedAuthority(userAuthorization.getUserAuthorizationId().getAuthorization().getAuthorizationName()));
                }
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return userInfoEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return userInfoEntity.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return userInfoEntity.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return userInfoEntity.isEnabled();
    }
}
