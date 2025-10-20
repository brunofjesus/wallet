package pt.brunojesus.wallet.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pt.brunojesus.wallet.dto.WalletAddAssetRequestDTO;
import pt.brunojesus.wallet.dto.WalletInfoDTO;
import pt.brunojesus.wallet.exception.ErrorResponse;

@Tag(name = "Wallet", description = "Wallet management endpoints")
@SecurityRequirement(name = "bearerAuth")
public interface WalletControllerApi {

    @Operation(
            summary = "Get wallet information",
            description = "Retrieves current wallet information including all assets, quantities, current prices and total value"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet information retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = WalletInfoDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "67e9fe7c-4ecf-44b7-9b67-d61d7f7da733",
                                      "original": {
                                        "total": 158000,
                                        "assets": [
                                          {
                                            "symbol": "ETH",
                                            "quantity": 2,
                                            "price": 4000,
                                            "value": 8000
                                          },
                                          {
                                            "symbol": "BTC",
                                            "quantity": 1.5,
                                            "price": 100000,
                                            "value": 150000
                                          }
                                        ]
                                      },
                                      "current": {
                                        "total": 156000,
                                        "assets": [
                                          {
                                            "symbol": "ETH",
                                            "quantity": 2,
                                            "price": 3000,
                                            "value": 6000,
                                            "timestamp": "2025-10-20T20:44:46.149839Z"
                                          },
                                          {
                                            "symbol": "BTC",
                                            "quantity": 1.5,
                                            "price": 100000,
                                            "value": 150000,
                                            "timestamp": "2025-10-20T20:44:46.150766Z"
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "JWT_EXPIRED",
                                      "message": "JWT token has expired",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Asset not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "ASSET_NOT_FOUND",
                                      "message": "No asset found for symbol: BTC",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service unavailable - unable to fetch current asset prices",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "PRICE_FETCH_ERROR",
                                      "message": "Unable to fetch price for asset BTC from external service",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/info")
    ResponseEntity<WalletInfoDTO> getWalletInfo();

    @Operation(
            summary = "Add asset to wallet",
            description = "Adds a new asset with specified quantity to the user's wallet"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Asset added successfully"
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
                                        "symbol": "Symbol is required",
                                        "quantity": "must be greater than 0"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "JWT_EXPIRED",
                                      "message": "JWT token has expired",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Asset not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "ASSET_NOT_FOUND",
                                      "message": "Asset with symbol XYZ not found",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Asset already exists in wallet",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "ASSET_ALREADY_EXISTS",
                                      "message": "User already has asset: BTC",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/asset")
    ResponseEntity<Void> addAsset(@RequestBody @Valid WalletAddAssetRequestDTO dto);


    @Operation(
            summary = "Update asset quantity in wallet",
            description = "Updates the quantity of an existing asset in the user's wallet"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Asset quantity updated successfully"
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
                                        "symbol": "Symbol is required",
                                        "quantity": "must be greater than 0"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "JWT_EXPIRED",
                                      "message": "JWT token has expired",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Asset not found in wallet",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "ASSET_NOT_FOUND",
                                      "message": "Asset with symbol BTC not found in wallet",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @PutMapping("/asset")
    ResponseEntity<Void> updateAsset(@RequestBody @Valid WalletAddAssetRequestDTO dto);
}
