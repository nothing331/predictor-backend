package api.controller;

import api.dto.auth.DemoLoginRequest;
import api.dto.auth.DemoRegisterRequest;
import api.dto.auth.TokenResponse;
import core.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/demo")
@ConditionalOnProperty(name = "app.demo.auth.enabled", havingValue = "true")
public class DemoAuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody DemoRegisterRequest request) {
        return ResponseEntity.ok(authService.demoRegister(
            request.username(),
            request.password(),
            request.email()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody DemoLoginRequest request) {
        return ResponseEntity.ok(authService.demoLogin(
            request.username(),
            request.password()
        ));
    }
}
