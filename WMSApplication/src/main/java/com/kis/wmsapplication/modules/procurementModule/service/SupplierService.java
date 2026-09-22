package com.kis.wmsapplication.modules.procurementModule.service;



import com.kis.wmsapplication.modules.procurementModule.dto.SupplierRequest;
import com.kis.wmsapplication.modules.procurementModule.dto.SupplierResponse;
import com.kis.wmsapplication.modules.procurementModule.model.Supplier;
import com.kis.wmsapplication.modules.procurementModule.repository.SupplierRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    private SupplierResponse mapToResponse(Supplier s) {
        return new SupplierResponse(
                s.getId(),
                s.getName(),
                s.getContactEmail(),
                s.getPhone(),
                s.getAvgLeadTimeDays(),
                s.getCompanyRole()
        );
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        // Проверка на дубликат имени
        // (Предполагается, что в репозитории есть метод boolean existsByName(String name))

        Supplier supplier = Supplier.builder()
                .name(request.name())
                .contactEmail(request.contactEmail())
                .phone(request.phone())
                .avgLeadTimeDays(request.avgLeadTimeDays())
                .companyRole(request.companyRole() != null ? request.companyRole() : com.kis.wmsapplication.modules.procurementModule.enums.CompanyRole.SUPPLIER)
                .build();

        return mapToResponse(supplierRepository.save(supplier));
    }

    public SupplierResponse getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Поставщик не найден"));
    }

    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Поставщик не найден"));

        supplier.setName(request.name());
        supplier.setContactEmail(request.contactEmail());
        supplier.setPhone(request.phone());
        supplier.setAvgLeadTimeDays(request.avgLeadTimeDays());
        if (request.companyRole() != null) {
            supplier.setCompanyRole(request.companyRole());
        }

        return mapToResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Поставщик не найден");
        }

        supplierRepository.deleteById(id);
    }
}