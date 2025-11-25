package com.example.clearing;

import com.shared.security.EnableSharedSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSharedSecurity
public class ClearingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearingServiceApplication.class, args);
    }
}
