package ch.legali.agent.example;

import ch.legali.agent.sdk.internal.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class StartConnectorEventListener {
  private static final int threadCount = 1;
  private static final Logger log = LoggerFactory.getLogger(HealthService.class);

  private final TaskExecutor taskExecutor;
  private final ApplicationContext applicationContext;

  public StartConnectorEventListener(
      TaskExecutor taskExecutor, ApplicationContext applicationContext) {
    this.taskExecutor = taskExecutor;
    this.applicationContext = applicationContext;
  }

  @EventListener
  public void onStartConnectorEvent(
      @SuppressWarnings("unused") HealthService.StartConnectorEvent event) {
    log.debug("Received StartConnectorEvent");
    for (int i = 0; i < threadCount; i++) {
      ExampleConnector connector = this.applicationContext.getBean(ExampleConnector.class);
      this.taskExecutor.execute(connector);
    }
  }
}
