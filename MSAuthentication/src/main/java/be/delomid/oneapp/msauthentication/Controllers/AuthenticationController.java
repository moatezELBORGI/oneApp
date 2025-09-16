package be.delomid.oneapp.msauthentication.Controllers;


  import be.delomid.oneapp.msauthentication.Services.IAuthenticationService;
  import be.delomid.oneapp.msauthentication.dto.AuthResponseDto;
  import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Authentication")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {
    private final IAuthenticationService iAuthenticationService;


    @GetMapping("/Login/secondStepOfAuthentication/{otpCode}")
    public AuthResponseDto secondStepOfAuthentication(@PathVariable String otpCode, HttpServletRequest httpServletRequest)
    {
        return iAuthenticationService.secondStepOfAuthentication(otpCode,httpServletRequest);
    }

}
