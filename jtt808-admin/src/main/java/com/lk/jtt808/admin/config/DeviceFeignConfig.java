package com.lk.jtt808.admin.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * admin 调用 device 内部接口时附加内部鉴权头。
 */
@Configuration
public class DeviceFeignConfig {

    @Bean
    public RequestInterceptor internalApiTokenInterceptor(
            @Value("${jtt808.internal-api.auth.header-name:X-Internal-Token}") String headerName,
            @Value("${jtt808.internal-api.auth.token:dev-internal-token}") String token) {
        return template -> {
            if (StringUtils.hasText(token)) {
                template.header(headerName, token);
            }
        };
    }
}
