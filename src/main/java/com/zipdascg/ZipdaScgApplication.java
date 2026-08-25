package com.zipdascg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ZipdaScgApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZipdaScgApplication.class, args);
    }

}
