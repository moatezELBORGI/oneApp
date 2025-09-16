package be.delomid.oneapp.msauthentication.Services;



import be.delomid.oneapp.msauthentication.Entities.UserInfo;
import be.delomid.oneapp.msauthentication.dto.UserInfoDto;
import be.delomid.oneapp.msauthentication.exceptionhandler.NotFoundException;
import be.delomid.oneapp.msauthentication.repositories.userrepositories.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsService implements IUserDetailsService {
    private final UserInfoRepository userInfoRepository;
    @Override
    public UserInfoDto getUserInfoById(String userName) {
        UserInfo userInfo=userInfoRepository.findByUserName(userName).orElseThrow(() -> new NotFoundException("User with given ID Not Found"));
        UserInfoDto userInfoDto=new UserInfoDto();
        userInfoDto.setEmailAddress(userInfo.getEmailAddress());
        userInfoDto.setUserName(userInfo.getUserName());
        userInfoDto.setUserId(userInfo.getUserId());
        userInfoDto.setFirstName(userInfo.getFirstName());
        userInfoDto.setLastName(userInfo.getLastName());
        return userInfoDto;
    }

    @Override
    public UserInfoDto getUserInfoByUUID(String uuid) {
        UserInfo userInfo=userInfoRepository.findById(UUID.fromString(uuid)).orElseThrow(() -> new NotFoundException("User with given ID Not Found"));
        UserInfoDto userInfoDto=new UserInfoDto();
        userInfoDto.setEmailAddress(userInfo.getEmailAddress());
        userInfoDto.setUserName(userInfo.getUserName());
        userInfoDto.setUserId(userInfo.getUserId());
        userInfoDto.setFirstName(userInfo.getFirstName());
        userInfoDto.setLastName(userInfo.getLastName());
        return userInfoDto;
    }
}
