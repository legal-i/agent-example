package ch.legali.agent.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "legali.example")
public class ExampleConfig {

  /** Example Connector iterations (per thread) */
  private int iterations;

  /** Optional path for sample pdf files. If blank the sample.pdf form resources will be used. */
  private String filesPath;

  public int getIterations() {
    return this.iterations;
  }

  public void setIterations(int iterations) {
    this.iterations = iterations;
  }

  public String getFilesPath() {
    return this.filesPath;
  }

  public void setFilesPath(String filesPath) {
    this.filesPath = filesPath;
  }
}
