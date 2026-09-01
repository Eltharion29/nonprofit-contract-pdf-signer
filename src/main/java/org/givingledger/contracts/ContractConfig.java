package org.givingledger.contracts;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("contracts")
public record ContractConfig(
        Path outputDirectory,
        Path certificatePath,
        Path privateKeyPath,
        long receiptThresholdCents) {
}
