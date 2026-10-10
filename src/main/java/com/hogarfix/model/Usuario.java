package com.hogarfix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // Nunca debe salir en respuestas JSON (ni siquiera el hash)
    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", referencedColumnName = "idRol")
    private Rol rol;

    @OneToOne(mappedBy = "usuario")
    private Tecnico tecnico;

    // Suspensión por parte del administrador (distinto de "pendiente de aprobación",
    // que usa isActivo = false). Una cuenta suspendida no puede iniciar sesión.
    @Column(name = "suspendido", columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    private boolean suspendido;

    @Column(name = "motivo_suspension", length = 255)
    private String motivoSuspension;

    // Secciones del panel admin a las que puede entrar (claves separadas por coma).
    // null = acceso completo (cuentas anteriores); "" = ninguna. El admin principal siempre tiene todo.
    @Column(name = "permisos", length = 300)
    private String permisos;

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
        return !suspendido;
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
