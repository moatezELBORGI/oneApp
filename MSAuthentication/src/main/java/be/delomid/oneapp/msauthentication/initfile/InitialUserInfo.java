package be.delomid.oneapp.msauthentication.initfile;



import be.delomid.oneapp.msauthentication.Entities.Authorization;
import be.delomid.oneapp.msauthentication.Entities.UserInfo;
import be.delomid.oneapp.msauthentication.repositories.userrepositories.AuthorizationRepository;
import be.delomid.oneapp.msauthentication.repositories.userrepositories.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class InitialUserInfo implements CommandLineRunner {
    private final AuthorizationRepository authorizationRepository;
     private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        this.createPermissions();
        this.createUsers();
    }

    private void createPermissions() {
        List<String> permissionNames = List.of(
                "perm_chanel",
                "perm_discussion",
                "perm_AttachmentsFile",
                "perm_SelfApartmentManagement"


        );
        List<Authorization> authorizationList=authorizationRepository.findAll();
        if(authorizationList.isEmpty() || authorizationList.size()< permissionNames.size()) {
             for (String permissionName : permissionNames) {
                Authorization authorization = new Authorization();
                authorization.setAuthorizationName(permissionName);
                authorization.setActiveAuthorization(true);
                Authorization savedAuthorization = authorizationRepository.save(authorization);
                log.info("Permission {} added to the database.", permissionName);
             }
        }

    }

    private void createUsers() {
        Optional<UserInfo> userInfo=userInfoRepository.findByUserName("moatez");
        if(userInfo.isEmpty()) {
            UserInfo userInfo1 = new UserInfo();
            userInfo1.setUserName("moatez");
            userInfo1.setPassword(passwordEncoder.encode("moatez"));
            userInfo1.setEmailAddress("moatez@delomid-it.com");
            userInfo1.setFirstName("Moatez");
            userInfo1.setLastName("BORGI");
            userInfo1.setCreatedAccountDate(LocalDateTime.now());
            userInfo1.setEnabled(true);
           UserInfo userInfo2= userInfoRepository.save(userInfo1);



        }
    }


}
