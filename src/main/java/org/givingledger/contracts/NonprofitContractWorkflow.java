package org.givingledger.contracts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class NonprofitContractWorkflow {
    public enum ReceiptDecision { ISSUE, HOLD_BELOW_THRESHOLD }
    public enum ReminderDecision { SCHEDULE, SKIP_NO_CONSENT }

    public record ContractRequest(
            String contractId,
            String nonprofitName,
            String donorName,
            long donationCents,
            String campaignCode,
            boolean volunteerReminderConsent,
            String contractHtml) {
    }

    public record ContractResult(
            String contractId,
            String signedPdf,
            ReceiptDecision donorReceipt,
            ReminderDecision volunteerReminder,
            CampaignEntry campaignReporting) {
    }

    public record CampaignEntry(String campaignCode, long recognizedCents, Instant recordedAt) {
    }

    private final ContractConfig config;
    private final InfraiPdfClient pdfClient;
    private final PdfContractSigner signer;
    private final Clock clock;

    @Autowired
    public NonprofitContractWorkflow(ContractConfig config, InfraiPdfClient pdfClient, PdfContractSigner signer) {
        this(config, pdfClient, signer, Clock.systemUTC());
    }

    NonprofitContractWorkflow(ContractConfig config, InfraiPdfClient pdfClient,
                              PdfContractSigner signer, Clock clock) {
        this.config = config;
        this.pdfClient = pdfClient;
        this.signer = signer;
        this.clock = clock;
    }

    public ContractResult execute(ContractRequest request, UUID idempotencyKey) throws Exception {
        byte[] unsigned = pdfClient.generateContract(request.contractHtml(), idempotencyKey);
        byte[] signed = signer.sign(unsigned, "Nonprofit contract " + request.contractId());
        Files.createDirectories(config.outputDirectory());
        Path destination = config.outputDirectory().resolve(request.contractId() + "-signed.pdf").normalize();
        if (!destination.startsWith(config.outputDirectory().normalize())) {
            throw new IllegalArgumentException("contractId must be a file-safe identifier");
        }
        Files.write(destination, signed);
        return decide(request, destination, clock.instant());
    }

    ContractResult decide(ContractRequest request, Path signedPdf, Instant now) {
        ReceiptDecision receipt = request.donationCents() >= config.receiptThresholdCents()
                ? ReceiptDecision.ISSUE : ReceiptDecision.HOLD_BELOW_THRESHOLD;
        ReminderDecision reminder = request.volunteerReminderConsent()
                ? ReminderDecision.SCHEDULE : ReminderDecision.SKIP_NO_CONSENT;
        CampaignEntry reporting = new CampaignEntry(request.campaignCode(), request.donationCents(), now);
        return new ContractResult(request.contractId(), signedPdf.toString(), receipt, reminder, reporting);
    }
}
