package com.veterinaria.turno.adapter.out.feign;

import com.veterinaria.turno.domain.exception.ServicioNoDisponibleException;
import com.veterinaria.turno.domain.port.out.MascotaServicePort;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA: implementa el puerto MascotaServicePort usando el cliente Feign.
 * Traduce el HTTP 404 de ms-mascota en un booleano: el dominio no sabe nada de HTTP.
 *
 * Resiliencia (Resilience4j). Las anotaciones se anidan: Retry( CircuitBreaker( llamada ) ).
 * - @CircuitBreaker cuenta cada intento: si fallan demasiados, el circuito se abre y las llamadas
 *   siguientes se rechazan al instante con CallNotPermittedException, sin tocar ms-mascota.
 * - @Retry reintenta los fallos transitorios con espera exponencial (1s, 2s). No reintenta si el
 *   circuito esta abierto (ignore-exceptions en ms-turno.yml), asi la respuesta es inmediata.
 * - El fallback va solo en @Retry (la capa exterior): se ejecuta una vez, cuando ya no hay mas intentos.
 * El 404 NO es un fallo: se resuelve dentro del metodo y no abre el circuito.
 */
@Component
@RequiredArgsConstructor
public class MascotaFeignAdapter implements MascotaServicePort {

    private final MascotaFeignClient client;

    @Override
    @Retry(name = "mascotaService", fallbackMethod = "fallbackMascota")
    @CircuitBreaker(name = "mascotaService")
    public boolean existsById(Long id) {
        try {
            client.findById(id);
            return true;
        } catch (FeignException.NotFound e) {
            return false;
        }
    }

    /** Mismo retorno y parametros que existsById + Throwable al final; el nombre coincide con fallbackMethod. */
    @SuppressWarnings("unused") // Lo invoca Resilience4j por reflexión (fallbackMethod = "fallbackMascota")
    private boolean fallbackMascota(Long id, Throwable ex) {
        String motivo = ex instanceof CallNotPermittedException
                ? "circuito abierto, se reintentara en unos segundos"
                : ex.getMessage();
        throw new ServicioNoDisponibleException("El servicio de mascotas no esta disponible: " + motivo);
    }
}
