package com.veterinaria.gateway.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LoggingFilterTest {

    private final LoggingFilter filter = new LoggingFilter();

    @Test
    void dejaPasarElRequestYLogueaEntradaYSalidaConStatus(CapturedOutput output) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/duenios/1"));
        AtomicBoolean cadenaInvocada = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            cadenaInvocada.set(true);
            ex.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(cadenaInvocada).isTrue();
        assertThat(output).contains("[GATEWAY] --> GET /api/duenios/1");
        assertThat(output).contains("[GATEWAY] <-- GET /api/duenios/1 404");
    }

    @Test
    void sinStatusEnLaRespuesta_noFallaYLogueaGuion(CapturedOutput output) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/turnos"));

        StepVerifier.create(filter.filter(exchange, ex -> Mono.empty())).verifyComplete();

        assertThat(output).contains("[GATEWAY] <-- POST /api/turnos -");
    }

    @Test
    void correAntesQueLosDemasFiltros() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}
