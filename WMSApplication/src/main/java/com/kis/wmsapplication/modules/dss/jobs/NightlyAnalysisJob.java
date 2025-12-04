package com.kis.wmsapplication.modules.dss.jobs;


import com.kis.wmsapplication.modules.dss.service.ReplenishmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NightlyAnalysisJob {

    private final ReplenishmentService replenishmentService;

    // cron = "секунды минуты часы день месяц день недели"
    @Scheduled(cron = "0 0 2 * * *")
    public void runNightlyJob() {
        replenishmentService.runAnalysisAndCreateOrders();
    }

    // Для тестов запуск каждую минуту
    @Scheduled(fixedRate = 60000)
    public void runTestJob() {
        replenishmentService.runAnalysisAndCreateOrders();
     }
}