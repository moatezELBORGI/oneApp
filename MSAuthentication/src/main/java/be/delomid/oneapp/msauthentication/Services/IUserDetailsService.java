package be.delomid.oneapp.msauthentication.Services;


import be.delomid.oneapp.msauthentication.dto.UserInfoDto;

public interface IUserDetailsService {

    UserInfoDto getUserInfoById(String userId);
    UserInfoDto getUserInfoByUUID(String userId);

}
