package com.kis.wmsapplication.modules.catalogModule.Service;

import com.kis.wmsapplication.modules.catalogModule.dto.ProductRequest;
import com.kis.wmsapplication.modules.catalogModule.dto.ProductResponse;
import com.kis.wmsapplication.modules.catalogModule.model.Category;
import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.model.Unit;
import com.kis.wmsapplication.modules.catalogModule.repository.CategoryRepository;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.catalogModule.repository.UnitRepository;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStockId;
import com.kis.wmsapplication.modules.inventoryModule.repository.StockRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final StockRepository stockRepository;
    private final HierarchyLevelRepository hierarchyLevelRepository;
    private final com.kis.wmsapplication.modules.procurementModule.repository.SupplierRepository supplierRepository;

    private ProductResponse mapToResponse(Product product) {
        Set<ProductResponse.CategorySummaryDto> categoryDtos = product.getCategories().stream()
                .map(cat -> new ProductResponse.CategorySummaryDto(cat.getId(), cat.getName()))
                .collect(Collectors.toSet());

        List<ProductLocationStock> stocks = stockRepository.findAllByProductId(product.getId());
        BigDecimal currentStock = stocks.stream()
                .map(ProductLocationStock::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reservedStock = stocks.stream()
                .map(ProductLocationStock::getReserved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableStock = currentStock.subtract(reservedStock);

        String supplierName = null;
        if (product.getSupplierId() != null) {
            supplierName = supplierRepository.findById(product.getSupplierId())
                    .map(s -> s.getName())
                    .orElse(null);
        }

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                currentStock,
                reservedStock,
                availableStock,
                product.getMinStock(),
                product.getMaxStock(),
                product.getReorderPoint(),
                product.getEoq(),
                product.getActive(),
                product.getSupplierId(),
                supplierName,

                categoryDtos,

                product.getUnit().getId(),
                product.getUnit().getCode(),
                product.getUnit().getDescription()
        );
    }


    private List<Category> findCategoriesById(List<Long> categoriesId) {


        List<Category> categories = categoryRepository.findAllById(categoriesId);

        if (categories.isEmpty()) {
            throw new RuntimeException("Ни одна категория не найдена: " + categoriesId);
        }
        return categories;
    }


    private Unit findUnitById(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Единица измерения с ID %s не найдена".formatted(unitId)));
    }



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
                .minStock(request.minStock())
                .maxStock(request.maxStock())
                .reorderPoint(request.reorderPoint())
                .eoq(request.eoq())
                .supplierId(request.supplierId())
                .active(true)

                .categories(new HashSet<>(categories))
                .unit(unit)
                .build();

        Product savedProduct = productRepository.save(newProduct);
        
        if (request.locationId() != null && request.initialQuantity() != null) {
            BigDecimal initialQty = request.initialQuantity();
            if (initialQty.compareTo(BigDecimal.ZERO) > 0) {
                HierarchyLevel location = hierarchyLevelRepository.findById(request.locationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Локация не найдена"));
                
                ProductLocationStock stock = stockRepository.findByLocationIdAndProductId(location.getId(), savedProduct.getId())
                        .orElseGet(() -> {
                            ProductLocationStock newStock = ProductLocationStock.builder()
                                    .location(location)
                                    .product(savedProduct)
                                    .quantity(BigDecimal.ZERO)
                                    .reserved(BigDecimal.ZERO)
                                    .build();
                            newStock.setId(new ProductLocationStockId(location.getId(), savedProduct.getId()));
                            return newStock;
                        });
                
                stock.setQuantity(stock.getQuantity().add(initialQty));
                stockRepository.save(stock);
            }
        }
        
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
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
        existingProduct.setMaxStock(request.maxStock());
        existingProduct.setReorderPoint(request.reorderPoint());
        existingProduct.setEoq(request.eoq());
        existingProduct.setSupplierId(request.supplierId());

        existingProduct.setCategories(new HashSet<>(categories));
        existingProduct.setUnit(unit);

        Product updatedProduct = productRepository.save(existingProduct);
        return mapToResponse(updatedProduct);
    }


    public ProductResponse findProductById(Long id) {
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
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Товар с ID %s не найден".formatted(id));
        }
        productRepository.deleteById(id);
    }
}