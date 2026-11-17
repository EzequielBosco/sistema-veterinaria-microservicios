package com.veterinaria.veterinario.application;

import com.veterinaria.veterinario.domain.exception.DuplicateEmailException;
import com.veterinaria.veterinario.domain.exception.DuplicateMatriculaException;
import com.veterinaria.veterinario.domain.exception.VeterinarioNotFoundException;
import com.veterinaria.veterinario.domain.model.Veterinario;
import com.veterinaria.veterinario.domain.port.out.VeterinarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VeterinarioServiceImplTest {

    @Mock
    private VeterinarioRepositoryPort veterinarioRepository;

    @InjectMocks
    private VeterinarioServiceImpl veterinarioService;

    @Test
    void listarTodos_retornaVeterinarios() {
        when(veterinarioRepository.buscarTodos()).thenReturn(List.of(crearVeterinario()));

        assertThat(veterinarioService.listarTodos()).hasSize(1);
    }

    @Test
    void buscarPorId_cuandoExiste_retornaVeterinario() {
        when(veterinarioRepository.buscarPorId(1L)).thenReturn(crearVeterinario());

        assertThat(veterinarioService.buscarPorId(1L).getMatricula()).isEqualTo("MV-4521");
    }

    @Test
    void buscarPorId_cuandoNoExiste_lanzaVeterinarioNotFoundException() {
        when(veterinarioRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(VeterinarioNotFoundException.class, () -> veterinarioService.buscarPorId(99L));
    }

    @Test
    void registrar_cuandoEsValido_guardaVeterinario() {
        Veterinario veterinario = crearVeterinario();
        when(veterinarioRepository.existePorMatricula("MV-4521")).thenReturn(false);
        when(veterinarioRepository.existePorEmail("maria@clinica.com")).thenReturn(false);
        when(veterinarioRepository.guardar(veterinario)).thenReturn(veterinario);

        assertThat(veterinarioService.registrar(veterinario)).isEqualTo(veterinario);
    }

    @Test
    void registrar_conMatriculaDuplicada_lanzaDuplicateMatriculaExceptionYNoGuarda() {
        when(veterinarioRepository.existePorMatricula("MV-4521")).thenReturn(true);

        assertThrows(DuplicateMatriculaException.class, () -> veterinarioService.registrar(crearVeterinario()));
        verify(veterinarioRepository, never()).guardar(any(Veterinario.class));
    }

    @Test
    void registrar_conEmailDuplicado_lanzaDuplicateEmailExceptionYNoGuarda() {
        when(veterinarioRepository.existePorMatricula("MV-4521")).thenReturn(false);
        when(veterinarioRepository.existePorEmail("maria@clinica.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> veterinarioService.registrar(crearVeterinario()));
        verify(veterinarioRepository, never()).guardar(any(Veterinario.class));
    }

    @Test
    void registrar_conEmailEnBlanco_normalizaEmailANull() {
        Veterinario veterinario = crearVeterinario();
        veterinario.setEmail(" ");
        when(veterinarioRepository.existePorMatricula("MV-4521")).thenReturn(false);
        when(veterinarioRepository.guardar(veterinario)).thenReturn(veterinario);

        assertThat(veterinarioService.registrar(veterinario).getEmail()).isNull();
        verify(veterinarioRepository, never()).existePorEmail(anyString());
    }

    @Test
    void actualizar_mismaMatriculaYEmail_noValidaDuplicados() {
        Veterinario existente = crearVeterinario();
        Veterinario cambios = crearVeterinario();
        cambios.setEspecialidad("Cirugía");
        when(veterinarioRepository.buscarPorId(1L)).thenReturn(existente);
        when(veterinarioRepository.guardar(existente)).thenReturn(existente);

        assertThat(veterinarioService.actualizar(1L, cambios).getEspecialidad()).isEqualTo("Cirugía");
        verify(veterinarioRepository, never()).existePorMatricula(anyString());
        verify(veterinarioRepository, never()).existePorEmail(anyString());
    }

    @Test
    void actualizar_conMatriculaDeOtro_lanzaDuplicateMatriculaException() {
        Veterinario cambios = crearVeterinario();
        cambios.setMatricula("MV-0001");
        when(veterinarioRepository.buscarPorId(1L)).thenReturn(crearVeterinario());
        when(veterinarioRepository.existePorMatricula("MV-0001")).thenReturn(true);

        assertThrows(DuplicateMatriculaException.class, () -> veterinarioService.actualizar(1L, cambios));
        verify(veterinarioRepository, never()).guardar(any(Veterinario.class));
    }

    @Test
    void eliminarPorId_cuandoExiste_eliminaVeterinario() {
        when(veterinarioRepository.buscarPorId(1L)).thenReturn(crearVeterinario());

        veterinarioService.eliminarPorId(1L);

        verify(veterinarioRepository).eliminarPorId(1L);
    }

    @Test
    void eliminarPorId_cuandoNoExiste_lanzaVeterinarioNotFoundException() {
        when(veterinarioRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(VeterinarioNotFoundException.class, () -> veterinarioService.eliminarPorId(99L));
        verify(veterinarioRepository, never()).eliminarPorId(anyLong());
    }

    private Veterinario crearVeterinario() {
        return new Veterinario(1L, "María", "García", "1199887766", "maria@clinica.com", "MV-4521", "Clínica general");
    }
}
