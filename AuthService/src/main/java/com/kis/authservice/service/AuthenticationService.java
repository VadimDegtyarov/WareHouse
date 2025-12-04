package com.kis.authservice.service;

import com.kis.authservice.dto.SignInUserDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.kis.authservice.dto.SignUpUserDTO;
import com.kis.authservice.model.Role;
import com.kis.authservice.model.User;
import com.kis.authservice.model.UserAuthInfo;
import com.kis.authservice.repository.RolesRepository;
import com.kis.authservice.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserAuthInfoService userAuthInfoService;
    private final PasswordEncoder passwordEncoder;
    private final RolesRepository rolesRepository;
    private final UserRepository userRepository;
    @Transactional
    public SignUpUserDTO signUp(SignUpUserDTO sigUpUserDTO) {
        try {
            Optional<Role> userRole = rolesRepository.findByRole("ROLE_USER");
            User user = new User();
            userRepository.save(user);
            UserAuthInfo myUser = UserAuthInfo.builder()
                    .phoneNumber(sigUpUserDTO.getPhoneNumber())
                    .roles(new ArrayList<>(List.of(userRole.get())))
                    .email(sigUpUserDTO.getEmail())
                    .user(user)
                    .passwordHash(passwordEncoder.encode(sigUpUserDTO.getPassword()))
                    .build();

            userAuthInfoService.create(myUser);
            return new SignUpUserDTO(myUser.getEmail(),myUser.getPhoneNumber(),myUser.getPasswordHash());
        } catch (Exception e) {
            log.error("Ошибка регистрации:{}", e.getMessage(),e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

    }

}
