package ch.legali.agent.example;

import ch.legali.agent.sdk.event.GenericRemoteEvent;
import ch.legali.agent.sdk.event.RemoteEventConfirmEvent;
import ch.legali.agent.sdk.internal.HealthService;
import ch.legali.agent.sdk.internal.client.EventClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ExampleRemoteEventService {
  private static final Logger log = LoggerFactory.getLogger(ExampleRemoteEventService.class);
  private final ApplicationEventPublisher applicationEventPublisher;
  private final EventClient eventClient;

  public ExampleRemoteEventService(
      ApplicationEventPublisher applicationEventPublisher, EventClient eventClient) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.eventClient = eventClient;
  }

  /** On connector start, ping the API to request an asynchronous pong event */
  @EventListener
  public void onStartConnectorEvent(
      @SuppressWarnings("unused") HealthService.StartConnectorEvent event) {
    log.info("🏓 Requesting a pong remote event");
    this.eventClient.ping();
  }

  /**
   * Handles remote events
   *
   * @param remoteEvent any event
   */
  @EventListener
  public void handleDummyRemoteEvent(GenericRemoteEvent remoteEvent) {
    log.info(
        "🏓 Remote Event received: "
            + remoteEvent.getId()
            + " payload: "
            + remoteEvent.getPayload());

    // Ack the event
    this.applicationEventPublisher.publishEvent(new RemoteEventConfirmEvent(remoteEvent.getId()));
  }
}
