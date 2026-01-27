package com.swaggerdocs.controller;

import com.swaggerdocs.model.SwaggerEntry;
import com.swaggerdocs.model.SwaggerInfo;
import com.swaggerdocs.model.SwaggerSubmission;
import com.swaggerdocs.model.ValidationResult;
import com.swaggerdocs.service.SwaggerService;
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
    public ResponseEntity<ValidationResult> submitSwagger(@RequestBody SwaggerSubmission submission) {
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
    public ResponseEntity<?> getRawSwagger(@PathVariable String appName) {
        return swaggerService.getApp(appName)
                .map(info -> ResponseEntity.ok(info.getSwagger()))
                .orElse(ResponseEntity.notFound().build());
    }
}
