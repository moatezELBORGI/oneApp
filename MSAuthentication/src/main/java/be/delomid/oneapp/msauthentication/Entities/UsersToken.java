package be.delomid.oneapp.msauthentication.Entities;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.UUID;

public class UsersToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userTokenId;

    private String token;

    private LocalDateTime expirationDate;

    private LocalDateTime creationDate;

    private boolean isExpired;

    private String fileToken;

    private String emailAdr;

    private String organizationId;

    private String userId;


}
