package pt.brunojesus.wallet.exception;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.brunojesus.wallet.price.AssetPriceFetchingException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ApiResponse(
            responseCode = "409", description = "Validation error",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"VALIDATION_ERROR\"," +
                                    "\"message\": \"Fields with invalid data detected\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": {" +
                                    "    \"email\": \"Email is required\"," +
                                    "    \"password\": \"Password is required\"" +
                                    "  }" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ApiResponse(
            responseCode = "400", description = "Validation error",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"VALIDATION_ERROR\"," +
                                    "\"message\": \"Fields with invalid data detected\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": {" +
                                    "    \"email\": \"Email is required\"," +
                                    "    \"quantity\": \"must be greater than 0\"" +
                                    "  }" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Method argument validation error: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "Fields with invalid data detected", details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ApiResponse(
            responseCode = "405", description = "HTTP method not allowed",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"METHOD_NOT_SUPPORTED\"," +
                                    "\"message\": \"HTTP method 'DELETE' is not supported for this endpoint\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": {" +
                                    "    \"supportedMethods\": \"[GET, POST, PUT]\"" +
                                    "  }" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.error("HTTP method not supported: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            details.put("supportedMethods", ex.getSupportedHttpMethods().toString());
        }

        String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());
        ErrorResponse errorResponse = new ErrorResponse("METHOD_NOT_SUPPORTED", message, details);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ApiResponse(
            responseCode = "400", description = "Malformed JSON request",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"MALFORMED_REQUEST\"," +
                                    "\"message\": \"Malformed JSON request\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": {" +
                                    "    \"cause\": \"Unexpected character ('}' (code 125)): was expecting double-quote to start field name\"" +
                                    "  }" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("HTTP message not readable: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        Throwable cause = ex.getCause();
        if (cause != null) {
            details.put("cause", cause.getMessage());
        }

        String message = "Malformed JSON request";
        ErrorResponse errorResponse = new ErrorResponse("MALFORMED_REQUEST", message, details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ApiResponse(
            responseCode = "415", description = "Unsupported media type",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"UNSUPPORTED_MEDIA_TYPE\"," +
                                    "\"message\": \"Content type not supported.\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": {" +
                                    "    \"providedContentType\": \"text/plain\"," +
                                    "    \"supportedMediaTypes\": \"[application/json]\"" +
                                    "  }" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.error("HTTP media type not supported: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getContentType() != null) {
            details.put("providedContentType", ex.getContentType().toString());
        }
        if (!ex.getSupportedMediaTypes().isEmpty()) {
            details.put("supportedMediaTypes", ex.getSupportedMediaTypes().toString());
        }

        String message = "Content type not supported.";
        ErrorResponse errorResponse = new ErrorResponse("UNSUPPORTED_MEDIA_TYPE", message, details);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    @ExceptionHandler(AssetPriceFetchingException.class)
    @ApiResponse(
            responseCode = "503", description = "Service unavailable - unable to fetch asset price",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"PRICE_FETCH_ERROR\"," +
                                    "\"message\": \"Unable to fetch price for asset BTC from external service\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleAssetPriceFetchingException(AssetPriceFetchingException ex) {
        log.error("Asset price fetching error: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("PRICE_FETCH_ERROR", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ApiResponse(
            responseCode = "409", description = "User already exists",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"USER_ALREADY_EXISTS\"," +
                                    "\"message\": \"User with email john.doe@example.com already exists\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.error("User already exists: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("USER_ALREADY_EXISTS", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ApiResponse(
            responseCode = "401", description = "Authentication failed",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"BAD_CREDENTIALS\"," +
                                    "\"message\": \"Bad credentials\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.error("Bad credentials: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("BAD_CREDENTIALS", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ApiResponse(
            responseCode = "401", description = "JWT token has expired",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"JWT_EXPIRED\"," +
                                    "\"message\": \"JWT token has expired\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException ex) {
        log.error("JWT token expired: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("JWT_EXPIRED", "JWT token has expired", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(MalformedJwtException.class)
    @ApiResponse(
            responseCode = "401", description = "JWT token is malformed",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"JWT_MALFORMED\"," +
                                    "\"message\": \"JWT token is malformed\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(MalformedJwtException ex) {
        log.error("Malformed JWT token: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("JWT_MALFORMED", "JWT token is malformed", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(UnsupportedJwtException.class)
    @ApiResponse(
            responseCode = "401", description = "JWT token is unsupported",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"JWT_UNSUPPORTED\"," +
                                    "\"message\": \"JWT token is unsupported\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleUnsupportedJwtException(UnsupportedJwtException ex) {
        log.error("Unsupported JWT token: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("JWT_UNSUPPORTED", "JWT token is unsupported", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(SignatureException.class)
    @ApiResponse(
            responseCode = "401", description = "JWT signature is invalid",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"JWT_SIGNATURE_INVALID\"," +
                                    "\"message\": \"JWT signature is invalid\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleSignatureException(SignatureException ex) {
        log.error("JWT signature invalid: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("JWT_SIGNATURE_INVALID", "JWT signature is invalid", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ApiResponse(
            responseCode = "401", description = "Authentication failed",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"AUTHENTICATION_FAILED\"," +
                                    "\"message\": \"Authentication failed\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "Authentication failed", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AssetAlreadyExistsException.class)
    @ApiResponse(
            responseCode = "409", description = "Asset already exists in wallet",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"ASSET_ALREADY_EXISTS\"," +
                                    "\"message\": \"Asset BTC already exists in wallet\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleAssetAlreadyExists(AssetAlreadyExistsException ex) {
        log.error("Asset already exists: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("ASSET_ALREADY_EXISTS", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(AssetNotFoundException.class)
    @ApiResponse(
            responseCode = "404", description = "Asset not found",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"ASSET_NOT_FOUND\"," +
                                    "\"message\": \"Asset with symbol XYZ not found\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleAssetNotFound(AssetNotFoundException ex) {
        log.error("Asset not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("ASSET_NOT_FOUND", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    @ApiResponse(
            responseCode = "500", description = "Internal server error",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value =
                            "{" +
                                    "\"error\": \"INTERNAL_ERROR\"," +
                                    "\"message\": \"An unexpected error occurred\"," +
                                    "\"timestamp\": \"2025-10-20T22:10:28.117844971\"," +
                                    "\"details\": null" +
                                    "}")
            )
    )
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        ErrorResponse errorResponse = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}