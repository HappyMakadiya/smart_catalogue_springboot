package com.example.smartcatalog.repository;

import com.example.smartcatalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations and a custom finder used by
 * {@link com.example.smartcatalog.service.CustomUserDetailsService} to load
 * a user by their login name (username / email).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique username.
     *
     * @param username the login name to search for
     * @return an {@link Optional} containing the matching {@link User}, or empty if not found
     */
    Optional<User> findByUsername(String username);
}
