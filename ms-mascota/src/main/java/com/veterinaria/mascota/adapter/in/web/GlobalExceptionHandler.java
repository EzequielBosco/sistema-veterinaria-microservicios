package com.veterinaria.mascota.adapter.in.web;

import com.veterinaria.mascota.domain.exception.BadRequestException;
import com.veterinaria.mascota.domain.exception.CupoMascotasExcedidoException;
import com.veterinaria.mascota.domain.exception.DuenioNotFoundException;
import com.veterinaria.mascota.domain.exception.MascotaNotFoundException;
import com.veterinaria.mascota.domain.exception.ServicioNoDisponibleException;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones de la capa web a codigos HTTP + un ErrorResponse uniforme.
 * El 404 de aca es el que ms-turno recibira cuando pregunte por una mascota inexistente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Mascota inexistente -> 404 Not Found */
    @ExceptionHandler(MascotaNotFoundException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrada(MascotaNotFoundException ex,
                                                             HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** El duenio referenciado no existe en ms-duenio -> 404 Not Found */
    @ExceptionHandler(DuenioNotFoundException.class)
    public ResponseEntity<ErrorResponse> manejarDuenio(DuenioNotFoundException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** ms-duenio no responde: reintentos agotados o circuito abierto (fallback de Resilience4j) -> 503 */
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarServicioNoDisponible(ServicioNoDisponibleException ex,
                                                                     HttpServletRequest request) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    /**
     * Red de seguridad: un fallo Feign que no paso por el fallback -> 503 Service Unavailable.
     * El 404 remoto no llega aca: lo traduce el DuenioFeignAdapter.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> manejarFeign(FeignException ex,
                                                      HttpServletRequest request) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio remoto no disponible: " + ex.getMessage(), request);
    }

    /** Duenio con el cupo de mascotas completo -> 422 Unprocessable Entity */
    @ExceptionHandler(CupoMascotasExcedidoException.class)
    public ResponseEntity<ErrorResponse> manejarCupoExcedido(CupoMascotasExcedidoException ex,
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

    /** JSON mal formado o tipos invalidos (ej. sexo distinto de MACHO/HEMBRA) -> 400 Bad Request */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoInvalido(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es invalido", request);
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
