package com.example.retail.controller;

import com.example.retail.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller to expose product endpoints.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // External fetch (from WireMock)
    @GetMapping("/fetch")
    public String fetchProductFromExternal(@RequestParam Long productId) {
        logger.info("Fetching product from external API for ID: {}", productId);
        return productService.fetchProductFromExternal(productId);
    }

    // DB fetch only
    @GetMapping("/db/{id}")
    public String fetchProductFromDb(@PathVariable Long id) {
        logger.info("Fetching product from DB for ID: {}", id);
        return productService.fetchProductFromDb(id);
    }

    // Combined DB + external
    @GetMapping("/combined/{id}")
    public String fetchCombinedProduct(@PathVariable Long id) {
        logger.info("Fetching combined product info for ID: {}", id);
        return productService.fetchCombinedProductInfo(id);
    }
}
