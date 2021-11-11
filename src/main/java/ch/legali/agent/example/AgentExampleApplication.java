package ch.legali.agent.example;

import ch.legali.agent.sdk.LegaliAgentSdk;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackageClasses = {LegaliAgentSdk.class, AgentExampleApplication.class})
@EnableScheduling
public class AgentExampleApplication {

  @SuppressWarnings("resource")
  public static void main(String[] args) {
    SpringApplication.run(AgentExampleApplication.class, args);
  }
}
