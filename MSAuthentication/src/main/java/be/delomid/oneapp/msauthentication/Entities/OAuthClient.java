package be.delomid.oneapp.msauthentication.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oauth_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret;

    private String appName;

    private String redirectUri;

    private String scopes; // ex: "read,write"
}