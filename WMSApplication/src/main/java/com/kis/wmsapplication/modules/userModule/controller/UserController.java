package com.kis.wmsapplication.modules.userModule.controller;

import com.kis.wmsapplication.modules.userModule.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kis.wmsapplication.modules.userModule.dto.AddInfoUserDTO;
import com.kis.wmsapplication.modules.userModule.model.Image;
import com.kis.wmsapplication.modules.userModule.model.User;
import com.kis.wmsapplication.modules.userModule.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PatchMapping("/add-info")
    public ResponseEntity<UserDto> addInfoUser(@RequestBody AddInfoUserDTO userDTO, @RequestHeader(value = "X-User-Id")String userID){
        log.info(userID);
        UUID id = UUID.fromString(userID);

        return ResponseEntity.ok().body(userService.updateUser(id, userDTO)) ;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<Collection<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{id}/avatar")
    public void uploadAvatar(@PathVariable UUID id, @ModelAttribute Image image)
    {
        userService.uploadImage(id,image);

    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<InputStreamResource> getAvatar(@PathVariable UUID id)
    {
         return userService.getAvatar(id);
    }
    
    @GetMapping("/current-user")
    public ResponseEntity<String> getCurrentUserId(@RequestHeader(value = "X-User-Id",required = false) String userIdString) {
        if (userIdString==null||userIdString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return ResponseEntity.ok(userIdString);
        }
    }


    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo(
            @RequestHeader(value = "X-User-Id", required = false) String userIdString) {
        Map<String, Object> result = new HashMap<>();

        if (userIdString == null || userIdString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        List<String> roles = List.of();

        try {
            UUID userId = UUID.fromString(userIdString);
            User user = userService.getUserById(userId);
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("firstName", user.getFirstName());
            result.put("lastName", user.getLastName());
            result.put("birthDate", user.getBirthDate());

            if (user.getUserAuthInfo() != null) {
                result.put("email", user.getUserAuthInfo().getEmail());
                result.put("phoneNumber", user.getUserAuthInfo().getPhoneNumber());
                if (user.getUserAuthInfo().getRoles() != null) {
                    roles = user.getUserAuthInfo().getRoles().stream()
                            .map(r -> r.getRole())
                            .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception ex) {
            log.warn("Не удалось получить профиль пользователя: {}", ex.getMessage());
            result.put("id", userIdString);
        }

        if (roles.isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                roles = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                        .collect(Collectors.toList());
            }
        }
        result.put("roles", roles);

        return ResponseEntity.ok(result);
    }
}
