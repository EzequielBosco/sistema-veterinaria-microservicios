package com.veterinaria.duenio.application;

import com.veterinaria.duenio.domain.exception.DuenioNotFoundException;
import com.veterinaria.duenio.domain.exception.DuplicatedCedulaException;
import com.veterinaria.duenio.domain.exception.DuplicatedEmailException;
import com.veterinaria.duenio.domain.model.Duenio;
import com.veterinaria.duenio.domain.port.in.DuenioService;
import com.veterinaria.duenio.domain.port.out.DuenioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de Duenio. Depende solo de los puertos del dominio,
 * nunca de adapter.* (Dependency Rule).
 */
@Service
public class DuenioServiceImpl implements DuenioService {

    private final DuenioRepositoryPort duenioRepository;

    public DuenioServiceImpl(DuenioRepositoryPort duenioRepository) {
        this.duenioRepository = duenioRepository;
    }

    @Override
    public List<Duenio> findAll() {
        return duenioRepository.findAll();
    }

    @Override
    public Duenio findById(Long id) {
        return duenioRepository.findById(id)
                .orElseThrow(() -> new DuenioNotFoundException("No existe un duenio con id " + id));
    }

    @Override
    public Duenio findByCedula(String cedula) {
        return duenioRepository.findByCedula(cedula)
                .orElseThrow(() -> new DuenioNotFoundException("No existe un duenio con cedula/DNI " + cedula));
    }

    @Override
    public Duenio findByEmail(String email) {
        return duenioRepository.findByEmail(email)
                .orElseThrow(() -> new DuenioNotFoundException("No existe un duenio con email " + email));
    }

    @Override
    public List<Duenio> search(String nombre, String apellido) {
        return duenioRepository.findByNombreAndApellido(nombre, apellido);
    }

    @Override
    @Transactional
    public Duenio save(Duenio duenio) {
        normalizarEmail(duenio);
        if (duenioRepository.existsByCedula(duenio.getCedula())) {
            throw new DuplicatedCedulaException(duenio.getCedula());
        }
        if (duenio.getEmail() != null && duenioRepository.existsByEmail(duenio.getEmail())) {
            throw new DuplicatedEmailException(duenio.getEmail());
        }
        return duenioRepository.save(duenio);
    }

    @Override
    @Transactional
    public Duenio update(Long id, Duenio duenio) {
        normalizarEmail(duenio);
        Duenio duenioExistente = findById(id);

        if (!duenioExistente.getCedula().equals(duenio.getCedula())
                && duenioRepository.existsByCedula(duenio.getCedula())) {
            throw new DuplicatedCedulaException(duenio.getCedula());
        }
        if (duenio.getEmail() != null
                && !duenio.getEmail().equals(duenioExistente.getEmail())
                && duenioRepository.existsByEmail(duenio.getEmail())) {
            throw new DuplicatedEmailException(duenio.getEmail());
        }

        duenioExistente.setNombre(duenio.getNombre());
        duenioExistente.setApellido(duenio.getApellido());
        duenioExistente.setTelefono(duenio.getTelefono());
        duenioExistente.setEmail(duenio.getEmail());
        duenioExistente.setCedula(duenio.getCedula());
        return duenioRepository.save(duenioExistente);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        findById(id);
        duenioRepository.deleteById(id);
    }

    private void normalizarEmail(Duenio duenio) {
        if (duenio.getEmail() != null && duenio.getEmail().isBlank()) {
            duenio.setEmail(null);
        }
    }
}
