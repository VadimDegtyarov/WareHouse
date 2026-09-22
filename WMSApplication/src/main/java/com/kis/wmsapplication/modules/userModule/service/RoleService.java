package com.kis.wmsapplication.modules.userModule.service;

import com.kis.wmsapplication.modules.userModule.model.Role;
import com.kis.wmsapplication.modules.userModule.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;


    @PostConstruct
    @Transactional
    public void initRoles() {
        List<String> roles = Arrays.asList(
                "ADMIN",                    // Администратор - полный доступ
                "MANAGER",                  // Менеджер - управление всеми процессами
                "WAREHOUSE_OPERATOR",       // Оператор склада - работа с инвентарем и топологией
                "PROCUREMENT_SPECIALIST",   // Специалист по закупкам - управление заказами поставщикам
                "SALES_SPECIALIST",         // Специалист по продажам - управление продажами
                "ANALYST",                  // Аналитик - доступ к аналитике и отчетам
                "VIEWER"                    // Просмотрщик - только чтение данных
        );

        for (String roleName : roles) {
            try {
                if (!roleRepository.findByRole(roleName).isPresent()) {
                    Role role = new Role(roleName);
                    roleRepository.save(role);
                    log.info("Создана роль: {}", roleName);
                } else {
                    log.debug("Роль {} уже существует", roleName);
                }
            } catch (Exception e) {
                log.warn("Не удалось создать роль {}: {}. Возможно, она уже существует.", roleName, e.getMessage());
            }
        }
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleByName(String roleName) {
        return roleRepository.findByRole(roleName)
                .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));
    }
}
