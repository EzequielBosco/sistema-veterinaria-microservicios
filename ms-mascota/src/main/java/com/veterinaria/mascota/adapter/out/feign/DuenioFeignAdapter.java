package com.veterinaria.mascota.adapter.out.feign;

import com.veterinaria.mascota.domain.exception.ServicioNoDisponibleException;
import com.veterinaria.mascota.domain.port.out.DuenioServicePort;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA: implementa el puerto DuenioServicePort usando el cliente Feign.
 * Traduce el HTTP 404 de ms-duenio en un booleano: el dominio no sabe nada de HTTP.
 * Resiliencia: Retry( CircuitBreaker( llamada ) ) con instancias "duenioService" de ms-mascota.yml.
 * El fallback va solo en @Retry y convierte cualquier fallo restante en ServicioNoDisponibleException (503).
 */
@Component
@RequiredArgsConstructor
public class DuenioFeignAdapter implements DuenioServicePort {

    private final DuenioFeignClient client;

    @Override
    @Retry(name = "duenioService", fallbackMethod = "fallbackDuenio")
    @CircuitBreaker(name = "duenioService")
    public boolean existsById(Long id) {
        try {
            client.findById(id);
            return true;
        } catch (FeignException.NotFound e) {
            return false;
        }
    }

    @SuppressWarnings("unused") // Lo invoca Resilience4j por reflexión (fallbackMethod = "fallbackDuenio")
    private boolean fallbackDuenio(Long id, Throwable ex) {
        String motivo = ex instanceof CallNotPermittedException
                ? "circuito abierto, se reintentara en unos segundos"
                : ex.getMessage();
        throw new ServicioNoDisponibleException("El servicio de duenios no esta disponible: " + motivo);
    }
}
