package ch.legali.agent.example.config;

import ch.legali.agent.sdk.internal.HealthService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AgentHealthIndicator implements HealthIndicator {
  private final HealthService healthService;

  public AgentHealthIndicator(HealthService healthService) {
    this.healthService = healthService;
  }

  @Override
  public Health health() {

    Health.Builder status = Health.up();
    if (this.healthService.getState() == HealthService.HealthState.DOWN) {
      status = Health.down();
    }
    return status.build();
  }
}
