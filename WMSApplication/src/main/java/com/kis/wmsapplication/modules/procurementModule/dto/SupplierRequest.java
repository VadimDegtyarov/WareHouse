package com.kis.wmsapplication.modules.procurementModule.dto;

import com.kis.wmsapplication.modules.procurementModule.enums.CompanyRole;
import jakarta.validation.constraints.*;

/**
 * DTO для создания/обновления поставщика (компании).
 */
public record SupplierRequest(
        @NotBlank(message = "Название компании обязательно")
        @Size(min = 2, max = 255, message = "Название должно быть от 2 до 255 символов")
        String name,

        @Email(message = "Некорректный формат email")
        @Size(max = 255, message = "Email не может быть длиннее 255 символов")
        String contactEmail,

        @Pattern(regexp = "^(\\+?[0-9]{7,15})?$", message = "Телефон должен содержать от 7 до 15 цифр")
        String phone,

        @NotNull(message = "Средний срок поставки обязателен")
        @Min(value = 1, message = "Срок поставки должен быть минимум 1 день")
        @Max(value = 365, message = "Срок поставки не может превышать 365 дней")
        Integer avgLeadTimeDays,

        CompanyRole companyRole
) {}
