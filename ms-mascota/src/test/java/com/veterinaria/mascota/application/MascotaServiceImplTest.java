package com.veterinaria.mascota.application;

import com.veterinaria.mascota.domain.exception.BadRequestException;
import com.veterinaria.mascota.domain.exception.CupoMascotasExcedidoException;
import com.veterinaria.mascota.domain.exception.DuenioNotFoundException;
import com.veterinaria.mascota.domain.exception.MascotaNotFoundException;
import com.veterinaria.mascota.domain.model.Mascota;
import com.veterinaria.mascota.domain.model.SexoMascota;
import com.veterinaria.mascota.domain.port.out.DuenioServicePort;
import com.veterinaria.mascota.domain.port.out.MascotaRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MascotaServiceImplTest {

    @Mock
    private MascotaRepositoryPort mascotaRepository;

    @Mock
    private DuenioServicePort duenioService;

    @InjectMocks
    private MascotaServiceImpl mascotaService;

    @Test
    void listarTodos_retornaMascotas() {
        when(mascotaRepository.buscarTodos()).thenReturn(List.of(crearMascota(1L)));

        assertThat(mascotaService.listarTodos()).hasSize(1);
    }

    @Test
    void buscarPorId_cuandoExiste_retornaMascota() {
        when(mascotaRepository.buscarPorId(1L)).thenReturn(crearMascota(1L));

        assertThat(mascotaService.buscarPorId(1L).getNombre()).isEqualTo("Firulais");
    }

    @Test
    void buscarPorId_cuandoNoExiste_lanzaMascotaNotFoundException() {
        when(mascotaRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(MascotaNotFoundException.class, () -> mascotaService.buscarPorId(99L));
    }

    @Test
    void buscarPorDuenioId_retornaMascotasDelDuenio() {
        when(mascotaRepository.buscarPorDuenioId(1L)).thenReturn(List.of(crearMascota(1L)));

        assertThat(mascotaService.buscarPorDuenioId(1L)).hasSize(1);
    }

    @Test
    void registrar_conCupoDisponibleYDuenioExistente_guardaMascota() {
        Mascota mascota = crearMascota(1L);
        when(mascotaRepository.contarPorDuenioId(1L)).thenReturn(4L);
        when(duenioService.existsById(1L)).thenReturn(true);
        when(mascotaRepository.guardar(mascota)).thenReturn(mascota);

        assertThat(mascotaService.registrar(mascota)).isEqualTo(mascota);
        verify(duenioService).existsById(1L);
    }

    @Test
    void registrar_conDuenioInexistente_lanzaDuenioNotFoundExceptionYNoGuarda() {
        Mascota mascota = crearMascota(77L);
        when(mascotaRepository.contarPorDuenioId(77L)).thenReturn(0L);
        when(duenioService.existsById(77L)).thenReturn(false);

        DuenioNotFoundException ex = assertThrows(DuenioNotFoundException.class,
                () -> mascotaService.registrar(mascota));
        assertThat(ex.getMessage()).contains("id 77");
        verify(mascotaRepository, never()).guardar(any(Mascota.class));
    }

    @Test
    void registrar_sinDuenioId_lanzaBadRequestExceptionYNoGuarda() {
        Mascota mascota = crearMascota(null);

        assertThrows(BadRequestException.class, () -> mascotaService.registrar(mascota));
        verify(mascotaRepository, never()).guardar(any(Mascota.class));
        verifyNoInteractions(duenioService);
    }

    @Test
    void registrar_conCupoCompleto_lanzaCupoMascotasExcedidoExceptionSinConsultarMsDuenio() {
        Mascota mascota = crearMascota(1L);
        when(mascotaRepository.contarPorDuenioId(1L)).thenReturn(5L);

        assertThrows(CupoMascotasExcedidoException.class, () -> mascotaService.registrar(mascota));
        verify(mascotaRepository, never()).guardar(any(Mascota.class));
        verifyNoInteractions(duenioService);
    }

    @Test
    void actualizar_mismoDuenio_noValidaCupoNiConsultaMsDuenio() {
        Mascota existente = crearMascota(1L);
        Mascota cambios = crearMascota(1L);
        cambios.setNombre("Rocky");
        when(mascotaRepository.buscarPorId(10L)).thenReturn(existente);
        when(mascotaRepository.guardar(existente)).thenReturn(existente);

        Mascota resultado = mascotaService.actualizar(10L, cambios);

        assertThat(resultado.getNombre()).isEqualTo("Rocky");
        verify(mascotaRepository, never()).contarPorDuenioId(anyLong());
        verifyNoInteractions(duenioService);
    }

    @Test
    void actualizar_cambioADuenioInexistente_lanzaDuenioNotFoundExceptionYNoReasigna() {
        Mascota existente = crearMascota(1L);
        when(mascotaRepository.buscarPorId(10L)).thenReturn(existente);
        when(mascotaRepository.contarPorDuenioId(77L)).thenReturn(0L);
        when(duenioService.existsById(77L)).thenReturn(false);

        assertThrows(DuenioNotFoundException.class, () -> mascotaService.actualizar(10L, crearMascota(77L)));
        assertThat(existente.getDuenioId()).isEqualTo(1L);
        verify(mascotaRepository, never()).guardar(any(Mascota.class));
    }

    @Test
    void actualizar_cambioDeDuenioConCupoCompleto_lanzaCupoMascotasExcedidoException() {
        when(mascotaRepository.buscarPorId(10L)).thenReturn(crearMascota(1L));
        when(mascotaRepository.contarPorDuenioId(2L)).thenReturn(5L);

        assertThrows(CupoMascotasExcedidoException.class, () -> mascotaService.actualizar(10L, crearMascota(2L)));
        verify(mascotaRepository, never()).guardar(any(Mascota.class));
    }

    @Test
    void actualizar_cambioDeDuenioConCupoDisponible_reasignaDuenio() {
        Mascota existente = crearMascota(1L);
        when(mascotaRepository.buscarPorId(10L)).thenReturn(existente);
        when(mascotaRepository.contarPorDuenioId(2L)).thenReturn(0L);
        when(duenioService.existsById(2L)).thenReturn(true);
        when(mascotaRepository.guardar(existente)).thenReturn(existente);

        assertThat(mascotaService.actualizar(10L, crearMascota(2L)).getDuenioId()).isEqualTo(2L);
    }

    @Test
    void actualizar_cuandoNoExiste_lanzaMascotaNotFoundException() {
        when(mascotaRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(MascotaNotFoundException.class, () -> mascotaService.actualizar(99L, crearMascota(1L)));
    }

    @Test
    void eliminarPorId_cuandoExiste_eliminaMascota() {
        when(mascotaRepository.buscarPorId(10L)).thenReturn(crearMascota(1L));

        mascotaService.eliminarPorId(10L);

        verify(mascotaRepository).eliminarPorId(10L);
    }

    @Test
    void eliminarPorId_cuandoNoExiste_lanzaMascotaNotFoundException() {
        when(mascotaRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(MascotaNotFoundException.class, () -> mascotaService.eliminarPorId(99L));
        verify(mascotaRepository, never()).eliminarPorId(anyLong());
    }

    private Mascota crearMascota(Long duenioId) {
        return new Mascota(null, "Firulais", "Perro", "Labrador", "Dorado",
                LocalDate.of(2020, 3, 10), SexoMascota.MACHO, duenioId);
    }
}
