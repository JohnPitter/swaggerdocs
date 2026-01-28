package com.swaggerdocs.controller;

import com.swaggerdocs.service.SwaggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PortalController {

    private final SwaggerService swaggerService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("apps", swaggerService.listApps());
        return "index";
    }

    @GetMapping("/docs/{appName}")
    public String docs(@PathVariable String appName,
                      @RequestParam(defaultValue = "swagger-ui") String view,
                      @RequestParam(required = false) String version,
                      Model model) {
        return swaggerService.getApp(appName)
                .map(info -> {
                    model.addAttribute("appName", appName);
                    model.addAttribute("info", info);
                    model.addAttribute("view", view);
                    model.addAttribute("selectedVersion", version);
                    return "docs";
                })
                .orElse("redirect:/");
    }

    @GetMapping("/docs/{appName}/history")
    public String history(@PathVariable String appName, Model model) {
        return swaggerService.getApp(appName)
                .map(info -> {
                    model.addAttribute("appName", appName);
                    model.addAttribute("info", info);
                    model.addAttribute("versions", swaggerService.getVersionHistory(appName));
                    return "history";
                })
                .orElse("redirect:/");
    }

    @GetMapping("/docs/{appName}/diff")
    public String diff(@PathVariable String appName,
                       @RequestParam String from,
                       @RequestParam(defaultValue = "current") String to,
                       Model model) {
        var changes = swaggerService.compareVersions(appName, from, to);
        model.addAttribute("appName", appName);
        model.addAttribute("fromVersion", from);
        model.addAttribute("toVersion", to);
        model.addAttribute("changes", changes);
        return "diff";
    }
}
