package com.kis.wmsapplication.modules.salesModule.controller;

import com.kis.wmsapplication.modules.procurementModule.model.Supplier;
import com.kis.wmsapplication.modules.procurementModule.repository.SupplierRepository;
import com.kis.wmsapplication.modules.salesModule.dto.CustomerDto;
import com.kis.wmsapplication.modules.salesModule.model.Customer;
import com.kis.wmsapplication.modules.salesModule.repository.CustomerRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import com.kis.wmsapplication.modules.warehouseModule.model.Location;
import com.kis.wmsapplication.modules.warehouseModule.repository.LocationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sales/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final LocationRepository locationRepository;

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        List<CustomerDto> customerDtos = customers.stream()
                .map(customer -> new CustomerDto(
                        customer.getId(),
                        customer.getName(),
                        customer.getEmail(),
                        customer.getPhone(),
                        customer.getAddressId() != null ? customer.getAddressId() : null,
                        customer.getPriority()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(customerDtos);
    }

    @GetMapping("/suppliers")
    public ResponseEntity<List<CustomerDto>> getSuppliersAsCustomers() {
        // Возвращаем поставщиков как потенциальных клиентов
        List<Supplier> suppliers = supplierRepository.findAll();
        List<CustomerDto> supplierDtos = suppliers.stream()
                .map(supplier -> new CustomerDto(
                        supplier.getId(),
                        supplier.getName(),
                        supplier.getContactEmail(),
                        supplier.getPhone(),
                        1L,
                        1
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(supplierDtos);
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody @Valid CustomerDto customerDto) {
        Location address = null;
        if (customerDto.addressId() != null) {
            address = locationRepository.findById(customerDto.addressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Адрес с ID %s не найден".formatted(customerDto.addressId())));
        }
        
        Customer customer = Customer.builder()
                .name(customerDto.name())
                .email(customerDto.email())
                .phone(customerDto.phone())
                .addressId(address)
                .priority(customerDto.priority() != null ? customerDto.priority() : 1)
                .build();
        customer = customerRepository.save(customer);
        return ResponseEntity.ok(new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddressId() != null ? customer.getAddressId() : null,
                customer.getPriority()
        ));
    }
}

