package com.microsoft.azuresample.acscicdtodo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class JavaWebApp {
    static final Logger LOG = LoggerFactory.getLogger(JavaWebApp.class);
    
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(JavaWebApp.class, args);
        LOG.info("My Spring Boot app started ...");
    }
}
