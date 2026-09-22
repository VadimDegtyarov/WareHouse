package com.kis.wmsapplication.modules.userModule.service;

import com.kis.wmsapplication.modules.userModule.dto.AdminUserDto;
import com.kis.wmsapplication.modules.userModule.dto.UserDto;
import com.kis.wmsapplication.modules.userModule.model.Role;
import com.kis.wmsapplication.modules.userModule.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataAccessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import com.kis.wmsapplication.modules.userModule.dto.AddInfoUserDTO;
import com.kis.wmsapplication.modules.userModule.model.Image;
import com.kis.wmsapplication.modules.userModule.model.User;
import com.kis.wmsapplication.modules.userModule.model.UserAuthInfo;
import com.kis.wmsapplication.modules.userModule.repository.UserAuthInfoRepository;
import com.kis.wmsapplication.modules.userModule.repository.UserRepository;

import java.io.InputStream;
import java.net.URLConnection;
import java.time.Instant;
import java.util.*;


@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserAuthInfoRepository userAuthInfoRepository;
    private final ImageService imageService;
    private final RoleRepository roleRepository;

    private User getUserByLogin(String login) {
        if (login.contains("@")) {
            return userAuthInfoRepository.findByEmail(login)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь с почтой: %s не найден".formatted(login)))
                    .getUser();
        } else if (login.contains("+")) {
            return userAuthInfoRepository.findByPhoneNumber(login)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь с номером телефона: %s не найден".formatted(login)))
                    .getUser();
        } else {
            return userRepository.findByUsername(login)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ником: %s не найден".formatted(login)));
        }
    }

    public Page<User> searchUsers(String query, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (StringUtils.hasText(query)) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + query.toLowerCase() + "%"),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + query.toLowerCase() + "%"),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + query.toLowerCase() + "%")
                    )
            );
        }

        return userRepository.findAll(spec, pageable);
    }


    private String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        String trimmed = roleName.trim().toUpperCase();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }


    private long countAdmins() {
        return userAuthInfoRepository.findAll().stream()
                .filter(uai -> uai.getRoles() != null && uai.getRoles().stream()
                        .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getRole())))
                .count();
    }

    private boolean isUserAdmin(User user) {
        return user.getUserAuthInfo() != null
                && user.getUserAuthInfo().getRoles() != null
                && user.getUserAuthInfo().getRoles().stream()
                        .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getRole()));
    }


    @Transactional
    public void removeRole(UUID userId, String roleName, UUID currentUserId) {
        String normalized = normalizeRoleName(roleName);
        User user = getUserById(userId);

        if ("ROLE_ADMIN".equals(normalized)) {
            if (currentUserId != null && currentUserId.equals(userId)) {
                throw new IllegalStateException(
                        "Нельзя снять роль ADMIN с самого себя. Попросите другого администратора.");
            }
            if (isUserAdmin(user) && countAdmins() <= 1) {
                throw new IllegalStateException(
                        "Нельзя снять роль ADMIN с последнего администратора в системе.");
            }
        }

        boolean removed = user.getUserAuthInfo().getRoles().removeIf(r -> r.getRole().equals(normalized));
        if (removed) {
            userRepository.save(user);
            log.info("С пользователя {} снята роль {}", user.getUsername(), normalized);
        }
    }


    @Transactional
    public void deleteUserById(UUID id, UUID currentUserId) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new IllegalStateException(
                    "Нельзя удалить собственный аккаунт. Обратитесь к другому администратору.");
        }
        User user = getUserById(id);
        if (isUserAdmin(user) && countAdmins() <= 1) {
            throw new IllegalStateException(
                    "Нельзя удалить последнего администратора в системе.");
        }
        userRepository.deleteById(id);
        log.info("Удалён пользователь {} ({})", user.getUsername(), id);
    }


    @Deprecated
    @Transactional
    public void removeRole(UUID userId, String roleName) {
        removeRole(userId, roleName, null);
    }


    @Deprecated
    @Transactional
    public void deleteUserById(UUID id) {
        deleteUserById(id, null);
    }

    @Transactional
    public void assignRole(UUID userId, String roleName) {
        String normalized = normalizeRoleName(roleName);
        User user = getUserById(userId);
        Role role = roleRepository.findByRole(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Роль " + normalized + " не найдена"));

        boolean already = user.getUserAuthInfo().getRoles().stream()
                .anyMatch(r -> r.getRole().equalsIgnoreCase(normalized));
        if (already) {
            log.info("Пользователь {} уже имеет роль {}", user.getUsername(), normalized);
            return;
        }

        user.getUserAuthInfo().getRoles().add(role);
        userRepository.save(user);
        log.info("Пользователю {} выдана роль {}", user.getUsername(), normalized);
    }

    public Collection<UserDto> getAllUsers() {
        List<UserDto> usersDTO = new ArrayList<>();
        List<User> users = userRepository.findAll();
        for (User user : users) {
            usersDTO.add(UserDto.builder()
                    .id(user.getId())
                    .birthDate(user.getBirthDate())
                    .firstName(user.getFirstName())
                    .email(user.getUserAuthInfo().getEmail())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getUserAuthInfo().getPhoneNumber())
                    .username(user.getUsername())
                    .password(user.getUserAuthInfo().getPassword())
                    .build());
        }
        return usersDTO;
    }

    public AdminUserDto getUserForAdmin(UUID id) {
        User user = getUserById(id);
        return AdminUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .birthDate(user.getBirthDate())
                .email(user.getUserAuthInfo().getEmail())
                .phoneNumber(user.getUserAuthInfo().getPhoneNumber())
                .roles(user.getUserAuthInfo().getRoles())
                .active(true) // Можно добавить поле active в User если нужно
                .build();
    }

    @Transactional
    public AdminUserDto updateUserForAdmin(UUID id, AddInfoUserDTO userDTO) {
        User user = getUserById(id);
        
        Optional.ofNullable(userDTO.getUsername()).filter(StringUtils::hasText).ifPresent(user::setUsername);
        Optional.ofNullable(userDTO.getBirthDate()).ifPresent(user::setBirthDate);
        Optional.ofNullable(userDTO.getFirstName()).filter(StringUtils::hasText).ifPresent(user::setFirstName);
        Optional.ofNullable(userDTO.getLastName()).filter(StringUtils::hasText).ifPresent(user::setLastName);
        
        userRepository.save(user);
        
        return AdminUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .birthDate(user.getBirthDate())
                .email(user.getUserAuthInfo().getEmail())
                .phoneNumber(user.getUserAuthInfo().getPhoneNumber())
                .roles(user.getUserAuthInfo().getRoles())
                .active(true)
                .build();
    }

    public User getUserByEmail(String email) {
        UserAuthInfo userAuth = userAuthInfoRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with email %s not found".formatted(email)));
        return userAuth.getUser();
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with username: %s not found".formatted(username)));
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        UserAuthInfo userAuth = userAuthInfoRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User with phone: %s not found".formatted(phoneNumber)));
        return userAuth.getUser();
    }

    public HttpStatus createUser(User user) {
        try {
            userRepository.save(user);
            return HttpStatus.CREATED;
        } catch (DataAccessException ex) {
            log.error("Ошибка при сохранении пользователя: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User with id %s not found".formatted(id)));
    }

    @Transactional
    public UserDto updateUser(UUID id, AddInfoUserDTO userDTO) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Пользователь с данным id:%s не найден".formatted(id)));

            Optional.ofNullable(userDTO.getUsername()).filter(StringUtils::hasText).ifPresent(user::setUsername);
            Optional.ofNullable(userDTO.getBirthDate()).ifPresent(user::setBirthDate);
            Optional.ofNullable(userDTO.getFirstName()).filter(StringUtils::hasText).ifPresent(user::setFirstName);
            Optional.ofNullable(userDTO.getLastName()).filter(StringUtils::hasText).ifPresent(user::setLastName);

            userRepository.save(user);
            return UserDto.builder()
                    .id(user.getId())
                    .birthDate(user.getBirthDate())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .password(user.getUserAuthInfo().getPasswordHash())
                    .email(user.getUserAuthInfo().getEmail())
                    .phoneNumber(user.getUserAuthInfo().getPhoneNumber())
                    .username(user.getUsername())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обновления данных: ", e);
        }

    }


    public HttpStatus deleteUser(String email) {
        userAuthInfoRepository.findByEmail(email).ifPresent(userAuthInfoRepository::delete);
        return HttpStatus.OK;
    }


    @Transactional
    public void uploadImage(UUID id, Image image) {
        User user = getUserById(id);
        String fileName = imageService.upload(image);
        user.setAvatarURL(fileName);
        userRepository.save(user);
    }

    public ResponseEntity<InputStreamResource> getAvatar(UUID id) {
        if (id.toString().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return getImage(id);

        }
    }

    private ResponseEntity<InputStreamResource> getImage(UUID id) {
        User user = getUserById(id);
        InputStream inputStream = imageService.getImage(user.getAvatarURL());
        String mimeType = URLConnection.guessContentTypeFromName(user.getAvatarURL());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(new InputStreamResource(inputStream));
    }

    public HttpStatus setBirth(String loginCurrentUser, Instant birthDate) {
        User user = getUserByLogin(loginCurrentUser);
        user.setBirthDate(birthDate);
        userRepository.save(user);
        return HttpStatus.OK;
    }

    public HttpStatus setFirstName(String loginCurrentUser, String newFirstName) {
        User user = getUserByLogin(loginCurrentUser);
        user.setFirstName(newFirstName);
        userRepository.save(user);
        return HttpStatus.OK;
    }

    public HttpStatus setLastName(String loginCurrentUser, String newLastName) {
        User user = getUserByLogin(loginCurrentUser);
        user.setLastName(newLastName);
        userRepository.save(user);
        return HttpStatus.OK;
    }

    public HttpStatus setAvatarURL(String loginCurrentUser, String avatarURL) {
        User user = getUserByLogin(loginCurrentUser);
        user.setAvatarURL(avatarURL);
        userRepository.save(user);
        return HttpStatus.OK;
    }
}
