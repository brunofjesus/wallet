package pt.brunojesus.wallet.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pt.brunojesus.wallet.dto.UserAuthRequestDTO;
import pt.brunojesus.wallet.dto.UserLoginResponseDTO;
import pt.brunojesus.wallet.dto.UserRegisterResponseDTO;
import pt.brunojesus.wallet.exception.ErrorResponse;

@Tag(name = "Authentication", description = "User authentication endpoints")
public interface AuthControllerApi {

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with email and password"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(
                            schema = @Schema(implementation = UserRegisterResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "email": "john.doe@example.com",
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "VALIDATION_ERROR",
                                      "message": "Fields with invalid data detected",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": {
                                        "email": "Email is required",
                                        "password": "Password must be at least 6 characters"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User already exists",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "USER_ALREADY_EXISTS",
                                      "message": "User with email john.doe@example.com already exists",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/signup")
    ResponseEntity<UserRegisterResponseDTO> signup(
            @Valid @RequestBody UserAuthRequestDTO dto
    );


    @Operation(
            summary = "Login a user",
            description = "Authenticates user with email and password, returns JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User logged in successfully",
                    content = @Content(
                            schema = @Schema(implementation = UserLoginResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "email": "john.doe@example.com"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "VALIDATION_ERROR",
                                      "message": "Fields with invalid data detected",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": {
                                        "email": "Email is required",
                                        "password": "Password is required"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication failed",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "BAD_CREDENTIALS",
                                      "message": "Bad credentials",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/login")
    ResponseEntity<UserLoginResponseDTO> login(@Valid @RequestBody UserAuthRequestDTO dto);
}
