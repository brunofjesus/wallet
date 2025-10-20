package pt.brunojesus.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.brunojesus.wallet.dto.UserAuthRequestDTO;
import pt.brunojesus.wallet.dto.UserLoginResponseDTO;
import pt.brunojesus.wallet.dto.UserRegisterResponseDTO;
import pt.brunojesus.wallet.openapi.AuthControllerApi;
import pt.brunojesus.wallet.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController implements AuthControllerApi {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserRegisterResponseDTO> signup(
            @Valid @RequestBody UserAuthRequestDTO dto
    ) {
        UserRegisterResponseDTO response = userService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Override
    public ResponseEntity<UserLoginResponseDTO> login(
            @Valid @RequestBody UserAuthRequestDTO dto
    ) {
        UserLoginResponseDTO user = userService.login(dto);
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

}