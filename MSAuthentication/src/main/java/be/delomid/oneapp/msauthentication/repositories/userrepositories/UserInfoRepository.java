package be.delomid.oneapp.msauthentication.repositories.userrepositories;

 import be.delomid.oneapp.msauthentication.Entities.UserInfo;
 import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserInfoRepository extends JpaRepository<UserInfo, UUID> {
    Optional<UserInfo> findByEmailAddress(String emailAddress);
    Optional<UserInfo> findByUserName(String username);


}
