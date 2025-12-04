package com.kis.wmsapplication.modules.userModule.config;

import com.kis.wmsapplication.modules.userModule.prop.MinioProperties;
import io.minio.MinioClient;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class AppConfig {
    private final MinioProperties minioProperties;
    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder().endpoint(minioProperties.getMinioURL())
                .credentials(minioProperties.getAccessName(), minioProperties.getAccessSecret())
                .build();
    }

}
