package core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "core", "api", "sse" })
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = { "core", "db.entity" })
public class PredictionMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(PredictionMarketApplication.class, args);
    }
}