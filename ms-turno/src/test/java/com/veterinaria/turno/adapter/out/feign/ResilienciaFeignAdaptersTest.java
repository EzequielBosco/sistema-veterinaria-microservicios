package com.veterinaria.turno.adapter.out.feign;

import com.veterinaria.turno.domain.exception.ServicioNoDisponibleException;
import com.veterinaria.turno.domain.port.out.MascotaServicePort;
import com.veterinaria.turno.domain.port.out.VeterinarioServicePort;
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
 * Verifica el cableado REAL de Resilience4j sobre los adaptadores Feign (AOP + nombres de instancia + fallback).
 * Son los errores silenciosos del Sprint 13: sin AOP, con un name distinto o con una firma de fallback incorrecta,
 * el codigo compila pero la resiliencia no funciona. Un test unitario comun no los detecta.
 * Contexto minimo: solo los adaptadores, AOP y Resilience4j; el cliente Feign esta simulado.
 */
@SpringBootTest(classes = ResilienciaFeignAdaptersTest.Config.class, properties = {
        "spring.cloud.config.enabled=false",
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=4",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=1m",
        "resilience4j.retry.configs.default.max-attempts=3",
        "resilience4j.retry.configs.default.wait-duration=10ms",
        "resilience4j.retry.configs.default.ignore-exceptions[0]=io.github.resilience4j.circuitbreaker.CallNotPermittedException",
        "resilience4j.circuitbreaker.instances.mascotaService.base-config=default",
        "resilience4j.circuitbreaker.instances.veterinarioService.base-config=default",
        "resilience4j.retry.instances.mascotaService.base-config=default",
        "resilience4j.retry.instances.veterinarioService.base-config=default"
})
class ResilienciaFeignAdaptersTest {

    @Configuration
    @EnableAspectJAutoProxy
    @ImportAutoConfiguration({CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class})
    @Import({MascotaFeignAdapter.class, VeterinarioFeignAdapter.class})
    static class Config {
    }

    @MockitoBean
    private MascotaFeignClient mascotaClient;

    @MockitoBean
    private VeterinarioFeignClient veterinarioClient;

    @Autowired
    private MascotaServicePort mascotaAdapter;

    @Autowired
    private VeterinarioServicePort veterinarioAdapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void cerrarCircuitos() {
        circuitBreakers.circuitBreaker("mascotaService").reset();
        circuitBreakers.circuitBreaker("veterinarioService").reset();
    }

    @Test
    void servicioCaido_reintenta3VecesYDespuesLanzaServicioNoDisponible() {
        when(mascotaClient.findById(1L)).thenThrow(serviceUnavailable());

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
                () -> mascotaAdapter.existsById(1L));

        assertThat(ex.getMessage()).startsWith("El servicio de mascotas no esta disponible");
        verify(mascotaClient, times(3)).findById(1L);
    }

    @Test
    void respuesta404_noEsUnFallo_niSeReintentaNiCuentaParaElCircuito() {
        when(mascotaClient.findById(99L)).thenThrow(notFound());

        assertThat(mascotaAdapter.existsById(99L)).isFalse();

        verify(mascotaClient, times(1)).findById(99L);
        assertThat(circuitBreakers.circuitBreaker("mascotaService").getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void fallosRepetidos_abrenElCircuito() {
        when(mascotaClient.findById(anyLong())).thenThrow(serviceUnavailable());

        // 2 requests x 3 intentos = 6 llamadas fallidas: supera minimum-number-of-calls (4) con 100% de fallos
        assertThrows(ServicioNoDisponibleException.class, () -> mascotaAdapter.existsById(1L));
        assertThrows(ServicioNoDisponibleException.class, () -> mascotaAdapter.existsById(2L));

        assertThat(circuitBreakers.circuitBreaker("mascotaService").getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitoAbierto_respondeAlInstanteSinLlamarAlServicioNiReintentar() {
        circuitBreakers.circuitBreaker("mascotaService").transitionToOpenState();

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
                () -> mascotaAdapter.existsById(1L));

        assertThat(ex.getMessage()).contains("circuito abierto");
        verify(mascotaClient, never()).findById(anyLong());
    }

    @Test
    void veterinario_usaSuPropiaInstancia() {
        circuitBreakers.circuitBreaker("veterinarioService").transitionToOpenState();
        when(mascotaClient.findById(1L)).thenReturn(new MascotaResponse());

        // El circuito de veterinarios abierto no afecta a mascotas
        assertThat(mascotaAdapter.existsById(1L)).isTrue();
        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
                () -> veterinarioAdapter.existsById(2L));
        assertThat(ex.getMessage()).startsWith("El servicio de veterinarios no esta disponible");
        verify(veterinarioClient, never()).findById(anyLong());
    }

    private static FeignException.NotFound notFound() {
        return new FeignException.NotFound("404 Not Found", request(), null, Map.of());
    }

    private static FeignException.ServiceUnavailable serviceUnavailable() {
        return new FeignException.ServiceUnavailable("503 Service Unavailable", request(), null, Map.of());
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "http://ms-mascota/api/mascotas/1",
                Map.of(), null, StandardCharsets.UTF_8, null);
    }
}
