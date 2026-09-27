package com.example.smartcatalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JPA entity that doubles as a Spring Security {@link UserDetails}.
 *
 * <p>Each user has a single role stored as a plain string (e.g. {@code "ROLE_USER"}).
 * Extend the {@code roles} field to a {@code @ManyToMany} relationship if you need
 * multiple roles per user in the future.
 */
@Entity
@Table(name = "users")          // "user" is a reserved word in PostgreSQL
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique login name / email used as the Spring Security principal. */
    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt-hashed password — never store plain text. */
    @Column(nullable = false)
    private String password;

    /** Single role stored as a string, e.g. {@code "ROLE_USER"} or {@code "ROLE_ADMIN"}. */
    @Column(nullable = false)
    private String role;

    // -------------------------------------------------------------------------
    // UserDetails contract
    // -------------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** Account is never expired in this implementation. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Account is never locked in this implementation. */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** Credentials never expire in this implementation. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** All persisted users are considered enabled. */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
