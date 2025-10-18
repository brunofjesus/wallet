package pt.brunojesus.wallet.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration class that defines the security filter chain and authentication rules.
 * This configuration establishes JWT-based stateless authentication with specific endpoint access controls.
 * 
 * The configuration:
 * - Disables CSRF protection (appropriate for stateless JWT authentication)
 * - Permits public access to authentication endpoints (/auth/**)
 * - Requires authentication for all other endpoints
 * - Uses stateless session management
 * - Integrates JWT token filtering before username/password authentication
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final AuthTokenFilter authTokenFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * Constructs a new SecurityConfiguration with the required security components.
     *
     * @param authTokenFilter the JWT authentication filter for processing tokens
     * @param authenticationProvider the authentication provider for user credential validation
     */
    @Autowired
    public SecurityConfiguration(
            AuthTokenFilter authTokenFilter,
            AuthenticationProvider authenticationProvider
    ) {
        this.authTokenFilter = authTokenFilter;
        this.authenticationProvider = authenticationProvider;
    }


    /**
     * Configures the security filter chain that defines how HTTP requests are secured.
     * <p>
     * This configuration:
     * <ul>
     *   <li>Disables CSRF protection since we're using stateless JWT authentication</li>
     *   <li>Allows unrestricted access to authentication endpoints (/auth/**)</li>
     *   <li>Requires authentication for all other requests</li>
     *   <li>Sets session management to stateless (no server-side sessions)</li>
     *   <li>Registers the custom authentication provider</li>
     *   <li>Adds the JWT token filter before the standard username/password filter</li>
     * </ul>
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if there's an error during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/auth/**").permitAll()
                                .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
