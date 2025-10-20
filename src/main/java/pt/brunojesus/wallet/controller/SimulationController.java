package pt.brunojesus.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.brunojesus.wallet.dto.SimulationRequestDTO;
import pt.brunojesus.wallet.dto.SimulationResultDTO;
import pt.brunojesus.wallet.service.SimulationService;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    @Autowired
    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    public ResponseEntity<SimulationResultDTO> simulate(@RequestBody @Valid SimulationRequestDTO request) {
       return ResponseEntity.ok(simulationService.simulate(request));
    }
}
