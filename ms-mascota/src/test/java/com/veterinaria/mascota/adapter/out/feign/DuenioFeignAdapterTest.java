package com.veterinaria.mascota.adapter.out.feign;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * El adaptador traduce la respuesta HTTP de ms-duenio al lenguaje del dominio:
 * 200 -> existe, 404 -> no existe, cualquier otro fallo -> se propaga (el handler responde 503).
 */
@ExtendWith(MockitoExtension.class)
class DuenioFeignAdapterTest {

    @Mock
    private DuenioFeignClient client;

    @InjectMocks
    private DuenioFeignAdapter adapter;

    @Test
    void cuandoElServicioDevuelveElDuenio_existe() {
        when(client.findById(1L)).thenReturn(new DuenioResponse());

        assertThat(adapter.existsById(1L)).isTrue();
    }

    @Test
    void cuandoElServicioResponde404_noExiste() {
        when(client.findById(99L)).thenThrow(
                new FeignException.NotFound("404 Not Found", request(99L), null, Map.of()));

        assertThat(adapter.existsById(99L)).isFalse();
    }

    @Test
    void cuandoElServicioNoEstaDisponible_propagaLaFeignException() {
        when(client.findById(1L)).thenThrow(
                new FeignException.ServiceUnavailable("503 Service Unavailable", request(1L), null, Map.of()));

        assertThrows(FeignException.ServiceUnavailable.class, () -> adapter.existsById(1L));
    }

    private static Request request(Long id) {
        return Request.create(Request.HttpMethod.GET, "http://ms-duenio/api/duenios/" + id,
                Map.of(), null, StandardCharsets.UTF_8, null);
    }
}
