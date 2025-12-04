package com.kis.wmsapplication.modules.userModule.dto;

import lombok.Data;

@Data
public class SubscriptionRequestDTO {
    private String loginCurrentUser;
    private String loginTargetUser;
}
