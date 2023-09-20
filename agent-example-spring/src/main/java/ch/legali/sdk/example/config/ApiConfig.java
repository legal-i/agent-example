package ch.legali.sdk.example.config;

import ch.legali.sdk.SdkConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "legali")
public class ApiConfig extends SdkConfig {}
