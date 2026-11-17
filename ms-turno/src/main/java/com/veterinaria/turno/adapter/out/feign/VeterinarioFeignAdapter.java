package com.veterinaria.turno.adapter.out.feign;

import com.veterinaria.turno.domain.exception.ServicioNoDisponibleException;
import com.veterinaria.turno.domain.port.out.VeterinarioServicePort;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA: implementa VeterinarioServicePort usando el cliente Feign.
 * Convierte el 404 de ms-veterinario en false. Misma resiliencia que MascotaFeignAdapter:
 * Retry( CircuitBreaker( llamada ) ) con instancias "veterinarioService" definidas en ms-turno.yml.
 */
@Component
@RequiredArgsConstructor
public class VeterinarioFeignAdapter implements VeterinarioServicePort {

    private final VeterinarioFeignClient client;

    @Override
    @Retry(name = "veterinarioService", fallbackMethod = "fallbackVeterinario")
    @CircuitBreaker(name = "veterinarioService")
    public boolean existsById(Long id) {
        try {
            client.findById(id);
            return true;
        } catch (FeignException.NotFound e) {
            return false;
        }
    }

    @SuppressWarnings("unused") // Lo invoca Resilience4j por reflexión (fallbackMethod = "fallbackVeterinario")
    private boolean fallbackVeterinario(Long id, Throwable ex) {
        String motivo = ex instanceof CallNotPermittedException
                ? "circuito abierto, se reintentara en unos segundos"
                : ex.getMessage();
        throw new ServicioNoDisponibleException("El servicio de veterinarios no esta disponible: " + motivo);
    }
}
