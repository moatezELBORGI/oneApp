package be.delomid.oneapp.msauthentication.Entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "authorization_oneapp")
public class Authorization implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID authorizationId;
    private String authorizationName;
    private boolean activeAuthorization;

    @OneToMany(mappedBy = "userAuthorizationId.authorization",fetch = FetchType.EAGER)
    @JsonIgnore
    private List<UserAuthorization> authorizationUserList;

    @OneToMany(mappedBy = "profileAuthorizationId.authorization",fetch = FetchType.EAGER)
    @JsonIgnore
    private List<ProfileAuthorization> profileAuthorizationList;


}
