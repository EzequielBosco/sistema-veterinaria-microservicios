package com.veterinaria.mascota.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Muestra el estado de cada circuit breaker (CLOSED / OPEN / HALF_OPEN) en /actuator/health, bajo "circuitBreakers".
 * Reemplaza al indicador de resilience4j-spring-boot3, que no se registra con Spring Boot 4 (busca la clase de
 * HealthIndicator de Boot 3). El estado general queda UP aunque un circuito este OPEN: el caido es ms-duenio.
 */
@Component("circuitBreakers")
@RequiredArgsConstructor
public class CircuitBreakersHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry registry;

    @Override
    public Health health() {
        Map<String, Object> circuitos = new TreeMap<>();
        for (CircuitBreaker circuito : registry.getAllCircuitBreakers()) {
            CircuitBreaker.Metrics metricas = circuito.getMetrics();
            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("state", circuito.getState().name());
            detalle.put("failureRate", metricas.getFailureRate() + "%");
            detalle.put("bufferedCalls", metricas.getNumberOfBufferedCalls());
            detalle.put("failedCalls", metricas.getNumberOfFailedCalls());
            detalle.put("notPermittedCalls", metricas.getNumberOfNotPermittedCalls());
            circuitos.put(circuito.getName(), detalle);
        }
        return Health.up().withDetails(circuitos).build();
    }
}
