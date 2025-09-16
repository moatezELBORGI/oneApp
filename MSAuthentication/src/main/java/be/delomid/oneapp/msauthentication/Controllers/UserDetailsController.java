package be.delomid.oneapp.msauthentication.Controllers;


  import be.delomid.oneapp.msauthentication.Services.IUserDetailsService;
  import be.delomid.oneapp.msauthentication.dto.UserInfoDto;
  import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/UserDetails")
@RequiredArgsConstructor
@Slf4j
public class UserDetailsController {
    private final IUserDetailsService iUserDetailsService;
    @GetMapping("/getUserInfoById/{userName}")
    public UserInfoDto getUserInfoById(@PathVariable String userName)
    {
        return iUserDetailsService.getUserInfoById(userName);
    }
    @GetMapping("/getUserInfoByUUID/{uuid}")
    public UserInfoDto getUserInfoByUUID(@PathVariable String uuid)
    {
        return iUserDetailsService.getUserInfoByUUID(uuid);
    }
}
