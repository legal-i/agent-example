package ch.legali.agent.example;

import ch.legali.agent.sdk.event.NamedRemoteEvent;
import ch.legali.agent.sdk.event.RemoteEventConfirmEvent;
import ch.legali.agent.sdk.internal.HealthService;
import ch.legali.agent.sdk.internal.client.EventClient;
import ch.legali.agent.sdk.models.SourceFileDTO;
import ch.legali.agent.sdk.services.FileDownloadService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ExampleRemoteEventService {
  private static final Logger log = LoggerFactory.getLogger(ExampleRemoteEventService.class);
  private final ApplicationEventPublisher applicationEventPublisher;
  private final FileDownloadService fileDownloadService;
  private final EventClient eventClient;

  public ExampleRemoteEventService(
      ApplicationEventPublisher applicationEventPublisher,
      FileDownloadService fileDownloadService,
      EventClient eventClient) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.fileDownloadService = fileDownloadService;
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
   * Handles remote events NOTE: on startup of the example, you will get all pending events
   *
   * @param remoteEvent any event
   */
  @EventListener
  public void handleDummyRemoteEvent(NamedRemoteEvent remoteEvent) {
    switch (remoteEvent.getName()) {

        // Status Change
      case "SOURCEFILE_STATUS_CHANGED":
        SourceFileDTO.Status status =
            SourceFileDTO.Status.valueOf(remoteEvent.getPayloadStr("status"));
        log.info(
            "⏭  Source file status changed for "
                + remoteEvent.getPayloadStr("uuid")
                + " to "
                + status
                + ". Message: "
                + remoteEvent.getPayloadStr("message"));
        break;

        // Documents Export
      case "LEGALCASE_DOCUMENTS_EXPORTED":
        log.info("🍻  New export: " + remoteEvent.getPayload());
        File file = this.fileDownloadService.downloadFile(remoteEvent.getPayloadStr("uri"));
        try {
          Files.copy(file.toPath(), Path.of("./dummy.pdf"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          e.printStackTrace();
        }
        log.info("⤵️  Downloaded file: {}, Size: {}", file.getName(), file.length());
        break;

        // Debug events
      default:
        log.info(
            "🏓 Remote Event received: "
                + "\nname "
                + remoteEvent.getName()
                + "\npayload: "
                + remoteEvent.getPayload()
                + "\nid "
                + remoteEvent.getId());
        break;
    }

    // Ack the event
    this.applicationEventPublisher.publishEvent(new RemoteEventConfirmEvent(remoteEvent.getId()));
  }
}
