package com.veterinaria.turno.config;

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
 *
 * Por que existe: resilience4j-spring-boot3 trae su propio indicador, pero lo registra solo si encuentra
 * org.springframework.boot.actuate.health.HealthIndicator (Spring Boot 3). En Spring Boot 4 esa clase se movio a
 * org.springframework.boot.health.contributor, asi que el indicador de la libreria no se registra (sin error)
 * y register-health-indicator: true no tiene efecto. Este indicador usa la API de Boot 4.
 *
 * El estado general queda UP aunque un circuito este OPEN: ms-turno funciona, el caido es el otro servicio.
 * Marcarlo DOWN haria que Eureka o el gateway lo consideren no disponible y propagaria la falla en cascada.
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
