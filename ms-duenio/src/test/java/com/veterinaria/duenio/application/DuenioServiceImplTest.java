package com.veterinaria.duenio.application;

import com.veterinaria.duenio.domain.exception.DuenioNotFoundException;
import com.veterinaria.duenio.domain.exception.DuplicateCedulaException;
import com.veterinaria.duenio.domain.exception.DuplicateEmailException;
import com.veterinaria.duenio.domain.model.Duenio;
import com.veterinaria.duenio.domain.port.out.DuenioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuenioServiceImplTest {

    @Mock
    private DuenioRepositoryPort duenioRepository;

    @InjectMocks
    private DuenioServiceImpl duenioService;

    @Test
    void listarTodos_cuandoNoHayDatos_retornaListaVacia() {
        when(duenioRepository.buscarTodos()).thenReturn(List.of());

        assertThat(duenioService.listarTodos()).isEmpty();
    }

    @Test
    void listarTodos_cuandoHayDatos_retornaListaConDuenios() {
        when(duenioRepository.buscarTodos()).thenReturn(List.of(crearDuenio()));

        List<Duenio> resultado = duenioService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getNombre()).isEqualTo("Ana");
    }

    @Test
    void buscarPorId_cuandoExiste_retornaDuenio() {
        when(duenioRepository.buscarPorId(1L)).thenReturn(crearDuenio());

        assertThat(duenioService.buscarPorId(1L).getNombre()).isEqualTo("Ana");
    }

    @Test
    void buscarPorId_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(DuenioNotFoundException.class, () -> duenioService.buscarPorId(99L));
    }

    @Test
    void buscarPorCedula_cuandoExiste_retornaDuenio() {
        when(duenioRepository.buscarPorCedula("12345678")).thenReturn(Optional.of(crearDuenio()));

        assertThat(duenioService.buscarPorCedula("12345678").getCedula()).isEqualTo("12345678");
    }

    @Test
    void buscarPorCedula_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.buscarPorCedula("99999999")).thenReturn(Optional.empty());

        assertThrows(DuenioNotFoundException.class, () -> duenioService.buscarPorCedula("99999999"));
    }

    @Test
    void buscarPorEmail_cuandoExiste_retornaDuenio() {
        when(duenioRepository.buscarPorEmail("ana@mail.com")).thenReturn(Optional.of(crearDuenio()));

        assertThat(duenioService.buscarPorEmail("ana@mail.com").getEmail()).isEqualTo("ana@mail.com");
    }

    @Test
    void buscarPorEmail_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.buscarPorEmail("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(DuenioNotFoundException.class, () -> duenioService.buscarPorEmail("nadie@mail.com"));
    }

    @Test
    void buscarPorNombreYApellido_retornaCoincidencias() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.buscarPorNombreYApellido("Ana", "Gomez")).thenReturn(List.of(duenio));

        assertThat(duenioService.buscarPorNombreYApellido("Ana", "Gomez")).containsExactly(duenio);
    }

    @Test
    void registrar_cuandoEsValido_guardaDuenio() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.existePorCedula("12345678")).thenReturn(false);
        when(duenioRepository.existePorEmail("ana@mail.com")).thenReturn(false);
        when(duenioRepository.guardar(duenio)).thenReturn(duenio);

        assertThat(duenioService.registrar(duenio).getCedula()).isEqualTo("12345678");
        verify(duenioRepository).guardar(duenio);
    }

    @Test
    void registrar_cuandoCedulaDuplicada_lanzaDuplicateCedulaExceptionYNoGuarda() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.existePorCedula("12345678")).thenReturn(true);

        assertThrows(DuplicateCedulaException.class, () -> duenioService.registrar(duenio));
        verify(duenioRepository, never()).guardar(any(Duenio.class));
    }

    @Test
    void registrar_cuandoEmailDuplicado_lanzaDuplicateEmailExceptionYNoGuarda() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.existePorCedula("12345678")).thenReturn(false);
        when(duenioRepository.existePorEmail("ana@mail.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> duenioService.registrar(duenio));
        verify(duenioRepository, never()).guardar(any(Duenio.class));
    }

    @Test
    void registrar_conEmailEnBlanco_normalizaEmailANull() {
        Duenio duenio = new Duenio(null, "Ana", "Gomez", "1122334455", "  ", "12345678");
        when(duenioRepository.existePorCedula("12345678")).thenReturn(false);
        when(duenioRepository.guardar(duenio)).thenReturn(duenio);

        assertThat(duenioService.registrar(duenio).getEmail()).isNull();
        verify(duenioRepository, never()).existePorEmail(anyString());
    }

    @Test
    void actualizar_cuandoExiste_actualizaYGuarda() {
        Duenio existente = crearDuenio();
        Duenio cambios = new Duenio(null, "Ana Maria", "Gomez", "1199999999", "ana2@mail.com", "12345678");
        when(duenioRepository.buscarPorId(1L)).thenReturn(existente);
        when(duenioRepository.existePorEmail("ana2@mail.com")).thenReturn(false);
        when(duenioRepository.guardar(existente)).thenReturn(existente);

        Duenio resultado = duenioService.actualizar(1L, cambios);

        assertThat(resultado.getNombre()).isEqualTo("Ana Maria");
        assertThat(resultado.getEmail()).isEqualTo("ana2@mail.com");
    }

    @Test
    void actualizar_cuandoNoExiste_lanzaDuenioNotFoundExceptionYNoGuarda() {
        when(duenioRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(DuenioNotFoundException.class, () -> duenioService.actualizar(99L, crearDuenio()));
        verify(duenioRepository, never()).guardar(any(Duenio.class));
    }

    @Test
    void actualizar_conCedulaDuplicada_lanzaDuplicateCedulaExceptionYNoGuarda() {
        Duenio cambios = new Duenio(null, "Ana", "Gomez", "1122334455", "ana@mail.com", "87654321");
        when(duenioRepository.buscarPorId(1L)).thenReturn(crearDuenio());
        when(duenioRepository.existePorCedula("87654321")).thenReturn(true);

        assertThrows(DuplicateCedulaException.class, () -> duenioService.actualizar(1L, cambios));
        verify(duenioRepository, never()).guardar(any(Duenio.class));
    }

    @Test
    void eliminarPorId_cuandoExiste_eliminaDuenio() {
        when(duenioRepository.buscarPorId(1L)).thenReturn(crearDuenio());

        duenioService.eliminarPorId(1L);

        verify(duenioRepository).eliminarPorId(1L);
    }

    @Test
    void eliminarPorId_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(DuenioNotFoundException.class, () -> duenioService.eliminarPorId(99L));
        verify(duenioRepository, never()).eliminarPorId(anyLong());
    }

    private Duenio crearDuenio() {
        return new Duenio(1L, "Ana", "Gomez", "1122334455", "ana@mail.com", "12345678");
    }
}
