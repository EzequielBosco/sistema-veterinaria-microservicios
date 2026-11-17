package com.veterinaria.turno.adapter.out.feign;

import com.veterinaria.turno.domain.port.out.VeterinarioServicePort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA: implementa el puerto VeterinarioServicePort usando el cliente Feign.
 * Convierte el 404 de ms-veterinario en false; cualquier otro fallo Feign se propaga (-> 503).
 */
@Component
@RequiredArgsConstructor
public class VeterinarioFeignAdapter implements VeterinarioServicePort {

    private final VeterinarioFeignClient client;

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
