package org.givingledger.contracts;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class ContractSigningController {
    private final NonprofitContractWorkflow workflow;

    public ContractSigningController(NonprofitContractWorkflow workflow) {
        this.workflow = workflow;
    }

    @PostMapping("/contracts/sign")
    public NonprofitContractWorkflow.ContractResult sign(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody NonprofitContractWorkflow.ContractRequest request) throws Exception {
        return workflow.execute(request, idempotencyKey);
    }

    @ExceptionHandler(InfraiException.class)
    public ResponseEntity<Map<String, Object>> rejected(InfraiException error) {
        int clientStatus = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(clientStatus).body(Map.of(
                "code", error.code(),
                "message", error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", error.getMessage()));
    }
}
