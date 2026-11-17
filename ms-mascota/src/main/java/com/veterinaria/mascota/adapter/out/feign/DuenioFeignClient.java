package com.veterinaria.mascota.adapter.out.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo hacia ms-duenio.
 * name = "ms-duenio" DEBE coincidir con el spring.application.name de ese servicio, tal como
 * aparece en Eureka. Spring Cloud LoadBalancer resuelve el nombre contra el registro de Eureka.
 */
@FeignClient(name = "ms-duenio")
public interface DuenioFeignClient {

    @GetMapping(value = "/api/duenios/{id}", produces = "application/json")
    DuenioResponse findById(@PathVariable("id") Long id);
}
