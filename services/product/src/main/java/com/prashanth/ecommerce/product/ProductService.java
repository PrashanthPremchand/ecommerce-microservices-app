package com.prashanth.ecommerce.product;

import com.prashanth.ecommerce.exception.ProductPurchaseException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @CircuitBreaker(name = "productService", fallbackMethod = "createProductFallback")
    public Integer createProduct(@Valid ProductRequest request) {
        var product = productMapper.toProduct(request);
        return productRepository.save(product).getId();
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "purchaseProductsFallback")
    public List<ProductPurchaseResponse> purchaseProducts(List<ProductPurchaseRequest> request) {
        var productIds = request.stream()
                .map(ProductPurchaseRequest::productId)
                .toList();
        var storedProducts = productRepository.findAllByIdInOrderById(productIds);
        if(productIds.size() != storedProducts.size()) {
            throw new ProductPurchaseException("One or more products does not exists");
        }
        var storedRequest = request.stream()
                .sorted(Comparator.comparing(ProductPurchaseRequest::productId))
                .toList();
        var purchasedProducts = new ArrayList<ProductPurchaseResponse>();
        for(int i = 0; i < storedProducts.size(); i++) {
            var product = storedProducts.get(i);
            var productRequest = storedRequest.get(i);
            if(product.getAvailableQuantity() < productRequest.quantity()) {
                throw new ProductPurchaseException("Product " + productIds.get(i) + " is out of stock");
            }
            var updatedAvailableQuantity = product.getAvailableQuantity() - productRequest.quantity();
            product.setAvailableQuantity(updatedAvailableQuantity);
            productRepository.save(product);
            purchasedProducts.add(productMapper.toProductPurchaseResponse(product, productRequest.quantity()));
        }
        return purchasedProducts;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "findByIdFallback")
    public ProductResponse findById(Integer productId) {
        return productRepository.findById(productId)
                .map(productMapper::toProductResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "findAllFallback")
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    public Integer createProductFallback(ProductRequest request, Exception ex) {
        throw new ProductPurchaseException("Product service is currently unavailable. Please try again later.");
    }

    public List<ProductPurchaseResponse> purchaseProductsFallback(List<ProductPurchaseRequest> request, Exception ex) {
        throw new ProductPurchaseException("Purchase service is temporarily unavailable. Please try again later.");
    }

    public ProductResponse findByIdFallback(Integer productId, Exception ex) {
        throw new EntityNotFoundException("Product service is temporarily unavailable. Please try again later.");
    }

    public List<ProductResponse> findAllFallback(Exception ex) {
        return new ArrayList<>();  // Return empty list or cached data
    }
}