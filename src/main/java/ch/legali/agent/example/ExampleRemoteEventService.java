package ch.legali.agent.example;

import ch.legali.agent.sdk.event.GenericRemoteEvent;
import ch.legali.agent.sdk.event.RemoteEventConfirmEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ExampleRemoteEventService {

  private final ApplicationEventPublisher applicationEventPublisher;

  public ExampleRemoteEventService(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @EventListener
  public void handleDummyRemoteEvent(GenericRemoteEvent remoteEvent) {

    System.out.println(
        "Remote Event received: "
            + remoteEvent.getId()
            + " payload: "
            + remoteEvent.getPayload());

    // confirm event
    this.applicationEventPublisher.publishEvent(
        new RemoteEventConfirmEvent(remoteEvent.getId()));
  }
}
