package ch.legali.agent.example;

import ch.legali.agent.sdk.exceptions.AlreadyExistsException;
import ch.legali.agent.sdk.exceptions.NotFoundException;
import ch.legali.agent.sdk.models.ImmutableLegalCaseDTO;
import ch.legali.agent.sdk.models.ImmutableSourceFileDTO;
import ch.legali.agent.sdk.models.LegalCaseDTO;
import ch.legali.agent.sdk.models.SourceFileDTO;
import ch.legali.agent.sdk.services.LegalCaseService;
import ch.legali.agent.sdk.services.SourceFileService;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ExampleConnector implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ExampleConnector.class);

  private final LegalCaseService legalCaseService;
  private final SourceFileService sourceFileService;

  public ExampleConnector(LegalCaseService legalCaseService, SourceFileService sourceFileService) {
    this.legalCaseService = legalCaseService;
    this.sourceFileService = sourceFileService;
  }

  @Override
  public void run() {
    // Create
    log.info("🗂 Adding LegalCase");
    LegalCaseDTO legalCase =
        ImmutableLegalCaseDTO.builder()
            .legalCaseUUID(UUID.randomUUID())
            .firstname("John")
            .lastname("Doe")
            .reference("123-456-789")
            .owner("DummyIamUser")
            .addGroups("group1", "group2")
            .putMetadata("meta.dummy", "dummy value")
            .build();
    this.legalCaseService.create(legalCase);

    // provoke exception
    try {
      this.legalCaseService.create(legalCase);
    } catch (AlreadyExistsException alreadyExistsException) {
      log.info("🙅‍ Already exists, refused to do it again!‍️");
    }

    // update legal case
    log.info("🤓 Updating LegalCase");
    LegalCaseDTO legalCaseResponse = this.legalCaseService.get(legalCase.getLegalCaseUUID());
    LegalCaseDTO nameChanged =
        ImmutableLegalCaseDTO.copyOf(legalCaseResponse)
            .withFirstname("Jane")
            .withReference("John is a girl now");
    this.legalCaseService.update(nameChanged);

    ClassPathResource cp = new ClassPathResource("sample.pdf");
    File file = null;
    try {
      file = cp.getFile();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    // add / delete a sourcefile
    SourceFileDTO sourceFile =
        ImmutableSourceFileDTO.builder()
            .sourceFileUUID(UUID.randomUUID())
            .legalCaseUUID(legalCase.getLegalCaseUUID())
            .reference("hello.pdf")
            .putMetadata("hello", "world")
            .build();

    log.info("🧾 Creating SourceFile");
    this.sourceFileService.create(sourceFile, file);

    log.info("😴 Waiting for SourceFile to be processed");
    SourceFileDTO.Status status =
        this.sourceFileService.waitForSourceFileReadyOrTimeout(
            sourceFile.getSourceFileUUID(), TimeUnit.SECONDS.toSeconds(30));
    if (status.equals(SourceFileDTO.Status.ERROR) || status.equals(SourceFileDTO.Status.TIMEOUT)) {
      log.error(
          "💥 AI (aka Achim intelligence) was not fast enough to segment this case... {}",
          sourceFile.getSourceFileUUID());
    }

    List<SourceFileDTO> list = this.sourceFileService.getByLegalCase(legalCase.getLegalCaseUUID());
    log.info("1️⃣ LegalCase has {} source files", list.size());

    log.info("␡ Deleting SourceFile");
    this.sourceFileService.delete(sourceFile.getSourceFileUUID());

    list = this.sourceFileService.getByLegalCase(legalCase.getLegalCaseUUID());
    log.info("😅 LegalCase has {} source files", list.size());

    log.info("🗑 Deleting LegalCase");
    this.legalCaseService.delete(legalCaseResponse.getLegalCaseUUID());

    try {
      this.legalCaseService.get(legalCase.getLegalCaseUUID());
    } catch (NotFoundException ignored) {
      log.info("🥳 LegalCase has successfully been deleted, hurray!");
    }
  }
}
