package com.miracle.smart_ecommerce_security.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Bean
    public RestTemplate restTemplate() {
        ClientHttpRequestInterceptor correlationInterceptor = (request, body, execution) -> {
            String cid = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            if (cid != null) {
                request.getHeaders().add(CORRELATION_HEADER, cid);
            }
            return execution.execute(request, body);
        };

        RestTemplate rt = new RestTemplate();
        rt.setInterceptors(List.of(correlationInterceptor));
        return rt;
    }
}
