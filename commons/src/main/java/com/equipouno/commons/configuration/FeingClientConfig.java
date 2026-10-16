package com.equipouno.commons.configuration;

import com.equipouno.commons.clients.FeingErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeingClientConfig {

    @Bean
    ErrorDecoder errorDecoder(){
        return new FeingErrorDecoder();
    }

    @Bean
    public RequestInterceptor  requestInterceptor() {
        return template -> {
            ServletRequestAttributes  attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authorizationHeader = request.getHeader("Authorization");
                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    template.header("Authorization", authorizationHeader);
                }
            }
        };
    }
}
