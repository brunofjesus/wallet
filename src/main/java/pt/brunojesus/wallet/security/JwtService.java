package pt.brunojesus.wallet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for handling JWT token operations including generation, validation, and claim extraction.
 * <p>
 * This service provides functionality to:
 * </p>
 * <ul>
 * <li>Generate JWT tokens for authenticated users</li>
 * <li>Validate JWT tokens and check their expiration</li>
 * <li>Extract claims and user information from tokens</li>
 * <li>Manage token expiration and signing keys</li>
 * </ul>
 */
@Service
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    /**
     * Extracts the username (subject) from a JWT token.
     * 
     * @param token the JWT token to extract username from
     * @return the username stored in the token's subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Checks if a JWT token has expired.
     *
     * @param token the JWT token to check for expiration
     * @return true if the token has expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token the JWT token to extract expiration from
     * @return the expiration date of the token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from a JWT token using a claims resolver function.
     * 
     * @param <T> the type of the claim value
     * @param token the JWT token to extract the claim from
     * @param claimsResolver function to resolve the specific claim from all claims
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for the given user details with no additional claims.
     * 
     * @param userDetails the user details to create the token for
     * @return a signed JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token for the given user details with additional custom claims.
     * 
     * @param extraClaims additional claims to include in the token
     * @param userDetails the user details to create the token for
     * @return a signed JWT token string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Builds a JWT token with the specified claims, user details, and expiration time.
     * 
     * @param extraClaims additional claims to include in the token
     * @param userDetails the user details to create the token for
     * @param expiration expiration time in milliseconds from now
     * @return a signed JWT token string
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validates a JWT token by checking if the username matches and the token is not expired.
     * 
     * @param token the JWT token to validate
     * @param userDetails the user details to validate against
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Extracts all claims from a JWT token.
     * 
     * @param token the JWT token to parse
     * @return all claims contained in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Creates a signing key from the configured secret key for JWT token signing and verification.
     * 
     * @return the HMAC secret key for JWT operations
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
