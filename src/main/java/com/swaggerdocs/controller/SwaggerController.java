package com.swaggerdocs.controller;

import com.swaggerdocs.model.SwaggerEntry;
import com.swaggerdocs.model.SwaggerInfo;
import com.swaggerdocs.model.SwaggerSubmission;
import com.swaggerdocs.model.ValidationResult;
import com.swaggerdocs.service.SwaggerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/swaggers")
@RequiredArgsConstructor
public class SwaggerController {

    private final SwaggerService swaggerService;

    @PostMapping
    public ResponseEntity<ValidationResult> submitSwagger(@Valid @RequestBody SwaggerSubmission submission) {
        log.info("Received swagger submission for app: {}", submission.getAppName());
        ValidationResult result = swaggerService.processSubmission(submission);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<SwaggerEntry>> listApps() {
        return ResponseEntity.ok(swaggerService.listApps());
    }

    @GetMapping("/{appName}")
    public ResponseEntity<SwaggerInfo> getApp(@PathVariable String appName) {
        return swaggerService.getApp(appName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{appName}/raw")
    public ResponseEntity<?> getRawSwagger(
            @PathVariable String appName,
            @RequestParam(required = false) String version) {
        if (version != null && !version.isEmpty()) {
            return swaggerService.getSwaggerAtVersion(appName, version)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return swaggerService.getApp(appName)
                .map(info -> ResponseEntity.ok(info.getSwagger()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{appName}/versions")
    public ResponseEntity<List<String>> getVersionHistory(@PathVariable String appName) {
        var versions = swaggerService.getVersionHistory(appName);
        if (versions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{appName}/diff")
    public ResponseEntity<?> compareVersions(
            @PathVariable String appName,
            @RequestParam String from,
            @RequestParam(defaultValue = "current") String to) {
        var changes = swaggerService.compareVersions(appName, from, to);
        return ResponseEntity.ok(changes);
    }
}
