package com.veterinaria.mascota.adapter.out.feign;

import com.veterinaria.mascota.domain.exception.ServicioNoDisponibleException;
import com.veterinaria.mascota.domain.port.out.DuenioServicePort;
import feign.FeignException;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica el cableado REAL de Resilience4j en el DuenioFeignAdapter (AOP + instancia "duenioService" + fallback).
 * Contexto minimo: solo el adaptador, AOP y Resilience4j; el cliente Feign esta simulado.
 */
@SpringBootTest(classes = ResilienciaDuenioFeignAdapterTest.Config.class, properties = {
        "spring.cloud.config.enabled=false",
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=4",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=1m",
        "resilience4j.retry.configs.default.max-attempts=3",
        "resilience4j.retry.configs.default.wait-duration=10ms",
        "resilience4j.retry.configs.default.ignore-exceptions[0]=io.github.resilience4j.circuitbreaker.CallNotPermittedException",
        "resilience4j.circuitbreaker.instances.duenioService.base-config=default",
        "resilience4j.retry.instances.duenioService.base-config=default"
})
class ResilienciaDuenioFeignAdapterTest {

    @Configuration
    @EnableAspectJAutoProxy
    @ImportAutoConfiguration({CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class})
    @Import(DuenioFeignAdapter.class)
    static class Config {
    }

    @MockitoBean
    private DuenioFeignClient client;

    @Autowired
    private DuenioServicePort adapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void cerrarCircuito() {
        circuitBreakers.circuitBreaker("duenioService").reset();
    }

    @Test
    void servicioCaido_reintenta3VecesYDespuesLanzaServicioNoDisponible() {
        when(client.findById(1L)).thenThrow(serviceUnavailable());

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
                () -> adapter.existsById(1L));

        assertThat(ex.getMessage()).startsWith("El servicio de duenios no esta disponible");
        verify(client, times(3)).findById(1L);
    }

    @Test
    void respuesta404_noEsUnFallo_niSeReintenta() {
        when(client.findById(99L)).thenThrow(
                new FeignException.NotFound("404 Not Found", request(), null, Map.of()));

        assertThat(adapter.existsById(99L)).isFalse();
        verify(client, times(1)).findById(99L);
    }

    @Test
    void fallosRepetidos_abrenElCircuitoYLuegoRespondeSinLlamarAlServicio() {
        when(client.findById(anyLong())).thenThrow(serviceUnavailable());
        assertThrows(ServicioNoDisponibleException.class, () -> adapter.existsById(1L));
        assertThrows(ServicioNoDisponibleException.class, () -> adapter.existsById(2L));
        assertThat(circuitBreakers.circuitBreaker("duenioService").getState()).isEqualTo(CircuitBreaker.State.OPEN);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
                () -> adapter.existsById(3L));

        assertThat(ex.getMessage()).contains("circuito abierto");
        verify(client, never()).findById(3L);
    }

    private static FeignException.ServiceUnavailable serviceUnavailable() {
        return new FeignException.ServiceUnavailable("503 Service Unavailable", request(), null, Map.of());
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "http://ms-duenio/api/duenios/1",
                Map.of(), null, StandardCharsets.UTF_8, null);
    }
}
