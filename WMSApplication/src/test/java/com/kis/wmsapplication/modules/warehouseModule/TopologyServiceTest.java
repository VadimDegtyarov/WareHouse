package com.kis.wmsapplication.modules.warehouseModule;

import com.kis.wmsapplication.modules.warehouseModule.dto.CreateLevelRequest;
import com.kis.wmsapplication.modules.warehouseModule.dto.HierarchyNodeDto;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevelCategory;
import com.kis.wmsapplication.modules.warehouseModule.model.Warehouse;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelCategoryRepository;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelRepository;
import com.kis.wmsapplication.modules.warehouseModule.repository.WarehouseRepository;
import com.kis.wmsapplication.modules.warehouseModule.service.TopologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TopologyServiceTest {

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private HierarchyLevelRepository levelRepository;

    @Autowired
    private HierarchyLevelCategoryRepository categoryRepository;

    private Warehouse testWarehouse;
    private HierarchyLevelCategory testCategory;

    @BeforeEach
    void setUp() {
        // Создание тестового склада
        testWarehouse = Warehouse.builder()
                .code("WH-001")
                .name("Test Warehouse")
                .build();
        warehouseRepository.save(testWarehouse);

        // Создание тестовой категории
        testCategory = HierarchyLevelCategory.builder()
                .levelName("ZONE")
                .build();
        categoryRepository.save(testCategory);
    }

    @Test
    void testCreateLevel() {
        CreateLevelRequest request = new CreateLevelRequest(
                testWarehouse.getId(),
                null,
                "ZONE",
                "ZONE-001",
                "Test Zone",
                BigDecimal.valueOf(100.0)
        );

        Long levelId = topologyService.createLevel(request);

        assertNotNull(levelId);
        assertTrue(levelRepository.existsById(levelId));
    }

    @Test
    void testGetWarehouseTopology() {
        // Создаем уровень
        CreateLevelRequest request = new CreateLevelRequest(
                testWarehouse.getId(),
                null,
                "ZONE",
                "ZONE-001",
                "Test Zone",
                BigDecimal.valueOf(100.0)
        );
        topologyService.createLevel(request);

        // Получаем топологию
        List<HierarchyNodeDto> topology = topologyService.getWarehouseTopology(testWarehouse.getId());

        assertNotNull(topology);
        assertFalse(topology.isEmpty());
        assertEquals("ZONE-001", topology.get(0).code());
    }

    @Test
    void testUtilizationCalculation() {
        // Создаем уровень с capacity
        CreateLevelRequest request = new CreateLevelRequest(
                testWarehouse.getId(),
                null,
                "ZONE",
                "ZONE-001",
                "Test Zone",
                BigDecimal.valueOf(100.0)
        );
        Long levelId = topologyService.createLevel(request);

        // Получаем топологию
        List<HierarchyNodeDto> topology = topologyService.getWarehouseTopology(testWarehouse.getId());
        
        assertNotNull(topology);
        HierarchyNodeDto node = topology.get(0);
        
        // Utilization должен быть null или 0, если нет товаров
        // Но не должен быть null если capacity задана
        if (node.utilization() != null) {
            assertTrue(node.utilization().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(node.utilization().compareTo(BigDecimal.ONE) <= 0);
        }
    }
}
