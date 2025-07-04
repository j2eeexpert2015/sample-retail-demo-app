package com.example.retail.service;

import com.example.retail.client.ProductClient;
import com.example.retail.entity.Product;
import com.example.retail.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to fetch product info from DB and external API.
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductClient productClient;

    public ProductService(ProductRepository productRepository, ProductClient productClient) {
        this.productRepository = productRepository;
        this.productClient = productClient;
    }

    // Fetch product only from DB
    public String fetchProductFromDb(Long id) {
        logger.debug("Fetching product from DB with ID: {}", id);
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return "Product with ID " + id + " not found in DB";
        }
        return "[DB] " + product.getName() + " - ₹" + product.getPrice();
    }

    // Fetch product only from external WireMock API
    public String fetchProductFromExternal(Long id) {
        return productClient.fetchProductById(id);
    }

    // Combine DB and external product info
    public String fetchCombinedProductInfo(Long id) {
        logger.debug("Combining product info from DB and external system");

        Product dbProduct = productRepository.findById(id).orElse(null);
        String dbInfo = (dbProduct != null)
                ? "[DB] " + dbProduct.getName() + " - ₹" + dbProduct.getPrice()
                : "[DB] Product not found";

        String externalInfo = productClient.fetchProductById(id);
        return dbInfo + "\n[External] " + externalInfo;
    }
}
