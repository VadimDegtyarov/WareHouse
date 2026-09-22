package com.kis.wmsapplication.modules.procurementModule.dto;

import com.kis.wmsapplication.modules.procurementModule.enums.CompanyRole;
import java.util.UUID;

public record SupplierResponse(
        Long id,
        String name,
        String contactEmail,
        String phone,
        Integer avgLeadTimeDays,
        CompanyRole companyRole
) {}