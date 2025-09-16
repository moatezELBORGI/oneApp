package be.delomid.oneapp.msauthentication.Entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OtpCode implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String codeOtp;
    private boolean validCode;
    private LocalDateTime dateOfCreation;
    private int lifeTime;
    @ManyToOne
    private UserInfo userInfo;

    public boolean hasExceededTwentyMinutes() {
        LocalDateTime expirationTime = this.dateOfCreation.plusMinutes(5);
        return LocalDateTime.now().isAfter(expirationTime);
    }
}
