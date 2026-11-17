package com.veterinaria.turno.adapter.in.web;

import com.veterinaria.turno.domain.exception.BadRequestException;
import com.veterinaria.turno.domain.exception.EstadoTurnoInvalidoException;
import com.veterinaria.turno.domain.exception.MascotaNotFoundException;
import com.veterinaria.turno.domain.exception.TurnoNotFoundException;
import com.veterinaria.turno.domain.exception.TurnoSuperpuestoException;
import com.veterinaria.turno.domain.exception.VeterinarioNotFoundException;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** Traduce las excepciones de ms-turno a codigos HTTP + un ErrorResponse uniforme. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Turno inexistente -> 404 Not Found */
    @ExceptionHandler(TurnoNotFoundException.class)
    public ResponseEntity<ErrorResponse> manejarTurno(TurnoNotFoundException ex,
                                                      HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** La mascota referenciada no existe en ms-mascota -> 404 Not Found */
    @ExceptionHandler(MascotaNotFoundException.class)
    public ResponseEntity<ErrorResponse> manejarMascota(MascotaNotFoundException ex,
                                                        HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Algun veterinario referenciado no existe en ms-veterinario -> 404 Not Found */
    @ExceptionHandler(VeterinarioNotFoundException.class)
    public ResponseEntity<ErrorResponse> manejarVeterinario(VeterinarioNotFoundException ex,
                                                            HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Fallo en la comunicacion Feign (servicio remoto caido, sin instancias en Eureka, timeout
     * o error 5xx del otro microservicio) -> 503 Service Unavailable.
     * El 404 remoto no llega aca: lo traducen los adaptadores Feign a "no existe".
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> manejarFeign(FeignException ex,
                                                      HttpServletRequest request) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio remoto no disponible: " + ex.getMessage(), request);
    }

    /** Un veterinario ya tiene otro turno en ese horario -> 409 Conflict */
    @ExceptionHandler(TurnoSuperpuestoException.class)
    public ResponseEntity<ErrorResponse> manejarSuperpuesto(TurnoSuperpuestoException ex,
                                                            HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** El estado del turno no permite la operacion (ej. cancelar uno ya atendido) -> 422 Unprocessable Entity */
    @ExceptionHandler(EstadoTurnoInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarEstadoInvalido(EstadoTurnoInvalidoException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /** Regla de negocio incumplida en el request -> 400 Bad Request */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> manejarBadRequest(BadRequestException ex,
                                                           HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Fallos de @Valid (DTO invalido) -> 400 Bad Request */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construir(HttpStatus.BAD_REQUEST, detalle, request);
    }

    /** JSON mal formado o tipos invalidos (ej. rol inexistente) -> 400 Bad Request */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoInvalido(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es invalido", request);
    }

    /** Parametro de URL con formato invalido (ej. fecha) -> 400 Bad Request */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarParametroInvalido(MethodArgumentTypeMismatchException ex,
                                                                  HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El parametro '" + ex.getName() + "' tiene un formato invalido", request);
    }

    /** Ruta inexistente -> 404 Not Found */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(NoResourceFoundException ex,
                                                                HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "Ruta no encontrada: " + request.getRequestURI(), request);
    }

    /** Cualquier otra excepcion no prevista -> 500 Internal Server Error */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGenerico(Exception ex,
                                                         HttpServletRequest request) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje,
                                                    HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
