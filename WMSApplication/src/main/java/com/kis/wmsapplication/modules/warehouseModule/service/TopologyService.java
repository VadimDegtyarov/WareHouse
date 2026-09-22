package com.kis.wmsapplication.modules.warehouseModule.service;

import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import com.kis.wmsapplication.modules.inventoryModule.repository.StockRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import com.kis.wmsapplication.modules.warehouseModule.dto.CreateLevelRequest;
import com.kis.wmsapplication.modules.warehouseModule.dto.HierarchyLevelDto;
import com.kis.wmsapplication.modules.warehouseModule.dto.HierarchyNodeDto;
import com.kis.wmsapplication.modules.warehouseModule.dto.UpdateLevelRequest;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevelCategory;
import com.kis.wmsapplication.modules.warehouseModule.model.Warehouse;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelCategoryRepository;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelRepository;
import com.kis.wmsapplication.modules.warehouseModule.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopologyService {

    private final WarehouseRepository warehouseRepository;
    private final HierarchyLevelRepository levelRepository;
    private final HierarchyLevelCategoryRepository categoryRepository;
    private final StockRepository stockRepository;

    // --- CREATE ---
    @Transactional
    public Long createLevel(CreateLevelRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Склад не найден"));

        HierarchyLevelCategory category = categoryRepository.findByLevelName(request.categoryName())
                .orElseThrow(() -> new ResourceNotFoundException("Тип уровня не найден: " + request.categoryName()));

        HierarchyLevel level = HierarchyLevel.builder()
                .warehouse(warehouse)
                .code(request.code())
                .name(request.name())
                .capacity(request.capacity())
                .category(category)
                .build();

        if (request.parentId() != null) {
            HierarchyLevel parent = levelRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Родительская ячейка не найдена"));

            if (!parent.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Родитель находится на другом складе!");
            }
            level.setParent(parent);
            
            // Валидация вместимости: сумма вместимостей дочерних элементов не должна превышать вместимость родителя
            if (request.capacity() != null && parent.getCapacity() != null) {
                BigDecimal childrenCapacity = calculateChildrenCapacity(parent);
                BigDecimal availableCapacity = parent.getCapacity().subtract(childrenCapacity);
                
                if (request.capacity().compareTo(availableCapacity) > 0) {
                    throw new IllegalArgumentException(
                        String.format("Вместимость не может превышать доступную вместимость родителя. " +
                            "Доступно: %s, запрошено: %s", availableCapacity, request.capacity())
                    );
                }
            }
        } else {
            // Если это корневой элемент, проверяем вместимость склада
            if (request.capacity() != null && warehouse.getCapacity() != null) {
                BigDecimal rootLevelsCapacity = calculateRootLevelsCapacity(warehouse.getId());
                BigDecimal availableCapacity = warehouse.getCapacity().subtract(rootLevelsCapacity);
                
                if (request.capacity().compareTo(availableCapacity) > 0) {
                    throw new IllegalArgumentException(
                        String.format("Вместимость не может превышать вместимость склада. " +
                            "Доступно: %s, запрошено: %s", availableCapacity, request.capacity())
                    );
                }
            }
        }

        return levelRepository.save(level).getId();
    }
    
    private BigDecimal calculateChildrenCapacity(HierarchyLevel parent) {
        return parent.getChildren().stream()
                .filter(child -> child.getCapacity() != null)
                .map(HierarchyLevel::getCapacity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateRootLevelsCapacity(Long warehouseId) {
        List<HierarchyLevel> rootLevels = levelRepository.findByWarehouseIdAndParentIsNull(warehouseId);
        return rootLevels.stream()
                .filter(level -> level.getCapacity() != null)
                .map(HierarchyLevel::getCapacity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- READ (SINGLE) ---
    public HierarchyLevelDto getLevelById(Long id) {
        HierarchyLevel level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ячейка не найдена"));

        return new HierarchyLevelDto(
                level.getId(),
                level.getParent() != null ? level.getParent().getId() : null,
                level.getCategory().getLevelName(),
                level.getCode(),
                level.getName(),
                level.getCapacity()
        );
    }

    // --- READ (TREE) ---
    @Transactional
    public List<HierarchyNodeDto> getWarehouseTopology(Long warehouseId) {
        List<HierarchyLevel> roots = levelRepository.findByWarehouseIdAndParentIsNull(warehouseId);
        return roots.stream()
                .map(this::mapToNode)
                .collect(Collectors.toList());
    }

    // --- UPDATE ---
    @Transactional
    public HierarchyLevelDto updateLevel(Long id, UpdateLevelRequest request) {
        HierarchyLevel level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ячейка не найдена"));

        // Валидация вместимости при обновлении
        if (request.capacity() != null) {
            if (level.getParent() != null) {
                HierarchyLevel parent = level.getParent();
                if (parent.getCapacity() != null) {
                    BigDecimal childrenCapacity = calculateChildrenCapacity(parent);
                    // Вычитаем текущую вместимость этого уровня
                    BigDecimal currentCapacity = level.getCapacity() != null ? level.getCapacity() : BigDecimal.ZERO;
                    BigDecimal availableCapacity = parent.getCapacity()
                            .subtract(childrenCapacity)
                            .add(currentCapacity);
                    
                    if (request.capacity().compareTo(availableCapacity) > 0) {
                        throw new IllegalArgumentException(
                            String.format("Вместимость не может превышать доступную вместимость родителя. " +
                                "Доступно: %s, запрошено: %s", availableCapacity, request.capacity())
                        );
                    }
                }
            } else {
                // Корневой элемент - проверяем вместимость склада
                Warehouse warehouse = level.getWarehouse();
                if (warehouse.getCapacity() != null) {
                    BigDecimal rootLevelsCapacity = calculateRootLevelsCapacity(warehouse.getId());
                    BigDecimal currentCapacity = level.getCapacity() != null ? level.getCapacity() : BigDecimal.ZERO;
                    BigDecimal availableCapacity = warehouse.getCapacity()
                            .subtract(rootLevelsCapacity)
                            .add(currentCapacity);
                    
                    if (request.capacity().compareTo(availableCapacity) > 0) {
                        throw new IllegalArgumentException(
                            String.format("Вместимость не может превышать вместимость склада. " +
                                "Доступно: %s, запрошено: %s", availableCapacity, request.capacity())
                        );
                    }
                }
            }
        }

        // Обновляем поля
        level.setCode(request.code());
        level.setName(request.name());
        level.setCapacity(request.capacity());

        // Сохраняем (Hibernate сделает update сам, но save вернет обновленную сущность)
        HierarchyLevel updated = levelRepository.save(level);

        return new HierarchyLevelDto(
                updated.getId(),
                updated.getParent() != null ? updated.getParent().getId() : null,
                updated.getCategory().getLevelName(),
                updated.getCode(),
                updated.getName(),
                updated.getCapacity()
        );
    }

    // --- DELETE ---
    @Transactional
    public void deleteLevel(Long id) {
        if (!levelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ячейка не найдена");
        }
        // Важно: Из-за CascadeType.ALL в сущности, удаление родителя удалит и всех детей!
        // Если в ячейке есть товар (Inventory), БД может выдать ошибку ConstraintViolation,
        // что хорошо (нельзя удалить полку с товаром).
        levelRepository.deleteById(id);
    }

    // Маппер для дерева
    private HierarchyNodeDto mapToNode(HierarchyLevel level) {
        // Получаем информацию о товарах в этой локации
        List<ProductLocationStock> stocks = stockRepository.findAllByLocationId(level.getId());
        
        // Считаем общее количество товаров в этой локации (только в текущей, без дочерних)
        BigDecimal localQuantity = stocks.stream()
                .map(ProductLocationStock::getQuantity)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Количество различных товаров в текущей локации
        int productCount = stocks.size();
        
        // Создаем список товаров в локации
        List<HierarchyNodeDto.StockItemDto> stockItems = stocks.stream()
                .map(stock -> new HierarchyNodeDto.StockItemDto(
                        stock.getProduct().getId(),
                        stock.getProduct().getSku(),
                        stock.getProduct().getName(),
                        stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO,
                        stock.getReserved() != null ? stock.getReserved() : BigDecimal.ZERO
                ))
                .collect(Collectors.toList());
        
        // Рекурсивно обрабатываем дочерние элементы
        List<HierarchyNodeDto> childrenDtos = level.getChildren().stream()
                .map(this::mapToNode)
                .collect(Collectors.toList());
        
        // Добавляем количество товаров из дочерних локаций
        BigDecimal childrenTotalQuantity = childrenDtos.stream()
                .filter(dto -> dto.totalQuantity() != null)
                .map(HierarchyNodeDto::totalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Общее количество (текущая локация + дочерние)
        BigDecimal finalTotalQuantity = localQuantity.add(childrenTotalQuantity);
        
        // Использование вместимости (только для текущей локации, не включая дочерние)
        // Используем localQuantity, а не finalTotalQuantity, так как capacity относится к текущей локации
        BigDecimal utilization = null;
        if (level.getCapacity() != null && level.getCapacity().compareTo(BigDecimal.ZERO) > 0) {
            // Всегда вычисляем utilization, даже если товаров нет (будет 0)
            if (localQuantity.compareTo(BigDecimal.ZERO) == 0) {
                utilization = BigDecimal.ZERO;
            } else {
                utilization = localQuantity.divide(level.getCapacity(), 4, java.math.RoundingMode.HALF_UP);
                // Ограничиваем до 1.0 (100%), если превышает
                if (utilization.compareTo(BigDecimal.ONE) > 0) {
                    utilization = BigDecimal.ONE;
                }
            }
        }

        return new HierarchyNodeDto(
                level.getId(),
                level.getCode(),
                level.getName(),
                level.getCategory().getLevelName(),
                finalTotalQuantity,
                productCount,
                utilization,
                stockItems,
                childrenDtos
        );
    }
}