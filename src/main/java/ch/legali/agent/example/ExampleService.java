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
public class ExampleService {
  private static final Logger log = LoggerFactory.getLogger(HealthService.class);

  private final TaskExecutor taskExecutor;
  private final ApplicationContext applicationContext;
  private final ExampleThread exampleThreadBean;
  private final ExampleConfig config;

  public ExampleService(
      TaskExecutor taskExecutor,
      ApplicationContext applicationContext,
      ExampleThread exampleThreadBean,
      ExampleConfig config) {
    this.taskExecutor = taskExecutor;
    this.applicationContext = applicationContext;
    this.exampleThreadBean = exampleThreadBean;
    this.config = config;
  }

  /**
   * Start connector threads on {@link HealthService.StartConnectorEvent)
   */
  @EventListener
  public void onStartConnectorEvent(
      @SuppressWarnings("unused") HealthService.StartConnectorEvent event)
      throws InterruptedException {
    log.info("Received StartConnectorEvent, let's go!");

    if (this.config.isCleanup()) {
      this.exampleThreadBean.cleanup();
    }

    final int threadPoolSize = ((ThreadPoolTaskExecutor) this.taskExecutor).getMaxPoolSize();
    for (int i = 0; i < threadPoolSize; i++) {
      Thread.sleep(500);
      ExampleThread connector = this.applicationContext.getBean(ExampleThread.class);
      this.taskExecutor.execute(connector);
    }
  }
}
