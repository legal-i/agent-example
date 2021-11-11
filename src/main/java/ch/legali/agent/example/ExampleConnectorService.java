package ch.legali.agent.example;

import ch.legali.agent.sdk.internal.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class ExampleConnectorService {
  private static final Logger log = LoggerFactory.getLogger(HealthService.class);

  private final TaskExecutor taskExecutor;
  private final ApplicationContext applicationContext;

  public ExampleConnectorService(TaskExecutor taskExecutor, ApplicationContext applicationContext) {
    this.taskExecutor = taskExecutor;
    this.applicationContext = applicationContext;
  }

  @EventListener
  public void onStartConnectorEvent(
      @SuppressWarnings("unused") HealthService.StartConnectorEvent event)
      throws InterruptedException {
    log.info("Received StartConnectorEvent, let's go!");

    final int threadPoolSize = ((ThreadPoolTaskExecutor) this.taskExecutor).getMaxPoolSize();
    for (int i = 0; i < threadPoolSize; i++) {
      Thread.sleep(500);
      ExampleConnector connector = this.applicationContext.getBean(ExampleConnector.class);
      this.taskExecutor.execute(connector);
    }
  }
}
