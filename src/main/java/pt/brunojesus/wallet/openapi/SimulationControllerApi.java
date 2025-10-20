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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pt.brunojesus.wallet.dto.SimulationRequestDTO;
import pt.brunojesus.wallet.dto.SimulationResultDTO;
import pt.brunojesus.wallet.exception.ErrorResponse;

@Tag(name = "Simulation", description = "Portfolio simulation endpoints")
@SecurityRequirement(name = "bearerAuth")
public interface SimulationControllerApi {

    @Operation(
            summary = "Simulate portfolio performance",
            description = "Simulates how the current wallet would have performed over a specified time period with historical data"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Simulation completed successfully",
                    content = @Content(
                            schema = @Schema(implementation = SimulationResultDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2024-08-10T22:00:00Z",
                                      "total": 102849.95834051541,
                                      "bestAsset": "ETH",
                                      "bestPerformance": 641.5977,
                                      "worstAsset": "BTC",
                                      "worstPerformance": 83.452,
                                      "assets": [
                                        {
                                          "symbol": "BTC",
                                          "quantity": 1.5,
                                          "price": 61150.66196935916,
                                          "value": 91725.99295403872
                                        },
                                        {
                                          "symbol": "ETH",
                                          "quantity": 4.25,
                                          "price": 2617.403620347455,
                                          "value": 11123.965386476684
                                        }
                                      ]
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
                                        "startDate": "Start date is required",
                                        "endDate": "End date must be after start date"
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
                    responseCode = "503",
                    description = "Service unavailable - unable to fetch historical asset prices",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "PRICE_FETCH_ERROR",
                                      "message": "Unable to fetch historical price for asset BTC from external service",
                                      "timestamp": "2025-10-20T22:10:28.117844971",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @PostMapping
    ResponseEntity<SimulationResultDTO> simulate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "1723327200",
                                      "assets": [
                                        {
                                          "symbol": "BTC",
                                          "quantity": 1.5,
                                          "value": 50000
                                        },
                                        {
                                          "symbol": "ETH",
                                          "quantity": 4.25,
                                          "value": 1500
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
            @RequestBody @Valid SimulationRequestDTO request
    );
}
