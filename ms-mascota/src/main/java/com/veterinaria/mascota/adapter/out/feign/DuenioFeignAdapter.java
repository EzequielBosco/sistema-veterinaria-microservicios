package com.veterinaria.mascota.adapter.out.feign;

import com.veterinaria.mascota.domain.port.out.DuenioServicePort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA: implementa el puerto DuenioServicePort usando el cliente Feign.
 * Traduce el HTTP 404 de ms-duenio en un booleano: el dominio no sabe nada de HTTP.
 * Cualquier otro fallo (servicio caido, timeout, 5xx) se propaga como FeignException
 * y el GlobalExceptionHandler lo convierte en 503.
 */
@Component
@RequiredArgsConstructor
public class DuenioFeignAdapter implements DuenioServicePort {

    private final DuenioFeignClient client;

    @Override
    public boolean existsById(Long id) {
        try {
            client.findById(id);
            return true;
        } catch (FeignException.NotFound e) {
            return false;
        }
    }
}
