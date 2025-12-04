package com.kis.wmsapplication.modules.userModule.controller;

import com.kis.wmsapplication.modules.userModule.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kis.wmsapplication.modules.userModule.dto.AddInfoUserDTO;
import com.kis.wmsapplication.modules.userModule.model.Image;
import com.kis.wmsapplication.modules.userModule.model.User;
import com.kis.wmsapplication.modules.userModule.service.UserService;

import java.security.Principal;
import java.util.Collection;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PatchMapping("/add-info")
    public ResponseEntity<UserDto> addInfoUser(@RequestBody AddInfoUserDTO userDTO, @RequestHeader(value = "X-User-Id")String userID){
        UUID id = UUID.fromString(userID);

        return ResponseEntity.ok().body(userService.updateUser(id, userDTO)) ;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<Collection<User>> getAllUsers() {
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
}
