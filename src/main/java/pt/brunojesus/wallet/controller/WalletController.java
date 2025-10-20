package pt.brunojesus.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.brunojesus.wallet.dto.WalletAddAssetRequestDTO;
import pt.brunojesus.wallet.dto.WalletInfoDTO;
import pt.brunojesus.wallet.openapi.WalletControllerApi;
import pt.brunojesus.wallet.service.WalletService;

@RestController
@RequestMapping("/wallet")
public class WalletController implements WalletControllerApi {

    private final WalletService walletService;

    @Autowired
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public ResponseEntity<WalletInfoDTO> getWalletInfo() {
        return ResponseEntity.ok(walletService.info());
    }

    @Override
    public ResponseEntity<Void> addAsset(@RequestBody @Valid WalletAddAssetRequestDTO dto) {
        walletService.addAsset(dto);
        return ResponseEntity.ok().build();
    }


    @Override
    public ResponseEntity<Void> updateAsset(@RequestBody @Valid WalletAddAssetRequestDTO dto) {
        walletService.updateAsset(dto);
        return ResponseEntity.ok().build();
    }
}
