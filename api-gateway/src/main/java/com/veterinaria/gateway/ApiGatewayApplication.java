package com.veterinaria.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: unico punto de entrada (puerto 8080) para los clientes externos.
 * No requiere anotaciones especiales: el gateway se activa por la dependencia
 * spring-cloud-starter-gateway-server-webflux. Las rutas vienen del Config Server (api-gateway.yml).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
