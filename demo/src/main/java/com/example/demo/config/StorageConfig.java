package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

@Configuration
public class StorageConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        createUploadDirectoryIfNotExists();
        
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String resourcePath = uploadPath.toUri().toString();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourcePath)
                .setCachePeriod(3600);
    }
    
    private void createUploadDirectoryIfNotExists() {
        File directory = Paths.get(uploadDir).toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}