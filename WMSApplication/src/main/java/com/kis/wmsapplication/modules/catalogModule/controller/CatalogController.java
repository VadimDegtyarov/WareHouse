package com.kis.wmsapplication.modules.catalogModule.controller;

import com.kis.wmsapplication.modules.catalogModule.dto.CategoryDto;
import com.kis.wmsapplication.modules.catalogModule.dto.UnitDto;
import com.kis.wmsapplication.modules.catalogModule.model.Category;
import com.kis.wmsapplication.modules.catalogModule.model.Unit;
import com.kis.wmsapplication.modules.catalogModule.repository.CategoryRepository;
import com.kis.wmsapplication.modules.catalogModule.repository.UnitRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;


    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryDto> categoryDtos = categories.stream()
                .map(cat -> new CategoryDto(cat.getId(), cat.getName(), cat.getDescription()))
                .toList();
        return ResponseEntity.ok(categoryDtos);
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + id + " не найдена"));
        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(@RequestBody @Valid CategoryDto request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Категория с именем '" + request.name() + "' уже существует");
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        category = categoryRepository.save(category);
        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id, @RequestBody @Valid CategoryDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + id + " не найдена"));
        
        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Категория с именем '" + request.name() + "' уже существует");
        }
        
        category.setName(request.name());
        category.setDescription(request.description());
        category = categoryRepository.save(category);
        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Категория с ID " + id + " не найдена");
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/units")
    public ResponseEntity<List<UnitDto>> getAllUnits() {
        List<Unit> units = unitRepository.findAll();
        List<UnitDto> unitDtos = units.stream()
                .map(unit -> new UnitDto(unit.getId(), unit.getCode(), unit.getDescription()))
                .toList();
        return ResponseEntity.ok(unitDtos);
    }

    @GetMapping("/units/{id}")
    public ResponseEntity<UnitDto> getUnitById(@PathVariable Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Единица измерения с ID " + id + " не найдена"));
        return ResponseEntity.ok(new UnitDto(unit.getId(), unit.getCode(), unit.getDescription()));
    }

    @PostMapping("/units")
    public ResponseEntity<UnitDto> createUnit(@RequestBody @Valid UnitDto request) {
        if (unitRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Единица измерения с кодом '" + request.code() + "' уже существует");
        }
        Unit unit = Unit.builder()
                .code(request.code())
                .description(request.description())
                .build();
        unit = unitRepository.save(unit);
        return ResponseEntity.ok(new UnitDto(unit.getId(), unit.getCode(), unit.getDescription()));
    }

    @PutMapping("/units/{id}")
    public ResponseEntity<UnitDto> updateUnit(@PathVariable Long id, @RequestBody @Valid UnitDto request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Единица измерения с ID " + id + " не найдена"));
        
        if (!unit.getCode().equals(request.code()) && unitRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Единица измерения с кодом '" + request.code() + "' уже существует");
        }
        
        unit.setCode(request.code());
        unit.setDescription(request.description());
        unit = unitRepository.save(unit);
        return ResponseEntity.ok(new UnitDto(unit.getId(), unit.getCode(), unit.getDescription()));
    }

    @DeleteMapping("/units/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        if (!unitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Единица измерения с ID " + id + " не найдена");
        }
        unitRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

