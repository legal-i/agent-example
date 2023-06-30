package ch.legali.sdk.example;

import ch.legali.sdk.LegaliAgentSdk;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackageClasses = {LegaliAgentSdk.class, ExampleApplication.class})
@EnableScheduling
public class ExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }
}
