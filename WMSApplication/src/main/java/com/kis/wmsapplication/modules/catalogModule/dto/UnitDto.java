package com.kis.wmsapplication.modules.catalogModule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record UnitDto(
        Long id,

        @NotBlank(message = "Код единицы измерения обязателен")
        @Size(min = 1, max = 20, message = "Код должен быть от 1 до 20 символов")
        String code,

        @Size(max = 255, message = "Описание не может быть длиннее 255 символов")
        String description
) {}
