package ch.legali.agent.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "legali.example")
public class ExampleConfig {

  private int runs;

  private String filesPath;

  public int getRuns() {
    return this.runs;
  }

  public void setRuns(int runs) {
    this.runs = runs;
  }

  public String getFilesPath() {
    return this.filesPath;
  }

  public void setFilesPath(String filesPath) {
    this.filesPath = filesPath;
  }
}
