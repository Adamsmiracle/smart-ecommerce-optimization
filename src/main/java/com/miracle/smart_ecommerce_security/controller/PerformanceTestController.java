package com.miracle.smart_ecommerce_security.controller;

import com.miracle.smart_ecommerce_security.domain.product.entity.Product;
import com.miracle.smart_ecommerce_security.domain.product.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/perf-test")
public class PerformanceTestController {

    private final ProductRepository productRepository;

    public PerformanceTestController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Bottleneck 1: simulates slow I/O (e.g. external service call, heavy DB query)
    @GetMapping("/slow-io")
    public String slowIO() throws InterruptedException {
        Thread.sleep(900);
        return "slow-io completed";
    }

    // Bottleneck 2: CPU-intensive work (simulates unoptimised computation)
    @GetMapping("/cpu-heavy")
    public long cpuHeavy() {
        long result = 0;
        for (long i = 0; i < 80_000_000L; i++) result += i;
        return result;
    }

    // Bottleneck 3: fetches all products with no pagination (real N+1 risk)
    @GetMapping("/all-products")
    public List<Product> allProducts() {
        return productRepository.findAll();
    }

    // Bottleneck 4: simulates slow order processing pipeline
    @GetMapping("/slow-order")
    public String slowOrder() throws InterruptedException {
        Thread.sleep(1200);
        return "order processed";
    }
}
