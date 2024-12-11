package global.alyssa.hl7mllpdemo;

import global.alyssa.hl7mllpdemo.listener.HL7MLLPListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Hl7MllpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Hl7MllpDemoApplication.class, args);

        // Start the MLLP Listener in a separate thread
        new Thread(new HL7MLLPListener(2575)).start();
    }

}
