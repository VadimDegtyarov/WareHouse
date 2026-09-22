package com.kis.authservice.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.kis.authservice.dto.SignUpUserDTO;
import com.kis.authservice.service.AuthenticationService;
import com.kis.authservice.service.UserAuthInfoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthenticationService authenticationService;
    private final UserAuthInfoService userAuthInfoService;

    @PostMapping("/create-user")
    public ResponseEntity<SignUpUserDTO> signUp(@RequestBody @Valid SignUpUserDTO signUpUserDTO) {

        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.signUp(signUpUserDTO));
    }


    @PostMapping("/grant-role-self")
    public ResponseEntity<Map<String, Object>> grantRoleToSelf(@RequestParam(name = "role", defaultValue = "ADMIN") String role) {
        List<String> roles = userAuthInfoService.grantRoleToCurrentUser(role);
        Map<String, Object> body = new HashMap<>();
        body.put("granted", role);
        body.put("currentRoles", roles);
        body.put("message", "Роль назначена. Для применения изменений необходимо повторно войти в систему.");
        return ResponseEntity.ok(body);
    }
}
