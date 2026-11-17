package com.veterinaria.mascota.application;

import com.veterinaria.mascota.domain.exception.BadRequestException;
import com.veterinaria.mascota.domain.exception.CupoMascotasExcedidoException;
import com.veterinaria.mascota.domain.exception.DuenioNotFoundException;
import com.veterinaria.mascota.domain.exception.MascotaNotFoundException;
import com.veterinaria.mascota.domain.model.Mascota;
import com.veterinaria.mascota.domain.port.in.MascotaService;
import com.veterinaria.mascota.domain.port.out.DuenioServicePort;
import com.veterinaria.mascota.domain.port.out.MascotaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de negocio de Mascota. Depende SOLO de puertos (in/out), nunca de adapter.*
 * El cupo de mascotas por duenio se valida localmente: las mascotas viven en esta BD.
 * La existencia del duenio se valida contra ms-duenio a traves del puerto DuenioServicePort
 * (implementado con Feign), despues de las validaciones locales.
 */
@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private static final int MAX_MASCOTAS_POR_DUENIO = 5;

    private final MascotaRepositoryPort mascotaRepository;
    // Puerto hacia ms-duenio: el servicio no sabe que detras hay una llamada HTTP
    private final DuenioServicePort duenioService;

    @Override
    public List<Mascota> listarTodos() {
        return mascotaRepository.buscarTodos();
    }

    @Override
    public Mascota buscarPorId(Long id) {
        Mascota mascota = mascotaRepository.buscarPorId(id);
        if (mascota == null) {
            throw new MascotaNotFoundException(id);
        }
        return mascota;
    }

    @Override
    public List<Mascota> buscarPorDuenioId(Long duenioId) {
        return mascotaRepository.buscarPorDuenioId(duenioId);
    }

    @Override
    @Transactional
    public Mascota registrar(Mascota mascota) {
        if (mascota.getDuenioId() == null) {
            throw new BadRequestException("Debe indicar el id del duenio de la mascota");
        }
        validarCupoMascotas(mascota.getDuenioId());
        validarDuenioExistente(mascota.getDuenioId());
        return mascotaRepository.guardar(mascota);
    }

    @Override
    @Transactional
    public Mascota actualizar(Long id, Mascota mascota) {
        Mascota existente = buscarPorId(id);
        existente.setNombre(mascota.getNombre());
        existente.setEspecie(mascota.getEspecie());
        existente.setRaza(mascota.getRaza());
        existente.setColor(mascota.getColor());
        existente.setSexo(mascota.getSexo());
        existente.setFechaNacimiento(mascota.getFechaNacimiento());

        if (mascota.getDuenioId() != null) {
            // Solo si cambia de duenio: editar los datos de la mascota no depende de ms-duenio
            if (!mascota.getDuenioId().equals(existente.getDuenioId())) {
                validarCupoMascotas(mascota.getDuenioId());
                validarDuenioExistente(mascota.getDuenioId());
            }
            existente.setDuenioId(mascota.getDuenioId());
        }
        return mascotaRepository.guardar(existente);
    }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        buscarPorId(id); // valida que exista antes de eliminar
        mascotaRepository.eliminarPorId(id);
    }

    private void validarCupoMascotas(Long duenioId) {
        long cantidadMascotas = mascotaRepository.contarPorDuenioId(duenioId);
        if (cantidadMascotas >= MAX_MASCOTAS_POR_DUENIO) {
            throw new CupoMascotasExcedidoException(duenioId, cantidadMascotas, MAX_MASCOTAS_POR_DUENIO);
        }
    }

    /** Validacion cruzada: el duenio vive en OTRO microservicio (ms-duenio). */
    private void validarDuenioExistente(Long duenioId) {
        if (!duenioService.existsById(duenioId)) {
            throw new DuenioNotFoundException(duenioId);
        }
    }
}
