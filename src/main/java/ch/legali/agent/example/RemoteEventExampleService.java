package ch.legali.agent.example;

import ch.legali.agent.sdk.event.GenericRemoteEvent;
import ch.legali.agent.sdk.event.RemoteEventConfirmEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class RemoteEventExampleService {

  private final ApplicationEventPublisher applicationEventPublisher;

  public RemoteEventExampleService(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @EventListener
  public void handleDumyRemoteEvent(GenericRemoteEvent jsonRemoteEvent) {
    System.out.println(
        "Remote Event received: "
            + jsonRemoteEvent.getId()
            + " payload: "
            + jsonRemoteEvent.getPayload());

    this.applicationEventPublisher.publishEvent(
        new RemoteEventConfirmEvent(jsonRemoteEvent.getId()));
  }
}
