package wingorithm.ticketing.vibecoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketingVcApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketingVcApplication.class, args);
    }

}
