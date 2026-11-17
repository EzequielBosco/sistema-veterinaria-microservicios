package com.veterinaria.duenio.application;

import com.veterinaria.duenio.domain.exception.DuenioNotFoundException;
import com.veterinaria.duenio.domain.exception.DuplicateCedulaException;
import com.veterinaria.duenio.domain.exception.DuplicateEmailException;
import com.veterinaria.duenio.domain.model.Duenio;
import com.veterinaria.duenio.domain.port.in.DuenioService;
import com.veterinaria.duenio.domain.port.out.DuenioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de negocio de Duenio. Depende SOLO de puertos (in/out), nunca de adapter.*
 * Así se respeta la Dependency Rule de la arquitectura hexagonal.
 */
@Service
@RequiredArgsConstructor
public class DuenioServiceImpl implements DuenioService {

    private final DuenioRepositoryPort duenioRepository;

    @Override
    public List<Duenio> listarTodos() {
        return duenioRepository.buscarTodos();
    }

    @Override
    public Duenio buscarPorId(Long id) {
        Duenio duenio = duenioRepository.buscarPorId(id);
        if (duenio == null) {
            throw new DuenioNotFoundException(id);
        }
        return duenio;
    }

    @Override
    public Duenio buscarPorCedula(String cedula) {
        return duenioRepository.buscarPorCedula(cedula)
                .orElseThrow(() -> new DuenioNotFoundException("No existe un duenio con cedula/DNI " + cedula));
    }

    @Override
    public Duenio buscarPorEmail(String email) {
        return duenioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new DuenioNotFoundException("No existe un duenio con email " + email));
    }

    @Override
    public List<Duenio> buscarPorNombreYApellido(String nombre, String apellido) {
        return duenioRepository.buscarPorNombreYApellido(nombre, apellido);
    }

    @Override
    @Transactional
    public Duenio registrar(Duenio duenio) {
        normalizarEmail(duenio);
        if (duenioRepository.existePorCedula(duenio.getCedula())) {
            throw new DuplicateCedulaException(duenio.getCedula());
        }
        if (duenio.getEmail() != null && duenioRepository.existePorEmail(duenio.getEmail())) {
            throw new DuplicateEmailException(duenio.getEmail());
        }
        return duenioRepository.guardar(duenio);
    }

    @Override
    @Transactional
    public Duenio actualizar(Long id, Duenio duenio) {
        normalizarEmail(duenio);
        Duenio existente = buscarPorId(id);

        if (!existente.getCedula().equals(duenio.getCedula())
                && duenioRepository.existePorCedula(duenio.getCedula())) {
            throw new DuplicateCedulaException(duenio.getCedula());
        }
        if (duenio.getEmail() != null
                && !duenio.getEmail().equals(existente.getEmail())
                && duenioRepository.existePorEmail(duenio.getEmail())) {
            throw new DuplicateEmailException(duenio.getEmail());
        }

        existente.setNombre(duenio.getNombre());
        existente.setApellido(duenio.getApellido());
        existente.setTelefono(duenio.getTelefono());
        existente.setEmail(duenio.getEmail());
        existente.setCedula(duenio.getCedula());
        return duenioRepository.guardar(existente);
    }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        buscarPorId(id); // valida que exista antes de eliminar
        duenioRepository.eliminarPorId(id);
    }

    private void normalizarEmail(Duenio duenio) {
        if (duenio.getEmail() != null && duenio.getEmail().isBlank()) {
            duenio.setEmail(null);
        }
    }
}
