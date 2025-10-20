package pt.brunojesus.wallet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pt.brunojesus.wallet.dto.UserLoginResponseDTO;
import pt.brunojesus.wallet.dto.UserAuthRequestDTO;
import pt.brunojesus.wallet.dto.UserRegisterResponseDTO;
import pt.brunojesus.wallet.entity.User;
import pt.brunojesus.wallet.exception.UserAlreadyExistsException;
import pt.brunojesus.wallet.repository.UserRepository;
import pt.brunojesus.wallet.security.JwtService;

import java.util.Objects;

/**
 * Service class for user management operations including registration, authentication, and user retrieval.
 * Handles user registration with password encoding, JWT-based authentication, and provides access to the
 * currently authenticated user from the security context.
 *
 * @author bruno
 * @see UserRepository
 * @see PasswordEncoder
 * @see AuthenticationManager
 * @see JwtService
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Constructs a UserService with the required dependencies for user management operations.
     *
     * @param userRepository the repository for user data access operations
     * @param passwordEncoder the encoder for securely hashing user passwords  
     * @param authenticationManager the Spring Security authentication manager for user login
     * @param jwtService the service for JWT token generation and management
     */
    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user.
     * @param dto The AuthRequestDTO with the user data.
     * @return The UserRegisterResponseDTO with the user id and email.
     */
    public UserRegisterResponseDTO register(UserAuthRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + dto.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        User savedUser = userRepository.save(user);

        return new UserRegisterResponseDTO(savedUser.getId(), savedUser.getEmail());
    }

    /**
     * Performs a user login and returns a JWT token.
     * @param dto The AuthRequestDTO with the login credentials.
     * @return The UserLoginResponseDTO with the JWT token.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException if the credentials are invalid.
     * @throws UsernameNotFoundException if the user is not found.
     * @throws AuthenticationException if the login fails.
     */
    public UserLoginResponseDTO login(UserAuthRequestDTO dto) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );
        if (auth.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return new UserLoginResponseDTO(
                    jwtService.generateToken(user)
            );
        }

        throw new AuthenticationServiceException("User cannot be authenticated");
    }

    /**
     * Gets the currently authenticated user from the security context.
     * @return The currently authenticated User entity.
     * @throws UsernameNotFoundException if the user is not found in the database.
     * @throws AuthenticationException if no authenticated user is found.
     */
    @NonNull
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String email = ((UserDetails) authentication.getPrincipal()).getUsername();
            return userRepository.findByEmail(email)
                    .filter(u -> Objects.nonNull(u.getId()))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }
        throw new AuthenticationServiceException("No authenticated user found");
    }
}