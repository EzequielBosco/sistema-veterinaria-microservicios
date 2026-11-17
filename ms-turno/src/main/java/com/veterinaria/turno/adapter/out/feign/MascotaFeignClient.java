package com.veterinaria.turno.adapter.out.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo hacia ms-mascota.
 * name = "ms-mascota" DEBE coincidir con el spring.application.name de ese servicio, tal como
 * aparece en Eureka. Spring Cloud LoadBalancer resuelve el nombre contra el registro de Eureka
 * en cada llamada: no hay IP ni puerto hardcodeados.
 */
@FeignClient(name = "ms-mascota")
public interface MascotaFeignClient {

    @GetMapping(value = "/api/mascotas/{id}", produces = "application/json")
    MascotaResponse findById(@PathVariable("id") Long id);
}
