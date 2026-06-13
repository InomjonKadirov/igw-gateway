package uz.thinkhub.igw.echo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The fake legacy upstream. In production this is the PHP/Yii2 monolith
 * (deployed separately, addressable via {@code igw.edge.upstream}); for
 * Phase 0 we run this small Spring Boot service on {@code localhost:8081}
 * so the gateway has something real to proxy to.
 *
 * <p>Port comes from {@code application.yaml} (default 8081).
 */
@SpringBootApplication
public class EchoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoServerApplication.class, args);
    }
}
