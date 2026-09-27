package com.example.smartcatalog.service;

import com.example.smartcatalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * DB-backed implementation of Spring Security's {@link UserDetailsService}.
 *
 * <p>Spring Security calls {@link #loadUserByUsername(String)} during authentication
 * to fetch the principal. Because {@link com.example.smartcatalog.model.User} already
 * implements {@link UserDetails}, we can return it directly — no adapter class needed.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads the user whose username matches the supplied value.
     *
     * @param username the login name supplied by the client
     * @return the matching {@link UserDetails}
     * @throws UsernameNotFoundException if no user with that username exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username));
    }
}
