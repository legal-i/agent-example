package ch.legali.sdk.example.quarkus;

import ch.legali.sdk.exceptions.FileConflictException;
import ch.legali.sdk.exceptions.NotFoundException;
import ch.legali.sdk.models.AgentExportDTO;
import ch.legali.sdk.models.AgentFileDTO;
import ch.legali.sdk.models.AgentLegalCaseDTO;
import ch.legali.sdk.models.AgentSourceFileDTO;
import ch.legali.sdk.services.ExportService;
import ch.legali.sdk.services.FileService;
import ch.legali.sdk.services.LegalCaseService;
import ch.legali.sdk.services.SourceFileService;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExampleService {

  @Inject Vertx vertx;

  @Inject LegalCaseService legalCaseService;

  @Inject SourceFileService sourceFileService;

  @Inject FileService fileService;

  @Inject ExportService exportService;

  private static final Logger log = LoggerFactory.getLogger(ExampleService.class);

  @ConsumeEvent(value = ExampleEventService.BUS_STARTED)
  void start(Instant when) {
    log.info("received start event, let's go!");

    vertx.<String>executeBlocking(
        promise -> {
          runExample();
        });
  }

  /**
   * This is the dummy logic of the connector. You can download and check JavaDoc of each method and
   * entity of the SDK.
   */
  private void runExample() {
    // Create
    log.info("🗂  Adding LegalCase");
    AgentLegalCaseDTO legalCase =
        AgentLegalCaseDTO.builder()
            .legalCaseId(UUID.randomUUID())
            .firstname("John")
            .lastname("Doe")
            .reference("123-456-789")
            // Pass the UserID from SSO
            .owner("DummyIamUser")
            // or pass the user's e-mail
            // .ownerEmail("dummy@user.com")
            .accessGroup("group1")
            .putMetadata("meta.dummy", "dummy value")
            .build();
    this.legalCaseService.create(legalCase);

    // update legal case
    log.info("🤓  Updating LegalCase");
    AgentLegalCaseDTO legalCaseResponse = this.legalCaseService.get(legalCase.legalCaseId());
    AgentLegalCaseDTO nameChanged =
        AgentLegalCaseDTO.builder()
            .from(legalCaseResponse)
            .firstname("Jane")
            .reference("John changed his name")
            .build();
    this.legalCaseService.update(nameChanged);

    /*
     * To keep a constant memory footprint on the agent, the SDK uses a FileObject and
     * not a ByteArrayResource. PDF files can be large if they contain images (>
     * 500MB), in multi-threaded mode this leads to unwanted spikes in memory usage.
     * Ideally the files are chunked downloaded to a temporary file and then passed to
     * the SDK.
     */

    // add / delete a sourcefile
    AgentSourceFileDTO sourceFile =
        AgentSourceFileDTO.builder()
            .sourceFileId(UUID.randomUUID())
            .legalCaseId(legalCase.legalCaseId())
            .folder(chooseFolder())
            .fileReference("hello.pdf")

            // To pass metadata properties, you can use strings...
            .putMetadata("legali.metadata.title", "Sample Document")
            .putMetadata("legali.metadata.doctype", this.chooseDocType())
            .putMetadata("legali.metadata.issuedate", "2012-12-12")

            // or using the enums keys
            .putMetadata(
                AgentSourceFileDTO.MetadataKeys.LEGALI_METADATA_RECEIPTDATE.key(), "2012-12-11")

            // for boolean value, pass "true" or "false" as strings
            .putMetadata(
                AgentSourceFileDTO.MetadataKeys.LEGALI_PIPELINE_SPLITTING_DISABLED.key(), "true")

            // pass a mapping key instead, this will look up the agent mappings stored in the db
            // if a matching key is found, the metadata is set accordingly
            .putMetadata("legali.mapping.key", "M1")

            // if a property is set to an empty string, it is ignored and the default is used
            .putMetadata("legali.metadata.some-property", "")
            .build();

    log.info("🧾  Creating SourceFile");
    try (InputStream is = getClass().getResourceAsStream("/sample.pdf")) {
      this.sourceFileService.create(sourceFile, is);
    } catch (IOException e) {
      log.error("🙅‍  Failed to create SourceFile", e);
    }

    log.info("😴  Waiting for SourceFile to be processed  (will timeout after 3 seconds!)");
    // NOTE: use with care, busy waiting and usually not required
    AgentSourceFileDTO.SourceFileStatus status =
        this.sourceFileService.waitForSourceFileReadyOrTimeout(
            sourceFile.sourceFileId(), TimeUnit.SECONDS.toSeconds(3));

    // NOTE: will always time out, if processing is disabled
    if (status.equals(AgentSourceFileDTO.SourceFileStatus.ERROR)
        || status.equals(AgentSourceFileDTO.SourceFileStatus.TIMEOUT)) {
      log.warn("💥 legal-i was not fast enough to process this file {}", sourceFile.sourceFileId());
    }

    // Try to create same sourcefile with another file
    try {
      try (InputStream file2 = getClass().getResourceAsStream("/sample2.pdf")) {
        this.sourceFileService.create(sourceFile, file2);
      } catch (IOException e) {
        log.error("🙅‍  Failed to open sample2.pdf file", e);
      }
    } catch (FileConflictException fileConflictException) {
      log.info("🙅‍  Sourcefile file are different, refused to do something!‍️");
    }
    log.info("🧾  Creating the same SourceFile AGAIN (creates are idempotent)");
    try (InputStream is = getClass().getResourceAsStream("/sample.pdf")) {
      this.sourceFileService.create(sourceFile, is);
    } catch (IOException e) {
      log.error("🙅‍  Failed to create SourceFile", e);
    }

    List<AgentSourceFileDTO> list = this.sourceFileService.getByLegalCase(legalCase.legalCaseId());
    log.info("1️⃣ LegalCase has {} source files", list.size());

    // download file again and verify md5
    AgentFileDTO downloadedFile = list.get(0).originalFile();
    try (InputStream is = this.fileService.downloadFile(downloadedFile.uri())) {
      Path target = Path.of("./" + downloadedFile.filename());
      Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);

      MessageDigest md = MessageDigest.getInstance("MD5");
      String md5 = Base64.getEncoder().encodeToString(md.digest(Files.readAllBytes(target)));
      log.info(
          "🧮 MD5 of downloaded file is {}",
          md5.equals(downloadedFile.md5()) ? "correct" : "DIFFERENT!");

      Files.delete(target);
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace();
    }

    List<AgentExportDTO> exportsList = this.exportService.list(legalCase.legalCaseId());
    log.info("1️⃣ LegalCase has {} exports", exportsList.size());

    UUID exportId = UUID.randomUUID();
    try {
      AgentExportDTO export = this.exportService.get(exportId);
      log.info("1️⃣ LegalCase has export with uuid {}", export.exportId());
    } catch (NotFoundException e) {
      log.info("1️⃣ LegalCase does not have export with uuid {}", exportId);
    }

    log.info("␡ Deleting SourceFile");
    this.sourceFileService.delete(sourceFile.sourceFileId());

    log.info("🗄  Archiving LegalCase");
    this.legalCaseService.archive(legalCaseResponse.legalCaseId());

    list = this.sourceFileService.getByLegalCase(legalCase.legalCaseId());
    log.info("😅  LegalCase has {} source files", list.size());

    log.info("🗑  Deleting LegalCase");
    this.legalCaseService.delete(legalCaseResponse.legalCaseId());

    try {
      this.legalCaseService.get(legalCase.legalCaseId());
    } catch (NotFoundException ignored) {
      log.info("🥳  LegalCase has successfully been deleted, well done!");
    }
  }

  public void cleanup() {
    List<AgentLegalCaseDTO> allCases = this.legalCaseService.list();
    for (AgentLegalCaseDTO currentLegalCase : allCases) {
      if ("example-agent".equals(currentLegalCase.metadata().getOrDefault("legali.uploader", ""))) {
        log.info("🧹 Cleaning up {}", currentLegalCase.legalCaseId());
        this.legalCaseService.delete(currentLegalCase.legalCaseId());
      }
    }
  }

  /**
   * @return String random doc type
   */
  private String chooseDocType() {
    return List.of("type_medical", "type_profession_ik_statement", "type_legal_disposal")
        .get((int) Math.floor(Math.random() * 3));
  }

  /**
   * @return String random folder
   */
  private String chooseFolder() {
    return List.of("accident", "liability", "iv-be").get((int) Math.floor(Math.random() * 3));
  }
}