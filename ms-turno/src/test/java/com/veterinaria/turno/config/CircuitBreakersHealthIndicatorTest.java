package com.veterinaria.turno.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakersHealthIndicatorTest {

    @Test
    void informaElEstadoDeCadaCircuito_ySigueUpAunqueUnoEsteAbierto() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("mascotaService").transitionToOpenState();
        registry.circuitBreaker("veterinarioService");

        Health health = new CircuitBreakersHealthIndicator(registry).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(detalle(health, "mascotaService")).containsEntry("state", "OPEN");
        assertThat(detalle(health, "veterinarioService")).containsEntry("state", "CLOSED");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> detalle(Health health, String circuito) {
        return (Map<String, Object>) health.getDetails().get(circuito);
    }
}
