package com.kis.wmsapplication.modules.userModule.controller;


import com.kis.wmsapplication.modules.userModule.dto.AddInfoUserDTO;
import com.kis.wmsapplication.modules.userModule.dto.AdminUserDto;
import com.kis.wmsapplication.modules.userModule.model.User;
import com.kis.wmsapplication.modules.userModule.service.RoleService;
import com.kis.wmsapplication.modules.userModule.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final RoleService roleService;


    private void checkAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("Требуется аутентификация");
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Доступ запрещен. Требуется роль ADMIN");
        }
    }


    private UUID parseCurrentUserId(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }


    @GetMapping
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "username") Pageable pageable
    ) {
        checkAdminRole();
        return ResponseEntity.ok(userService.searchUsers(query, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUser(@PathVariable UUID id) {
        checkAdminRole();
        return ResponseEntity.ok(userService.getUserForAdmin(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable UUID id,
            @RequestBody AddInfoUserDTO userDTO
    ) {
        checkAdminRole();
        return ResponseEntity.ok(userService.updateUserForAdmin(id, userDTO));
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> addRole(@PathVariable UUID id, @RequestParam String roleName) {
        checkAdminRole();
        userService.assignRole(id, roleName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/roles")
    public ResponseEntity<Void> removeRole(
            @PathVariable UUID id,
            @RequestParam String roleName,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserIdHeader) {
        checkAdminRole();
        userService.removeRole(id, roleName, parseCurrentUserId(currentUserIdHeader));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserIdHeader) {
        checkAdminRole();
        userService.deleteUserById(id, parseCurrentUserId(currentUserIdHeader));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        checkAdminRole();
        return ResponseEntity.ok(roleService.getAllRoles());
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage() == null ? "Действие запрещено" : ex.getMessage()));
    }
}