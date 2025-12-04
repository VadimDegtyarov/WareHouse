package com.kis.wmsapplication.modules.userModule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class AddInfoUserDTO {
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
}
