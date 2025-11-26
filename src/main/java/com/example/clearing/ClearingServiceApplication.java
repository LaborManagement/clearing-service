package com.example.clearing;

import com.shared.security.EnableSharedSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableSharedSecurity
@ComponentScan(basePackages = {
        "com.example.clearing",
        "com.shared" // pick up shared-lib components (e.g., TenantAccessDao)
})
public class ClearingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearingServiceApplication.class, args);
    }
}
