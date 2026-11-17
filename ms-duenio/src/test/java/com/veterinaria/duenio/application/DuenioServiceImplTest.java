package com.veterinaria.duenio.application;

import com.veterinaria.duenio.domain.exception.DuenioNotFoundException;
import com.veterinaria.duenio.domain.exception.DuplicatedCedulaException;
import com.veterinaria.duenio.domain.exception.DuplicatedEmailException;
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
    void findAll_cuandoNoHayDatos_retornaListaVacia() {
        when(duenioRepository.findAll()).thenReturn(List.of());

        assertThat(duenioService.findAll()).isEmpty();
    }

    @Test
    void findAll_cuandoHayDatos_retornaListaConDuenios() {
        when(duenioRepository.findAll()).thenReturn(List.of(crearDuenio()));

        List<Duenio> resultado = duenioService.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getNombre()).isEqualTo("Ana");
    }

    @Test
    void findById_cuandoExiste_retornaDuenio() {
        when(duenioRepository.findById(1L)).thenReturn(Optional.of(crearDuenio()));

        assertThat(duenioService.findById(1L).getNombre()).isEqualTo("Ana");
    }

    @Test
    void findById_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DuenioNotFoundException.class, () -> duenioService.findById(99L));
    }

    @Test
    void findByCedula_cuandoExiste_retornaDuenio() {
        when(duenioRepository.findByCedula("12345678")).thenReturn(Optional.of(crearDuenio()));

        assertThat(duenioService.findByCedula("12345678").getCedula()).isEqualTo("12345678");
    }

    @Test
    void findByCedula_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.findByCedula("99999999")).thenReturn(Optional.empty());

        assertThrows(DuenioNotFoundException.class, () -> duenioService.findByCedula("99999999"));
    }

    @Test
    void findByEmail_cuandoExiste_retornaDuenio() {
        when(duenioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(crearDuenio()));

        assertThat(duenioService.findByEmail("ana@mail.com").getEmail()).isEqualTo("ana@mail.com");
    }

    @Test
    void findByEmail_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(DuenioNotFoundException.class, () -> duenioService.findByEmail("nadie@mail.com"));
    }

    @Test
    void search_retornaCoincidencias() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.findByNombreAndApellido("Ana", "Gomez")).thenReturn(List.of(duenio));

        assertThat(duenioService.search("Ana", "Gomez")).containsExactly(duenio);
    }

    @Test
    void save_cuandoEsValido_guardaDuenio() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.existsByCedula("12345678")).thenReturn(false);
        when(duenioRepository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(duenioRepository.save(duenio)).thenReturn(duenio);

        Duenio resultado = duenioService.save(duenio);

        assertThat(resultado.getCedula()).isEqualTo("12345678");
        verify(duenioRepository).save(duenio);
    }

    @Test
    void save_cuandoCedulaDuplicada_lanzaDuplicatedCedulaExceptionYNoGuarda() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.existsByCedula("12345678")).thenReturn(true);

        assertThrows(DuplicatedCedulaException.class, () -> duenioService.save(duenio));
        verify(duenioRepository, never()).save(any(Duenio.class));
    }

    @Test
    void save_cuandoEmailDuplicado_lanzaDuplicatedEmailExceptionYNoGuarda() {
        Duenio duenio = crearDuenio();
        when(duenioRepository.existsByCedula("12345678")).thenReturn(false);
        when(duenioRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThrows(DuplicatedEmailException.class, () -> duenioService.save(duenio));
        verify(duenioRepository, never()).save(any(Duenio.class));
    }

    @Test
    void save_conEmailEnBlanco_normalizaEmailANull() {
        Duenio duenio = new Duenio(null, "Ana", "Gomez", "1122334455", "  ", "12345678");
        when(duenioRepository.existsByCedula("12345678")).thenReturn(false);
        when(duenioRepository.save(duenio)).thenReturn(duenio);

        Duenio resultado = duenioService.save(duenio);

        assertThat(resultado.getEmail()).isNull();
        verify(duenioRepository, never()).existsByEmail(anyString());
    }

    @Test
    void update_cuandoExiste_actualizaYGuarda() {
        Duenio existente = crearDuenio();
        Duenio cambios = new Duenio(null, "Ana Maria", "Gomez", "1199999999", "ana2@mail.com", "12345678");
        when(duenioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(duenioRepository.existsByEmail("ana2@mail.com")).thenReturn(false);
        when(duenioRepository.save(existente)).thenReturn(existente);

        Duenio resultado = duenioService.update(1L, cambios);

        assertThat(resultado.getNombre()).isEqualTo("Ana Maria");
        assertThat(resultado.getEmail()).isEqualTo("ana2@mail.com");
        verify(duenioRepository).save(existente);
    }

    @Test
    void update_cuandoNoExiste_lanzaDuenioNotFoundExceptionYNoGuarda() {
        when(duenioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DuenioNotFoundException.class, () -> duenioService.update(99L, crearDuenio()));
        verify(duenioRepository, never()).save(any(Duenio.class));
    }

    @Test
    void update_conCedulaDuplicada_lanzaDuplicatedCedulaExceptionYNoGuarda() {
        Duenio cambios = new Duenio(null, "Ana", "Gomez", "1122334455", "ana@mail.com", "87654321");
        when(duenioRepository.findById(1L)).thenReturn(Optional.of(crearDuenio()));
        when(duenioRepository.existsByCedula("87654321")).thenReturn(true);

        assertThrows(DuplicatedCedulaException.class, () -> duenioService.update(1L, cambios));
        verify(duenioRepository, never()).save(any(Duenio.class));
    }

    @Test
    void deleteById_cuandoExiste_eliminaDuenio() {
        when(duenioRepository.findById(1L)).thenReturn(Optional.of(crearDuenio()));

        duenioService.deleteById(1L);

        verify(duenioRepository).deleteById(1L);
    }

    @Test
    void deleteById_cuandoNoExiste_lanzaDuenioNotFoundException() {
        when(duenioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DuenioNotFoundException.class, () -> duenioService.deleteById(99L));
        verify(duenioRepository, never()).deleteById(anyLong());
    }

    private Duenio crearDuenio() {
        return new Duenio(1L, "Ana", "Gomez", "1122334455", "ana@mail.com", "12345678");
    }
}
