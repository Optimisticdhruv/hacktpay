package com.recoverai;

import com.recoverai.config.RecoveryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RecoveryProperties.class)
public class RecoverAiApplication {
    public static void main(String[] args) { SpringApplication.run(RecoverAiApplication.class, args); }
}
