package com.miracle.smart_ecommerce_security.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin/metrics")
@Tag(name = "System Metrics", description = "Endpoints for retrieving system performance metrics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemMetricsController {

    private final MeterRegistry meterRegistry;

    @GetMapping
    @Operation(summary = "Get System Metrics", description = "Returns request latency, throughput, and memory usage statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Memory Usage
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        Map<String, Object> memoryMetrics = new HashMap<>();
        memoryMetrics.put("used_mb", heapMemoryUsage.getUsed() / (1024 * 1024));
        memoryMetrics.put("max_mb", heapMemoryUsage.getMax() / (1024 * 1024));
        metrics.put("memory", memoryMetrics);

        // HTTP Requests latency and throughput
        Timer httpTimer = meterRegistry.find("http.server.requests").timer();
        Map<String, Object> httpMetrics = new HashMap<>();
        if (httpTimer != null) {
            httpMetrics.put("total_requests", httpTimer.count());
            httpMetrics.put("max_latency_ms", httpTimer.max(TimeUnit.MILLISECONDS));
            httpMetrics.put("mean_latency_ms", httpTimer.mean(TimeUnit.MILLISECONDS));
            // rough throughput approximation since startup via micrometer count
            httpMetrics.put("total_time_s", httpTimer.totalTime(TimeUnit.SECONDS));
        } else {
            httpMetrics.put("message", "No HTTP request data available yet");
        }
        metrics.put("http", httpMetrics);

        // Custom application-level metrics can be added depending on existing Micrometer instrumentation

        return ResponseEntity.ok(ApiResponse.success(metrics, "System metrics retrieved successfully"));
    }
}

