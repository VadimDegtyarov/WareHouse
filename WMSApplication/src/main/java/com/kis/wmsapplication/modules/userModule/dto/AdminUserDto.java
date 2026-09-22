package com.kis.wmsapplication.modules.userModule.dto;

import com.kis.wmsapplication.modules.userModule.model.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Data
@Builder
public class AdminUserDto {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private Instant birthDate;
    private String email;
    private String phoneNumber;
    private Collection<Role> roles;
    private boolean active;
}
