package com.lingxi.promptmanager.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.promptmanager.dto.LibraryExport;
import com.lingxi.promptmanager.dto.PromptRequest;
import com.lingxi.promptmanager.dto.PromptResponse;
import com.lingxi.promptmanager.service.CategoryService;
import com.lingxi.promptmanager.service.PromptService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RestController
public class ImportExportController {

    private final PromptService promptService;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    public ImportExportController(PromptService promptService, CategoryService categoryService, ObjectMapper objectMapper) {
        this.promptService = promptService;
        this.categoryService = categoryService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/export")
    public ResponseEntity<LibraryExport> exportLibrary() {
        LibraryExport body = new LibraryExport(Instant.now(), categoryService.list(), promptService.listAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lingxi-prompts.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @PostMapping(value = "/api/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public LibraryExport importJson(@RequestBody JsonNode json) {
        LibraryExport imported = parseImport(json);
        categoryService.replaceAll(imported.categories());
        promptService.replaceAll(imported.prompts().stream()
                .map(prompt -> new PromptRequest(prompt.title(), prompt.content(), prompt.category(), prompt.tags()))
                .toList());
        return new LibraryExport(Instant.now(), categoryService.list(), promptService.listAll());
    }

    @PostMapping(value = "/api/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public LibraryExport importFile(@RequestParam("file") MultipartFile file) throws IOException {
        return importJson(objectMapper.readTree(file.getInputStream()));
    }

    private LibraryExport parseImport(JsonNode json) {
        if (json.isArray()) {
            List<PromptResponse> prompts = objectMapper.convertValue(json, new TypeReference<>() {
            });
            List<String> categories = prompts.stream()
                    .map(PromptResponse::category)
                    .filter(category -> category != null && !category.isBlank())
                    .distinct()
                    .toList();
            return new LibraryExport(Instant.now(), categories, prompts);
        }

        List<String> categories = objectMapper.convertValue(json.path("categories"), new TypeReference<>() {
        });
        List<PromptResponse> prompts = objectMapper.convertValue(json.path("prompts"), new TypeReference<>() {
        });
        return new LibraryExport(Instant.now(), categories, prompts);
    }
}
