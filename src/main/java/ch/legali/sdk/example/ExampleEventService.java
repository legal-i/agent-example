package ch.legali.sdk.example;

import ch.legali.api.events.ExportCreatedEvent;
import ch.legali.api.events.ExportSharedEvent;
import ch.legali.api.events.ExportViewedEvent;
import ch.legali.api.events.LegalCaseCreatedEvent;
import ch.legali.api.events.LegalCaseReadyEvent;
import ch.legali.api.events.LegalCaseStatusChangedEvent;
import ch.legali.api.events.LegalCaseUpdatedEvent;
import ch.legali.api.events.PongEvent;
import ch.legali.api.events.SourceFileCreatedEvent;
import ch.legali.api.events.SourceFileTaskFailedEvent;
import ch.legali.api.events.SourceFileUpdatedEvent;
import ch.legali.sdk.internal.HealthService;
import ch.legali.sdk.services.EventService;
import ch.legali.sdk.services.FileService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** This service is used to react to events form the legal-i cloud. */
@Service
public class ExampleEventService {
  private static final Logger log = LoggerFactory.getLogger(ExampleEventService.class);
  private final FileService fileService;
  private final EventService eventService;

  public ExampleEventService(FileService fileService, EventService eventService) {
    this.fileService = fileService;
    this.eventService = eventService;
  }

  @PostConstruct
  public void init() {
    // NOTE: all events that the agent subscribes to, need to be handled by an event listener.
    this.eventService.subscribe(
        PongEvent.class,

        // legalcase CRUD through frontend
        LegalCaseCreatedEvent.class,
        LegalCaseStatusChangedEvent.class,
        LegalCaseUpdatedEvent.class,

        // all sourcefiles processed
        LegalCaseReadyEvent.class,

        // sourcefiles CRUD through frontend
        SourceFileCreatedEvent.class,
        SourceFileUpdatedEvent.class,

        // processing error
        SourceFileTaskFailedEvent.class,

        // export
        ExportCreatedEvent.class,
        ExportSharedEvent.class,
        ExportViewedEvent.class);
  }

  /** On connector start, ping the API to request a pong event */
  @EventListener
  public void onStartConnectorEvent(
      @SuppressWarnings("unused") HealthService.StartConnectorEvent event) {
    log.info("🏓 Requesting a pong remote event");
    this.eventService.ping();
  }

  /*
   * NOTE: all events that the agent subscribes to, need to be handled by an event listener.
   */

  @EventListener
  public void handle(PongEvent event) {
    log.info("🏓 PingPong Event received: " + "\nid " + event.getUuid());
    this.eventService.acknowledge(event);
  }

  // legalcase handlers
  @EventListener
  public void handle(LegalCaseCreatedEvent event) {
    log.info(
        "LegalCaseCreatedEvent: "
            + "\n"
            + event.getLegalCase().getFirstname()
            + " "
            + event.getLegalCase().getLastname());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(LegalCaseStatusChangedEvent event) {
    log.info(
        "LegalCaseStatusChangedEvent: "
            + "\n"
            + event.getLegalCaseUuid()
            + " "
            + event.getStatus());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(LegalCaseUpdatedEvent event) {
    log.info(
        "LegalCaseUpdatedEvent: "
            + "\n"
            + event.getLegalCase().getFirstname()
            + " "
            + event.getLegalCase().getLastname());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(LegalCaseReadyEvent event) {
    log.info("LegalCaseReadyEvent: " + "\n" + event.getLegalCaseUuid());
    this.eventService.acknowledge(event);
  }

  // sourcefiles handler

  @EventListener
  public void handle(SourceFileCreatedEvent event) {
    log.info("SourceFileCreatedEvent: " + "\n" + event.getSourceFile().getSourceFileUUID());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(SourceFileUpdatedEvent event) {
    log.info("SourceFileUpdatedEvent: " + "\n" + event.getField());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(SourceFileTaskFailedEvent event) {
    log.info("SourceFileTaskFailedEvent: " + "\n" + event.getSourceFileUuid());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(ExportCreatedEvent event) {
    log.info("🍻  ExportCreatedEvent: " + event.getExport().exportUUID());
    log.info("    Recipient : " + event.getExport().recipient());
    log.info("    Case Id   : " + event.getExport().legalCaseUUID());
    log.info("    Timestamp : " + event.getTs());

    try (InputStream is = this.fileService.downloadFile(event.getExport().fileUri())) {
      Files.copy(is, Path.of("./dummy.pdf"), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
    log.info("⤵️  Downloaded file: {}", event.getExport().fileUri());

    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(ExportSharedEvent event) {
    log.info(
        "✉️ ExportSharedEvent: "
            + event.getExport().exportUUID()
            + "\n"
            + event.getMethod()
            + "\n"
            + event.getExport().fileUri()
            + "\n"
            + event.getEmail());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(ExportViewedEvent event) {
    log.info(
        "📖 ExportViewedEvent: "
            + "\n"
            + event.getExport().legalCaseUUID()
            + " "
            + event.getUser().getRemoteAddr());
    this.eventService.acknowledge(event);
  }
}
