package com.kis.wmsapplication.modules.userModule.controller;


import com.kis.wmsapplication.modules.userModule.model.User;
import com.kis.wmsapplication.modules.userModule.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    // Поиск пользователей (Пагинация + Сортировка + Фильтр)
    @GetMapping
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "username") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.searchUsers(query, pageable));
    }

    //  Выдача роли
    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> addRole(@PathVariable UUID id, @RequestParam String roleName) {
        userService.assignRole(id, roleName);
        return ResponseEntity.ok().build();
    }

    // Отзыв роли
    @DeleteMapping("/{id}/roles")
    public ResponseEntity<Void> removeRole(@PathVariable UUID id, @RequestParam String roleName) {
        userService.removeRole(id, roleName);
        return ResponseEntity.ok().build();
    }

    // 4. Удаление (Бан)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}