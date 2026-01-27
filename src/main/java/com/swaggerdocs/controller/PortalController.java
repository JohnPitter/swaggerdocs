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
                      Model model) {
        return swaggerService.getApp(appName)
                .map(info -> {
                    model.addAttribute("appName", appName);
                    model.addAttribute("info", info);
                    model.addAttribute("view", view);
                    return "docs";
                })
                .orElse("redirect:/");
    }
}
