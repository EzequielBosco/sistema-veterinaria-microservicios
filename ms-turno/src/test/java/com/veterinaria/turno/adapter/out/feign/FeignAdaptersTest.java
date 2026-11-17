package com.veterinaria.turno.adapter.out.feign;

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
 * Los adaptadores traducen la respuesta HTTP de otro microservicio al lenguaje del dominio:
 * 200 -> existe, 404 -> no existe, cualquier otro fallo -> se propaga (el handler responde 503).
 */
@ExtendWith(MockitoExtension.class)
class FeignAdaptersTest {

    @Mock
    private MascotaFeignClient mascotaClient;

    @Mock
    private VeterinarioFeignClient veterinarioClient;

    @InjectMocks
    private MascotaFeignAdapter mascotaAdapter;

    @InjectMocks
    private VeterinarioFeignAdapter veterinarioAdapter;

    @Test
    void mascota_cuandoElServicioLaDevuelve_existe() {
        when(mascotaClient.findById(1L)).thenReturn(new MascotaResponse());

        assertThat(mascotaAdapter.existsById(1L)).isTrue();
    }

    @Test
    void mascota_cuandoElServicioResponde404_noExiste() {
        when(mascotaClient.findById(99L)).thenThrow(notFound("http://ms-mascota/api/mascotas/99"));

        assertThat(mascotaAdapter.existsById(99L)).isFalse();
    }

    @Test
    void mascota_cuandoElServicioNoEstaDisponible_propagaLaFeignException() {
        when(mascotaClient.findById(1L)).thenThrow(serviceUnavailable("http://ms-mascota/api/mascotas/1"));

        assertThrows(FeignException.ServiceUnavailable.class, () -> mascotaAdapter.existsById(1L));
    }

    @Test
    void veterinario_cuandoElServicioLoDevuelve_existe() {
        when(veterinarioClient.findById(2L)).thenReturn(new VeterinarioResponse());

        assertThat(veterinarioAdapter.existsById(2L)).isTrue();
    }

    @Test
    void veterinario_cuandoElServicioResponde404_noExiste() {
        when(veterinarioClient.findById(99L)).thenThrow(notFound("http://ms-veterinario/api/veterinarios/99"));

        assertThat(veterinarioAdapter.existsById(99L)).isFalse();
    }

    @Test
    void veterinario_cuandoElServicioNoEstaDisponible_propagaLaFeignException() {
        when(veterinarioClient.findById(2L)).thenThrow(serviceUnavailable("http://ms-veterinario/api/veterinarios/2"));

        assertThrows(FeignException.ServiceUnavailable.class, () -> veterinarioAdapter.existsById(2L));
    }

    private static FeignException.NotFound notFound(String url) {
        return new FeignException.NotFound("404 Not Found", request(url), null, Map.of());
    }

    private static FeignException.ServiceUnavailable serviceUnavailable(String url) {
        return new FeignException.ServiceUnavailable("503 Service Unavailable", request(url), null, Map.of());
    }

    private static Request request(String url) {
        return Request.create(Request.HttpMethod.GET, url, Map.of(), null, StandardCharsets.UTF_8, null);
    }
}
