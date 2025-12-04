package com.kis.wmsapplication.modules.catalogModule.Service;

import com.kis.wmsapplication.modules.catalogModule.dto.ProductRequest;
import com.kis.wmsapplication.modules.catalogModule.dto.ProductResponse;
import com.kis.wmsapplication.modules.catalogModule.model.Category;
import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.model.Unit;
import com.kis.wmsapplication.modules.catalogModule.repository.CategoryRepository;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.catalogModule.repository.UnitRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    // Нам нужны репозитории для Category и Unit, чтобы найти связанные сущности по ID
    // Предполагаем, что они уже созданы:
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;

    private ProductResponse mapToResponse(Product product) {
        // Преобразуем сущности категорий в DTO, чтобы не сломать JSON
        Set<ProductResponse.CategorySummaryDto> categoryDtos = product.getCategories().stream()
                .map(cat -> new ProductResponse.CategorySummaryDto(cat.getId(), cat.getName()))
                .collect(Collectors.toSet());

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                // СППР
                product.getMinStock(),
                product.getReorderPoint(),
                product.getEoq(),
                product.getActive(),

                categoryDtos, // Передаем DTO

                product.getUnit().getId(),
                product.getUnit().getCode(),
                product.getUnit().getDescription()
        );
    }

    /**
     * Находит Category по ID или выбрасывает исключение.
     */
    private List<Category> findCategoriesById(List<UUID> categoriesId) {


        List<Category> categories = categoryRepository.findAllById(categoriesId);

        if (categories.isEmpty()) {
            throw new RuntimeException("Ни одна категория не найдена: " + categoriesId);
        }
        return categories;
    }

    /**
     * Находит Unit по ID или выбрасывает исключение.
     */
    private Unit findUnitById(UUID unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Единица измерения с ID %s не найдена".formatted(unitId)));
    }


    // --- CRUD Операции ---

    /**
     * Создает новый товар.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("Артикул %s занят".formatted(request.sku()));
        }

        List<Category> categories = findCategoriesById(request.categories());
        Unit unit = findUnitById(request.unitId());

        Product newProduct = Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                // Заполняем поля СППР
                .minStock(request.minStock())
                .reorderPoint(request.reorderPoint())
                .eoq(request.eoq())
                .supplierId(request.supplierId())
                .active(true) // По умолчанию активен

                .categories(new HashSet<>(categories))
                .unit(unit)
                .build();

        Product savedProduct = productRepository.save(newProduct);
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));

        if (!existingProduct.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("Артикул %s занят".formatted(request.sku()));
        }

        List<Category> categories = findCategoriesById(request.categories());
        Unit unit = findUnitById(request.unitId());

        existingProduct.setSku(request.sku());
        existingProduct.setName(request.name());
        existingProduct.setDescription(request.description());
        existingProduct.setPrice(request.price());

        // Обновляем СППР
        existingProduct.setMinStock(request.minStock());
        existingProduct.setReorderPoint(request.reorderPoint());
        existingProduct.setEoq(request.eoq());
        existingProduct.setSupplierId(request.supplierId());

        existingProduct.setCategories(new HashSet<>(categories));
        existingProduct.setUnit(unit);

        Product updatedProduct = productRepository.save(existingProduct);
        return mapToResponse(updatedProduct);
    }


    public ProductResponse findProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Товар с ID %s не найден".formatted(id)));
        return mapToResponse(product);
    }


    public List<ProductResponse> findAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Товар с ID %s не найден".formatted(id));
        }
        productRepository.deleteById(id);
    }
}