package com.meridian.dacp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import com.meridian.dacp.service.DacpService;

@SpringBootApplication(scanBasePackages = {"com.meridian.dacp"})
@EnableAsync
public class Application {
    
    @Value("${dapp.port:3689}") int port;

    @Value("${dapp.sessionId:51}") int sessionId;

    @Bean
    public DacpService dacpService() {
        return new DacpService(port, sessionId);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    public static void main(String [] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
