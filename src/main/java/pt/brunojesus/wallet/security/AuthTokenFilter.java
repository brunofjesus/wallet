package pt.brunojesus.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * JWT authentication filter that processes incoming HTTP requests to validate JWT tokens.
 * <p>
 * This filter extends {@link OncePerRequestFilter} to ensure it runs once per request and:
 * </p>
 * <ul>
 * <li>Extracts JWT tokens from the Authorization header</li>
 * <li>Validates the token and extracts user information</li>
 * <li>Sets up Spring Security authentication context</li>
 * <li>Handles authentication exceptions gracefully</li>
 * </ul>
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    /**
     * Constructs an AuthTokenFilter with required dependencies.
     *
     * @param jwtService service for JWT token operations
     * @param userDetailsService service for loading user details
     * @param handlerExceptionResolver resolver for handling filter exceptions
     */
    @Autowired
    public AuthTokenFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * Processes each HTTP request to authenticate users via JWT tokens.
     * <p>
     * The filter checks for a Bearer token in the Authorization header, validates it,
     * and sets up the Spring Security context if authentication is successful.
     * If no valid token is present, the request continues without authentication.
     * </p>
     *
     * <p>
     * When an exception occurs during authentication, the filter handles it gracefully by delegating to the
     * configured {@link HandlerExceptionResolver}.
     * </p>
     *
     * @param request the HTTP request
     * @param response the HTTP response  
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}

