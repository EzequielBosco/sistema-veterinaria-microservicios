package com.veterinaria.veterinario.application;

import com.veterinaria.veterinario.domain.exception.DuplicateEmailException;
import com.veterinaria.veterinario.domain.exception.DuplicateMatriculaException;
import com.veterinaria.veterinario.domain.exception.VeterinarioNotFoundException;
import com.veterinaria.veterinario.domain.model.Veterinario;
import com.veterinaria.veterinario.domain.port.in.VeterinarioService;
import com.veterinaria.veterinario.domain.port.out.VeterinarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de negocio de Veterinario. Depende SOLO de puertos (in/out), nunca de adapter.*
 * La agenda de turnos de un veterinario ahora la expone ms-turno.
 */
@Service
@RequiredArgsConstructor
public class VeterinarioServiceImpl implements VeterinarioService {

    private final VeterinarioRepositoryPort veterinarioRepository;

    @Override
    public List<Veterinario> listarTodos() {
        return veterinarioRepository.buscarTodos();
    }

    @Override
    public Veterinario buscarPorId(Long id) {
        Veterinario veterinario = veterinarioRepository.buscarPorId(id);
        if (veterinario == null) {
            throw new VeterinarioNotFoundException(id);
        }
        return veterinario;
    }

    @Override
    public List<Veterinario> buscarPorEspecialidad(String especialidad) {
        return veterinarioRepository.buscarPorEspecialidad(especialidad.trim());
    }

    @Override
    @Transactional
    public Veterinario registrar(Veterinario veterinario) {
        normalizarEmail(veterinario);
        validarMatriculaDisponible(veterinario.getMatricula());
        validarEmailDisponible(veterinario.getEmail());
        return veterinarioRepository.guardar(veterinario);
    }

    @Override
    @Transactional
    public Veterinario actualizar(Long id, Veterinario veterinario) {
        normalizarEmail(veterinario);
        Veterinario existente = buscarPorId(id);

        if (!existente.getMatricula().equals(veterinario.getMatricula())) {
            validarMatriculaDisponible(veterinario.getMatricula());
        }
        if (veterinario.getEmail() != null && !veterinario.getEmail().equals(existente.getEmail())) {
            validarEmailDisponible(veterinario.getEmail());
        }

        existente.setNombre(veterinario.getNombre());
        existente.setApellido(veterinario.getApellido());
        existente.setTelefono(veterinario.getTelefono());
        existente.setEmail(veterinario.getEmail());
        existente.setMatricula(veterinario.getMatricula());
        existente.setEspecialidad(veterinario.getEspecialidad());
        return veterinarioRepository.guardar(existente);
    }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        buscarPorId(id); // valida que exista antes de eliminar
        veterinarioRepository.eliminarPorId(id);
    }

    private void validarMatriculaDisponible(String matricula) {
        if (veterinarioRepository.existePorMatricula(matricula)) {
            throw new DuplicateMatriculaException(matricula);
        }
    }

    private void validarEmailDisponible(String email) {
        if (email != null && veterinarioRepository.existePorEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }

    private void normalizarEmail(Veterinario veterinario) {
        if (veterinario.getEmail() != null && veterinario.getEmail().isBlank()) {
            veterinario.setEmail(null);
        }
    }
}
