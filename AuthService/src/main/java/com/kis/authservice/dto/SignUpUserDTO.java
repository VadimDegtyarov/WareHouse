package com.kis.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignUpUserDTO {
    
    @NotBlank(message = "Адрес электронной почты обязателен")
    @Email(message = "Адрес электронной почты должен быть в формате user@example.com")
    @Size(min = 5, max = 255, message = "Email должен содержать от 5 до 255 символов")
    private String email;
    
    @Pattern(regexp = "^(\\+?[0-9]{10,15})?$", message = "Телефон должен содержать от 10 до 15 цифр")
    private String phoneNumber;
    
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, max = 255, message = "Пароль должен содержать минимум 8 символов")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Пароль должен содержать минимум одну заглавную букву, одну строчную букву и одну цифру"
    )
    private String password;
}
