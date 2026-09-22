package com.kis.wmsapplication.modules.salesModule.dto;

import jakarta.validation.constraints.*;

/**
 * DTO для клиента.
 * Используется как для создания, так и для ответа.
 */
public record CustomerDto(
        Long id,

        @NotBlank(message = "Имя клиента обязательно")
        @Size(min = 2, max = 255, message = "Имя должно быть от 2 до 255 символов")
        String name,

        @Email(message = "Некорректный формат email")
        @Size(max = 255, message = "Email не может быть длиннее 255 символов")
        String email,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Телефон должен содержать от 7 до 15 цифр")
        String phone,

        @Positive(message = "ID адреса должен быть положительным")
        Long addressId,

        @Min(value = 1, message = "Приоритет должен быть от 1 до 10")
        @Max(value = 10, message = "Приоритет должен быть от 1 до 10")
        Integer priority
) {}
