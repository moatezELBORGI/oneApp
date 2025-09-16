package be.delomid.oneapp.msauthentication.repositories.userrepositories;

  import be.delomid.oneapp.msauthentication.Entities.OtpCode;
  import be.delomid.oneapp.msauthentication.Entities.UserInfo;
  import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtpCodeRepository  extends JpaRepository<OtpCode, Long> {
    OtpCodeRepository findByCodeOtp(String otpCode);
    List<OtpCode> findByUserInfo(UserInfo userInfo);
    OtpCode findByCodeOtpAndUserInfoAndValidCodeIsTrue(String otp, UserInfo userInfo);


}
