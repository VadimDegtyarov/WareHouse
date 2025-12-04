package com.kis.authservice.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.kis.authservice.dto.SignUpUserDTO;
import com.kis.authservice.model.UserAuthInfo;
import com.kis.authservice.service.AuthenticationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthenticationService authenticationService;

    @PostMapping("/create-user")
    public ResponseEntity<SignUpUserDTO> signUp(@RequestBody @Valid SignUpUserDTO signUpUserDTO) {

        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.signUp(signUpUserDTO));
    }
}
