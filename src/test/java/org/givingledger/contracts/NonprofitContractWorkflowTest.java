package org.givingledger.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class NonprofitContractWorkflowTest {
    @Test
    void holdsSmallReceipt_skipsReminder_withoutLosingCampaignReporting() {
        ContractConfig config = new ContractConfig(Path.of("out"), Path.of("cert.pem"), Path.of("key.pem"), 2500);
        Clock clock = Clock.fixed(Instant.parse("2026-08-31T08:00:00Z"), ZoneOffset.UTC);
        NonprofitContractWorkflow workflow = new NonprofitContractWorkflow(config, null, null, clock);
        var request = new NonprofitContractWorkflow.ContractRequest(
                "grant-104", "River Food Fund", "A. Donor", 2400,
                "AUTUMN-PANTRY", false, "<h1>Contract</h1>");

        var result = workflow.decide(request, Path.of("out/grant-104-signed.pdf"), clock.instant());

        assertThat(result.donorReceipt()).isEqualTo(NonprofitContractWorkflow.ReceiptDecision.HOLD_BELOW_THRESHOLD);
        assertThat(result.volunteerReminder()).isEqualTo(NonprofitContractWorkflow.ReminderDecision.SKIP_NO_CONSENT);
        assertThat(result.campaignReporting().recognizedCents()).isEqualTo(2400);
        assertThat(result.campaignReporting().campaignCode()).isEqualTo("AUTUMN-PANTRY");
    }
}
