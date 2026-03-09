package org.com.pravin.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.productservice.dto.ProductRequest;
import org.com.pravin.productservice.dto.ProductResponse;
import org.com.pravin.productservice.entity.Product;
import org.com.pravin.productservice.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;

    // On first call → hits DB, stores in Redis
    // On subsequent calls → returns from Redis (no DB hit)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProduct(Long id) {
        log.info("Fetching product {} from DATABASE", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return ProductResponse.from(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .build();
        productRepository.save(product);
        log.info("Created product: {}", product.getId());
        return ProductResponse.from(product);
    }

    // When product updates → evict from cache so next read gets fresh data
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product {} — evicting from cache", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        productRepository.save(product);

        return ProductResponse.from(product);
    }

    // Evict all products cache
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        log.info("Deleted product {} — evicted all cache", id);
    }
}
