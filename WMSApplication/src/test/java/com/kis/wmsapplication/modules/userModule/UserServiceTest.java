package com.kis.wmsapplication.modules.userModule;

import com.kis.wmsapplication.modules.userModule.model.Role;
import com.kis.wmsapplication.modules.userModule.model.User;
import com.kis.wmsapplication.modules.userModule.model.UserAuthInfo;
import com.kis.wmsapplication.modules.userModule.repository.RoleRepository;
import com.kis.wmsapplication.modules.userModule.repository.UserAuthInfoRepository;
import com.kis.wmsapplication.modules.userModule.repository.UserRepository;
import com.kis.wmsapplication.modules.userModule.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthInfoRepository userAuthInfoRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // Создание тестового пользователя
        testUser = User.builder()
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .build();

        UserAuthInfo authInfo = UserAuthInfo.builder()
                .email("test@example.com")
                .phoneNumber("+1234567890")
                .passwordHash("hashedpassword")
                .roles(new ArrayList<>())
                .build();

        testUser.setUserAuthInfo(authInfo);
        authInfo.setUser(testUser);

        // Создание тестовой роли
        testRole = new Role("TEST_ROLE");
        roleRepository.save(testRole);

        userRepository.save(testUser);
    }

    @Test
    void testAssignRole() {
        UUID userId = testUser.getId();
        
        userService.assignRole(userId, "TEST_ROLE");
        
        User updatedUser = userService.getUserById(userId);
        assertTrue(updatedUser.getUserAuthInfo().getRoles().stream()
                .anyMatch(r -> r.getRole().equals("TEST_ROLE")));
    }

    @Test
    void testRemoveRole() {
        UUID userId = testUser.getId();
        
        // Сначала назначаем роль
        userService.assignRole(userId, "TEST_ROLE");
        
        // Затем удаляем
        userService.removeRole(userId, "TEST_ROLE");
        
        User updatedUser = userService.getUserById(userId);
        assertFalse(updatedUser.getUserAuthInfo().getRoles().stream()
                .anyMatch(r -> r.getRole().equals("TEST_ROLE")));
    }

    @Test
    void testGetUserById() {
        UUID userId = testUser.getId();
        User foundUser = userService.getUserById(userId);
        
        assertNotNull(foundUser);
        assertEquals("testuser", foundUser.getUsername());
    }

    @Test
    void testDeleteUser() {
        UUID userId = testUser.getId();
        
        userService.deleteUserById(userId);
        
        assertFalse(userRepository.existsById(userId));
    }
}
