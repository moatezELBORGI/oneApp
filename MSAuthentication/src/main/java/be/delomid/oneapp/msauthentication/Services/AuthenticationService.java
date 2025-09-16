package be.delomid.oneapp.msauthentication.Services;



import be.delomid.oneapp.msauthentication.Configration.JwtTokenGenerator;
import be.delomid.oneapp.msauthentication.Configration.JwtTokenUtils;
import be.delomid.oneapp.msauthentication.Entities.OtpCode;
import be.delomid.oneapp.msauthentication.Entities.UserAuthorization;
import be.delomid.oneapp.msauthentication.Entities.UserInfo;
import be.delomid.oneapp.msauthentication.dto.AuthRequestDto;
import be.delomid.oneapp.msauthentication.dto.AuthResponseDto;
import be.delomid.oneapp.msauthentication.dto.OtpVerifyToken;
import be.delomid.oneapp.msauthentication.dto.TokenType;
import be.delomid.oneapp.msauthentication.exceptionhandler.NotFoundException;
import be.delomid.oneapp.msauthentication.exceptionhandler.OtpException;
import be.delomid.oneapp.msauthentication.repositories.userrepositories.OtpCodeRepository;
import be.delomid.oneapp.msauthentication.repositories.userrepositories.UserInfoRepository;
import be.delomid.oneapp.msauthentication.utils.Utils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService implements IAuthenticationService {

    private final JwtTokenGenerator jwtTokenGenerator;
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpCodeRepository otpCodeRepository;
    private final JwtTokenUtils jwtTokenUtils;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 5;

    private static Authentication createAuthenticationObject(UserInfo userInfoEntity) {
        String username = userInfoEntity.getUserName();
        String password = userInfoEntity.getPassword();
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (UserAuthorization userAuthorization : userInfoEntity.getUserAuthorizationList()) {
            if (userAuthorization.getUserAuthorizationId().getAuthorization() != null && userAuthorization.getIsActiveUserAuthorization()) {
                authorities.add(new SimpleGrantedAuthority(userAuthorization.getUserAuthorizationId().getAuthorization().getAuthorizationName()));
            }
        }

        GrantedAuthority[] authoritiesArray = authorities.toArray(new GrantedAuthority[0]);
        return new UsernamePasswordAuthenticationToken(username, password, List.of(authoritiesArray));
    }
    @Override
    public AuthResponseDto thirdStepOfAuthentication(HttpServletRequest request) {
        try {
            String userName=jwtTokenUtils.getUserNameOfUser(Utils.getTokenFromAuthorization(request));
            var userInfoEntity = userInfoRepository.findByUserName(userName)
                    .orElseThrow(() -> {
                        log.error("[AuthService:userSignInAuth] User :{} not found",userName );
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "USER NOT FOUND");
                    });

            // Création de l'objet d'authentification
            Authentication authentication = createAuthenticationObject(userInfoEntity);
            // Stocker l'authentification dans le SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Génération du token
            String accessToken = jwtTokenGenerator.generateAccessToken(authentication);
            log.info("[AuthService:userSignInAuth] Access token for user:{}, has been generated", userInfoEntity.getUserName());
            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .accessTokenExpiry(10 * 60)
                    .userName(userInfoEntity.getUserName())
                    .tokenType(TokenType.Bearer)
                    .build();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AuthService:userSignInAuth] Exception while authenticating the user due to: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please Try Again");
        }
    }

    @Override
    public OtpVerifyToken firstStepOfAuthentication(AuthRequestDto authRequestDto, HttpServletRequest request, HttpServletResponse response) {
        try {
            var userInfoEntity = userInfoRepository.findByEmailAddress(authRequestDto.getEmailAddress())
                    .orElseThrow(() -> {
                        log.error("[AuthService:userSignInAuth] User :{} not found", authRequestDto.getEmailAddress());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "USER NOT FOUND");
                    });

            // Vérifier si le compte est bloqué
            if (userInfoEntity.getBlockedUntil() != null && userInfoEntity.getBlockedUntil().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.LOCKED, "Compte temporairement bloqué");
            }

            // Vérifier le mot de passe
            if (!passwordEncoder.matches(authRequestDto.getPassword(), userInfoEntity.getPassword())) {
                int attempts = userInfoEntity.getTentativeConnexion() + 1;
                userInfoEntity.setTentativeConnexion(attempts);

                if (attempts >= MAX_LOGIN_ATTEMPTS) {
                    LocalDateTime blockUntil = LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES);
                    userInfoEntity.setBlockedUntil(blockUntil);
                    log.warn("Compte bloqué pour l'utilisateur: {} après {} tentatives", userInfoEntity.getEmailAddress(), attempts);
                    userInfoRepository.save(userInfoEntity);
                    throw new ResponseStatusException(HttpStatus.LOCKED, "Compte bloqué après trop de tentatives");
                }

                userInfoRepository.save(userInfoEntity);
                log.error("[AuthService:userSignInAuth] Invalid password for user: {}", authRequestDto.getEmailAddress());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID CREDENTIALS");
            }

            // Authentification réussie → réinitialiser les tentatives
            userInfoEntity.setTentativeConnexion(0);
            userInfoEntity.setBlockedUntil(null);
            userInfoRepository.save(userInfoEntity);

            Authentication authentication = createAuthenticationObject(userInfoEntity);
            String verifyOtpToken = jwtTokenGenerator.verifyOtpToken(authentication);
            String otpCode = generateOtpCode(userInfoEntity);

            this.sendEmailWithTemplate(
                    userInfoEntity.getEmailAddress(),
                    "Code de vérification OTP",
                    "Authentification à double facteur",
                    otpCode
            );

            return OtpVerifyToken.builder()
                    .otpVerifyToken(verifyOtpToken)
                    .otpVerifyTokenExpiry(20 * 60)
                    .tokenType(TokenType.Bearer)
                    .build();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AuthService:userSignInAuth] Exception while sending otp to the user due to: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please Try Again");
        }
    }

    @Override
    public AuthResponseDto secondStepOfAuthentication(String otpCodeSend, HttpServletRequest request) {
        String userName=jwtTokenUtils.getUserNameOfUser(Utils.getTokenFromAuthorization(request));
        UserInfo userInfo=userInfoRepository.findByUserName(userName).orElseThrow(() -> new NotFoundException("User with given ID Not Found"));
        OtpCode otpCode=otpCodeRepository.findByCodeOtpAndUserInfoAndValidCodeIsTrue(otpCodeSend,userInfo);
        if(otpCode==null ) {
            throw new OtpException("Otp code not found");

        }
        if(otpCode.hasExceededTwentyMinutes())
        {
            otpCode.setValidCode(false);
            otpCodeRepository.save(otpCode);
            throw new OtpException("Otp code expired");

        }
        otpCode.setValidCode(false);
        otpCodeRepository.save(otpCode);
        return this.thirdStepOfAuthentication(request);
    }



    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    public  void sendEmailWithTemplate(String to, String subject, String title, String body) throws MessagingException {
        // Create a Thymeleaf context
        Context context = new Context();
        context.setVariable("subject", "Your OTP Code");
        context.setVariable("date", LocalDate.now().toString());
        context.setVariable("header", "Your OTP");
        context.setVariable("username", to);
        context.setVariable("message", "Here is your OTP code, valid for 5 minutes.");
        context.setVariable("otp", body);
        context.setVariable("supportEmail", "info@delomid-it.com");
        context.setVariable("helpCenterUrl", "https://www.delomid-it.com/");
        context.setVariable("companyName", "Copy right Delomid-IT");
        context.setVariable("companyAddress", "Rue de la Loi 28, 1000 Bruxelles, Belgique.");
        context.setVariable("facebookUrl", "https://facebook.com");
        context.setVariable("instagramUrl", "https://instagram.com");
        context.setVariable("twitterUrl", "https://twitter.com");
        context.setVariable("youtubeUrl", "https://youtube.com");
        context.setVariable("year", Year.now().toString());
        // Generate email content using the template
        String htmlContent = templateEngine.process("emailTemplate",context);

        // Create the email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML content

        // Send the email
        mailSender.send(message);
    }
    private String generateOtpCode(UserInfo userInfoEntity) {
        for(OtpCode otpCode:otpCodeRepository.findByUserInfo(userInfoEntity))
        {
            otpCode.setValidCode(false);
            otpCodeRepository.save(otpCode);
        }
        OtpCode otpCode=new OtpCode();
        otpCode.setUserInfo(userInfoEntity);
        otpCode.setValidCode(true);
        otpCode.setLifeTime(10);
        otpCode.setDateOfCreation(LocalDateTime.now());
        String code = Utils.generateRandomString();
        otpCode.setCodeOtp(code);
        otpCodeRepository.save(otpCode);
        return code;
    }
    private void deleteCookieIfExists(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }
    private Cookie creatotpverifytokenCookie(HttpServletRequest request, HttpServletResponse response, String otpVerifyTokenToken) {
        deleteCookieIfExists(request, response, "otp_verify_token");
        Cookie accessTokenCookie = new Cookie("otp_verify_token",otpVerifyTokenToken);
        accessTokenCookie.setHttpOnly(false);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(8 * 60 * 60); // 8 hours in seconds
        response.addCookie(accessTokenCookie);
        return accessTokenCookie;
    }
}
