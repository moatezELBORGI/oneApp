package be.delomid.oneapp.msauthentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OtpVerifyToken {
    @JsonProperty("otp_verify_token")
    private String otpVerifyToken;

    @JsonProperty("otp_verify_token_expiry")
    private int otpVerifyTokenExpiry;

    @JsonProperty("token_type")
    private TokenType tokenType;
}
