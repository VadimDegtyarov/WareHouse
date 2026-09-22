package com.kis.wmsapplication.modules.userModule.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private Instant birthDate;
    private String email;
    private String phoneNumber;
    private String password;
}
