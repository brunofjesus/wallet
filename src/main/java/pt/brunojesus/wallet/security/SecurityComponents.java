package pt.brunojesus.wallet.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pt.brunojesus.wallet.repository.UserRepository;

/**
 * Configuration class that provides Spring Security components for authentication and authorization.
 * This class defines beans for user details service, password encoding, authentication management,
 * and authentication provider configuration.
 */
@Configuration
public class SecurityComponents {

    private final UserRepository userRepository;

    /**
     * SecurityComponents constructor.
     *
     * @param userRepository the repository for accessing user data
     */
    @Autowired
    public SecurityComponents(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a UserDetailsService bean that loads user-specific data.
     * <p>
     * Used by Spring Security to load user data during authentication.
     *
     * @return a UserDetailsService implementation that loads users by email
     * @throws UsernameNotFoundException if the user is not found in the repository
     */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Creates a BCryptPasswordEncoder bean for password hashing and verification.
     *
     * @return a BCryptPasswordEncoder instance for password encoding
     */
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates an AuthenticationManager bean from the AuthenticationConfiguration.
     *
     * @param config the authentication configuration provided by Spring Security
     * @return the configured AuthenticationManager
     *
     * @throws Exception if there's an error retrieving the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Creates a DaoAuthenticationProvider bean that handles authentication using
     * user details from a data source.
     *
     * @return a configured DaoAuthenticationProvider for database-based authentication
     */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

}
