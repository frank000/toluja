package com.toluja.app.tenant;

import com.toluja.app.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracao/tenant/print-agent")
@RequiredArgsConstructor
public class TenantAgentPackageController {

    private final TenantAgentPackageService service;

    @GetMapping("/windows")
    public ResponseEntity<ByteArrayResource> downloadWindows(Authentication authentication,
                                                             HttpServletRequest request) {
        return buildResponse(service.generateWindowsPackage(
                AuthContext.tenantId(authentication),
                requestBaseUrl(request)
        ));
    }

    @GetMapping("/linux")
    public ResponseEntity<ByteArrayResource> downloadLinux(Authentication authentication,
                                                           HttpServletRequest request) {
        return buildResponse(service.generateLinuxPackage(
                AuthContext.tenantId(authentication),
                requestBaseUrl(request)
        ));
    }

    private ResponseEntity<ByteArrayResource> buildResponse(TenantAgentPackageService.GeneratedPackage generatedPackage) {
        ByteArrayResource resource = new ByteArrayResource(generatedPackage.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(generatedPackage.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(generatedPackage.fileName())
                        .build()
                        .toString())
                .contentLength(generatedPackage.content().length)
                .body(resource);
    }

    private String requestBaseUrl(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder(request.getScheme())
                .append("://")
                .append(request.getServerName());
        int port = request.getServerPort();
        boolean defaultHttp = "http".equalsIgnoreCase(request.getScheme()) && port == 80;
        boolean defaultHttps = "https".equalsIgnoreCase(request.getScheme()) && port == 443;
        if (!defaultHttp && !defaultHttps) {
            builder.append(':').append(port);
        }
        return builder.toString();
    }
}
