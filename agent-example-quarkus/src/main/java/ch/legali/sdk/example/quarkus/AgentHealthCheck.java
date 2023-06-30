package ch.legali.sdk.example.quarkus;

import ch.legali.sdk.internal.HealthService;
import ch.legali.sdk.internal.HealthService.HealthState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AgentHealthCheck implements HealthCheck {

  private static final String NAME = "cloud_connection";

  @Inject HealthService healthService;

  @Override
  public HealthCheckResponse call() {
    return this.healthService.getState() == HealthState.READY
        ? HealthCheckResponse.up(NAME)
        : HealthCheckResponse.down(NAME);
  }
}
