package ch.legali.sdk.example;

import ch.legali.sdk.example.config.ExampleConfig;
import ch.legali.sdk.exceptions.FileConflictException;
import ch.legali.sdk.exceptions.NotFoundException;
import ch.legali.sdk.models.AgentExportDTO;
import ch.legali.sdk.models.AgentFileDTO;
import ch.legali.sdk.models.AgentLegalCaseDTO;
import ch.legali.sdk.models.AgentSourceFileDTO;
import ch.legali.sdk.models.AgentSourceFileDTO.MetadataKeys;
import ch.legali.sdk.models.AgentSourceFileDTO.SourceFileStatus;
import ch.legali.sdk.services.ExportService;
import ch.legali.sdk.services.FileService;
import ch.legali.sdk.services.LegalCaseService;
import ch.legali.sdk.services.SourceFileService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Note that the connector API is thread-safe. */
@Component
public class ExampleThread implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ExampleThread.class);

  private final LegalCaseService legalCaseService;
  private final SourceFileService sourceFileService;
  private final ExportService exportService;
  private final FileService fileService;
  private final ExampleConfig exampleConfig;

  public ExampleThread(
      LegalCaseService legalCaseService,
      SourceFileService sourceFileService,
      ExportService exportService,
      FileService fileService,
      ExampleConfig exampleConfig) {
    this.legalCaseService = legalCaseService;
    this.sourceFileService = sourceFileService;
    this.exportService = exportService;
    this.fileService = fileService;
    this.exampleConfig = exampleConfig;
  }

  @Override
  public void run() {
    int i = 0;
    while (i++ < this.exampleConfig.getIterations()) {
      log.info("🚀  Starting run {}", i);
      this.runExample();
    }
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
    Path fileToUpload = chooseLocalFile();

    // add / delete a sourcefile
    AgentSourceFileDTO sourceFile =
        AgentSourceFileDTO.builder()
            .sourceFileId(UUID.randomUUID())
            .legalCaseId(legalCase.legalCaseId())
            .folder(chooseFolder())
            .fileReference("hello.pdf")
            .putMetadata("hello", "world")
            // To pass metadata properties, you can use strings..
            .putMetadata("legali.metadata.title", "Sample Document")
            .putMetadata("legali.metadata.doctype", this.chooseDocType())
            // or the enums keys
            .putMetadata(MetadataKeys.LEGALI_METADATA_ISSUEDATE.key(), "2012-12-12")
            // for boolean value, pass "true" or "false" as strings
            .putMetadata(MetadataKeys.LEGALI_PIPELINE_SPLITTING_DISABLED.key(), "true")
            // if a property is set to an empty string, it is ignored and the default is used
            .putMetadata("legali.metadata.some-property", "")
            .build();

    log.info("🧾  Creating SourceFile");
    try (InputStream is = Files.newInputStream(fileToUpload)) {
      this.sourceFileService.create(sourceFile, is);
    } catch (IOException e) {
      log.error("🙅‍  Failed to create SourceFile", e);
    }

    log.info("😴  Waiting for SourceFile to be processed  (will timeout after 3 seconds!)");
    // NOTE: use with care, busy waiting and usually not required
    SourceFileStatus status =
        this.sourceFileService.waitForSourceFileReadyOrTimeout(
            sourceFile.sourceFileId(), TimeUnit.SECONDS.toSeconds(3));

    // NOTE: will always time out, if processing is disabled
    if (status.equals(SourceFileStatus.ERROR) || status.equals(SourceFileStatus.TIMEOUT)) {
      log.warn("💥 legal-i was not fast enough to process this file {}", sourceFile.sourceFileId());
    }

    // Try to create same sourcefile with another file
    try {
      ClassPathResource cp = new ClassPathResource("sample2.pdf");
      try (InputStream file2 = cp.getInputStream()) {
        this.sourceFileService.create(sourceFile, file2);
      } catch (IOException e) {
        log.error("🙅‍  Failed to open sample2.pdf file", e);
      }
    } catch (FileConflictException fileConflictException) {
      log.info("🙅‍  Sourcefile file are different, refused to do something!‍️");
    }
    log.info("🧾  Creating the same SourceFile AGAIN (creates are idempotent)");
    try (InputStream is = Files.newInputStream(fileToUpload)) {
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

    list = this.sourceFileService.getByLegalCase(legalCase.legalCaseId());
    log.info("😅  LegalCase has {} source files", list.size());

    log.info("🗄  Archiving LegalCase");
    this.legalCaseService.archive(legalCaseResponse.legalCaseId());

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
   * Returns either a random file from the given directory or the sample.pdf
   *
   * @return File
   */
  private Path chooseLocalFile() {
    // NOTE: if a directory has been specified, the connector loads a random file form
    // there
    try {
      if (this.exampleConfig.getFilesPath() != null
          && !this.exampleConfig.getFilesPath().isBlank()) {
        List<Path> files =
            Files.list(Paths.get(this.exampleConfig.getFilesPath())).collect(Collectors.toList());
        int randomIndex = (int) Math.floor(Math.random() * files.size());
        Path f = files.get(randomIndex);
        log.info(
            "Chosen file {}, {} MB",
            f.getFileName(),
            Math.round((double) Files.size(f) / (1024 * 1024)));
        return f;
      }

      // fall back to sample, if no or invalid path specified
      log.debug("Using sample.pdf");
      ClassPathResource cp = new ClassPathResource("sample.pdf");
      return cp.getFile().toPath();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
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
