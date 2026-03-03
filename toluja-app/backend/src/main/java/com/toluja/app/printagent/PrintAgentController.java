package com.toluja.app.printagent;

import com.toluja.app.dto.PrintAgentDtos;
import com.toluja.app.tenant.Tenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/print-agent")
@RequiredArgsConstructor
public class PrintAgentController {

    private static final String PRINT_KEY_HEADER = "X-Print-Key";

    private final PrintAgentAuthService authService;
    private final PrintAgentJobService jobService;

    @GetMapping("/jobs/next")
    public ResponseEntity<PrintAgentDtos.NextJobResponse> nextJob(
            @RequestParam String deviceId,
            @RequestHeader(PRINT_KEY_HEADER) String printKey
    ) {
        Tenant tenant = authService.authenticate(printKey);
        PrintAgentDtos.NextJobResponse job = jobService.reserveNextJob(tenant.getTenantId(), deviceId);
        if (job == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(job);
    }

    @PostMapping("/jobs/{jobId}/ack")
    public PrintAgentDtos.AckResponse ack(
            @PathVariable String jobId,
            @Valid @RequestBody PrintAgentDtos.AckRequest request,
            @RequestHeader(PRINT_KEY_HEADER) String printKey
    ) {
        Tenant tenant = authService.authenticate(printKey);
        return jobService.ack(jobId, tenant.getTenantId(), request);
    }

    @PostMapping("/jobs")
    public PrintAgentDtos.NextJobResponse enqueue(
            @Valid @RequestBody PrintAgentDtos.NextJobResponse request,
            @RequestHeader(PRINT_KEY_HEADER) String printKey
    ) {
        Tenant tenant = authService.authenticate(printKey);
        if (!tenant.getTenantId().equals(request.tenantId())) {
            throw new ResponseStatusException(FORBIDDEN, "tenantId do job não corresponde à print key");
        }
        return jobService.enqueue(request);
    }
}
