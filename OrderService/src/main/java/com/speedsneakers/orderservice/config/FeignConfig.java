package com.speedsneakers.orderservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propaga la cookie "token" del request original a las llamadas Feign hacia
 * otros microservicios (p. ej. ProductServiceElastic), que ahora validan el
 * JWT de forma independiente y rechazan las llamadas sin él.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor cookieForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null) {
                template.header("Cookie", cookieHeader);
            }
        };
    }
}
