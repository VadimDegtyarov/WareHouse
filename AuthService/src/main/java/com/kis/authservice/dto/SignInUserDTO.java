package com.kis.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInUserDTO {

    @NotBlank(message = "Логин обязателен")
    @Pattern(
        regexp = "^(.+@.+\\..+|\\+?[0-9]{10,15})$", 
        message = "Логин должен быть email адресом или номером телефона"
    )
    private String login;
    
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, max = 255, message = "Пароль должен содержать минимум 8 символов")
    private String password;
}
