package pt.brunojesus.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.brunojesus.wallet.dto.WalletAddAssetDTO;
import pt.brunojesus.wallet.dto.WalletInfoDTO;
import pt.brunojesus.wallet.service.WalletService;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    @Autowired
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/info")
    public ResponseEntity<WalletInfoDTO> getWalletInfo() {
        return ResponseEntity.ok(walletService.info());
    }

    @PostMapping("/asset")
    public ResponseEntity<Void> addAsset(@RequestBody @Valid WalletAddAssetDTO dto) {
        walletService.addAsset(dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/asset")
    public ResponseEntity<Void> updateAsset(@RequestBody @Valid WalletAddAssetDTO dto) {
        walletService.updateAsset(dto);
        return ResponseEntity.ok().build();
    }
}
