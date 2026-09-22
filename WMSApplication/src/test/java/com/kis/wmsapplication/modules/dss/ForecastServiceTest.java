package com.kis.wmsapplication.modules.dss;

import com.kis.wmsapplication.modules.dss.service.ForecastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ForecastServiceTest {

    @Autowired
    private ForecastService forecastService;

    @Test
    void testForecastDemand() {
        BigDecimal forecast = forecastService.forecastDemand(1L, 30);
        
        assertNotNull(forecast);
        assertTrue(forecast.compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testCalculateOptimalStockLevel() {
        BigDecimal currentStock = BigDecimal.valueOf(50);
        BigDecimal optimalLevel = forecastService.calculateOptimalStockLevel(1L, currentStock, 7);
        
        assertNotNull(optimalLevel);
        assertTrue(optimalLevel.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testGetOptimizationRecommendations() {
        BigDecimal currentStock = BigDecimal.valueOf(50);
        BigDecimal optimalLevel = BigDecimal.valueOf(100);
        
        List<String> recommendations = forecastService.getOptimizationRecommendations(
                1L, currentStock, optimalLevel
        );
        
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }
}
