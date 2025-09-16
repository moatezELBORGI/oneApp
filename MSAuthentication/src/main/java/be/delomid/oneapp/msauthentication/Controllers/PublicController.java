package be.delomid.oneapp.msauthentication.Controllers;

   import be.delomid.oneapp.msauthentication.Services.IAuthenticationService;
   import be.delomid.oneapp.msauthentication.dto.AuthRequestDto;
   import be.delomid.oneapp.msauthentication.dto.OtpVerifyToken;
   import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
   import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {
    private final IAuthenticationService iAuthenticationService ;
    @PostMapping("/Login/firstStepOfAuthentication")
    public OtpVerifyToken firstStepOfAuthentication(@RequestBody AuthRequestDto authRequestDto, HttpServletRequest request, HttpServletResponse response)
    {
        return iAuthenticationService.firstStepOfAuthentication(authRequestDto,request,response);
    }
}
