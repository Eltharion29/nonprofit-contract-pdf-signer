package org.givingledger.contracts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ContractSigningApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContractSigningApplication.class, args);
    }
}
