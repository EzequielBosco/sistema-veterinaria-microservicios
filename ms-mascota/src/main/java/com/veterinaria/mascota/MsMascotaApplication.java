package com.veterinaria.mascota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Arranque de ms-mascota.
 * @EnableFeignClients hace que Spring escanee las interfaces @FeignClient (en adapter.out.feign)
 * y genere su implementacion HTTP en tiempo de ejecucion.
 */
@SpringBootApplication
@EnableFeignClients
public class MsMascotaApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsMascotaApplication.class, args);
    }
}
