package com.veterinaria.gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logging centralizado: registra cada request que entra al gateway y su respuesta (status y duracion).
 * Es un GlobalFilter, asi que se aplica a TODAS las rutas. Es reactivo: chain.filter(...) devuelve un Mono
 * que completa cuando el microservicio respondio, y recien ahi (then) se loguea la salida.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();
        String metodo = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        log.info("[GATEWAY] --> {} {}", metodo, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duracion = System.currentTimeMillis() - inicio;
            // Puede ser null si la respuesta todavia no tiene status (ej. la conexion se corto)
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("[GATEWAY] <-- {} {} {} {}ms", metodo, path, status != null ? status.value() : "-", duracion);
        }));
    }

    /** -1: corre antes que los demas filtros (a menor numero, mayor prioridad), asi loguea todo. */
    @Override
    public int getOrder() {
        return -1;
    }
}
