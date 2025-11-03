package pt.zeromonos.garbagecollection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class GarbageCollectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GarbageCollectionServiceApplication.class, args);
    }

    @Bean // Esta anotação diz ao Spring para criar e gerir este objeto
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
