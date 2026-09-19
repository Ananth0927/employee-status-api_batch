package com.example.employeebatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmployeeStatusBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeStatusBatchApplication.class, args);
    }
}
