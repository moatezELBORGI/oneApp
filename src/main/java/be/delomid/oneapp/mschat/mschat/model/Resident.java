package be.delomid.oneapp.mschat.mschat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "residents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resident {
    
    @Id
    @Column(name = "id_users")
    private String idUsers;
    
    @Column(name = "fname", nullable = false)
    private String fname;
    
    @Column(name = "lname", nullable = false)
    private String lname;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "picture")
    private String picture;
    
    @OneToOne(mappedBy = "resident", fetch = FetchType.LAZY)
    private Apartment apartment;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}