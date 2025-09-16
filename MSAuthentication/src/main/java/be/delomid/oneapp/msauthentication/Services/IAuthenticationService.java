package be.delomid.oneapp.msauthentication.Services;


import be.delomid.oneapp.msauthentication.dto.AuthRequestDto;
import be.delomid.oneapp.msauthentication.dto.AuthResponseDto;
import be.delomid.oneapp.msauthentication.dto.OtpVerifyToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthenticationService {
    AuthResponseDto thirdStepOfAuthentication(HttpServletRequest request);
    OtpVerifyToken firstStepOfAuthentication(AuthRequestDto authRequestDto, HttpServletRequest request, HttpServletResponse response);
    AuthResponseDto secondStepOfAuthentication(String otpCode,HttpServletRequest request);
}
