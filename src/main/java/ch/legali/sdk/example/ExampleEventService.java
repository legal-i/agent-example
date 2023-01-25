package ch.legali.sdk.example;

import ch.legali.api.events.ExportCreatedEvent;
import ch.legali.api.events.ExportSharedEvent;
import ch.legali.api.events.ExportViewedEvent;
import ch.legali.api.events.LegalCaseCreatedEvent;
import ch.legali.api.events.LegalCaseReadyEvent;
import ch.legali.api.events.LegalCaseStatusChangedEvent;
import ch.legali.api.events.LegalCaseUpdatedEvent;
import ch.legali.api.events.NotebookUpdatedEvent;
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
        NotebookUpdatedEvent.class,

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
    log.info("🏓 PingPong Event received: " + "\nid " + event.id());
    this.eventService.acknowledge(event);
  }

  // legalcase handlers
  @EventListener
  public void handle(LegalCaseCreatedEvent event) {
    log.info(
        "LegalCaseCreatedEvent: "
            + "\n"
            + event.legalCase().firstname()
            + " "
            + event.legalCase().lastname());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(LegalCaseStatusChangedEvent event) {
    log.info("LegalCaseStatusChangedEvent: " + "\n" + event.legalCaseId() + " " + event.status());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(LegalCaseUpdatedEvent event) {
    log.info(
        "LegalCaseUpdatedEvent: "
            + "\n"
            + event.legalCase().firstname()
            + " "
            + event.legalCase().lastname());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(LegalCaseReadyEvent event) {
    log.info("LegalCaseReadyEvent: " + "\n" + event.legalCaseId());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(NotebookUpdatedEvent event) {
    log.info("📓 NotebookUpdatedEvent: " + "\n" + event.notebook());
    this.eventService.acknowledge(event);
  }

  // sourcefiles handler

  @EventListener
  public void handle(SourceFileCreatedEvent event) {
    log.info("SourceFileCreatedEvent: " + "\n" + event.sourceFile().sourceFileId());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(SourceFileUpdatedEvent event) {
    log.info("SourceFileUpdatedEvent: " + "\n" + event.sourceFile().folder());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(SourceFileTaskFailedEvent event) {
    log.info("SourceFileTaskFailedEvent: " + "\n" + event.sourceFileId());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(ExportCreatedEvent event) {
    log.info("🍻  ExportCreatedEvent: " + event.export().exportId());
    log.info("    Recipient : " + event.export().recipient());
    log.info("    Case Id   : " + event.export().legalCaseId());
    log.info("    Timestamp : " + event.ts());

    try (InputStream is = this.fileService.downloadFile(event.export().file().uri())) {
      Files.copy(
          is,
          Path.of("./" + event.export().file().filename()),
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
    log.info("⤵️  Downloaded file: {}", event.export().file().filename());

    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(ExportSharedEvent event) {
    log.info(
        "✉️ ExportSharedEvent: "
            + event.export().exportId()
            + "\n"
            + event.method()
            + "\n"
            + event.export().file().uri()
            + "\n"
            + event.email());
    this.eventService.acknowledge(event);
  }

  @EventListener
  public void handle(ExportViewedEvent event) {
    log.info(
        "📖 ExportViewedEvent: "
            + "\n"
            + event.export().legalCaseId()
            + " "
            + event.user().remoteAddr());
    this.eventService.acknowledge(event);
  }
}
