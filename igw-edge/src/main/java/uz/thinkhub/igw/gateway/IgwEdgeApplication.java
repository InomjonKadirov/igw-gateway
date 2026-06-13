package uz.thinkhub.igw.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("uz.thinkhub.igw.gateway")
public class IgwEdgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(IgwEdgeApplication.class, args);
    }
}
