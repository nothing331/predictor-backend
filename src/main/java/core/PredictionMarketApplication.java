package core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "core", "api" })
public class PredictionMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(PredictionMarketApplication.class, args);
    }
}