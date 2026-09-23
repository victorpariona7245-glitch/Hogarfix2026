package com.hogarfix.model;

import com.hogarfix.model.base.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "tbl_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends Auditable implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(name = "username", nullable = false, length = 150, unique = true)
    private String username;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", referencedColumnName = "idRol")
    private Rol rol;

    @OneToOne(mappedBy = "usuario")
    private Tecnico tecnico;

    // 🔒 Métodos requeridos por UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Devuelve el rol como autoridad
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.getNombre().toUpperCase()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return getIsActivo();
    }
}
