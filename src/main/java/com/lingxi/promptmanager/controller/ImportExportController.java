package com.lingxi.promptmanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.promptmanager.dto.LibraryExport;
import com.lingxi.promptmanager.dto.PromptRequest;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    public LibraryExport importJson(@RequestBody String json) {
        ImportedLibrary imported = parseImportText(json);
        categoryService.replaceAll(imported.categories());
        promptService.replaceAll(imported.prompts());
        return new LibraryExport(Instant.now(), categoryService.list(), promptService.listAll());
    }

    @PostMapping(value = "/api/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public LibraryExport importFile(@RequestParam("file") MultipartFile file) throws IOException {
        return importJson(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    private ImportedLibrary parseImportText(String source) {
        return parseImport(readFlexibleJson(source));
    }

    private JsonNode readFlexibleJson(String source) {
        String text = source == null ? "" : source.replace("\uFEFF", "").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("导入文件为空");
        }
        if (looksLikeObjectBody(text)) {
            try {
                return objectMapper.readTree("{" + text);
            } catch (IOException ignored) {
                try {
                    return objectMapper.readTree("{" + text + "}");
                } catch (IOException ignoredAgain) {
                    // Fall through to normal parsing and the user-facing error below.
                }
            }
        }
        try {
            return objectMapper.readTree(text);
        } catch (IOException firstError) {
            throw new IllegalArgumentException("JSON 格式不正确，请检查文件是否完整");
        }
    }

    private static boolean looksLikeObjectBody(String text) {
        return text.startsWith("\"") && text.contains(":");
    }

    private ImportedLibrary parseImport(JsonNode json) {
        List<PromptRequest> prompts = readPrompts(json);
        if (prompts.isEmpty()) {
            throw new IllegalArgumentException("导入文件中没有可用的提示词数据");
        }

        Set<String> categories = new LinkedHashSet<>(readTextList(findField(json, "categories", "categoryList", "folders")));
        prompts.stream()
                .map(PromptRequest::category)
                .filter(category -> category != null && !category.isBlank())
                .forEach(categories::add);

        return new ImportedLibrary(new ArrayList<>(categories), prompts);
    }

    private List<PromptRequest> readPrompts(JsonNode node) {
        if (node.isArray()) {
            return readPromptArray(node);
        }

        JsonNode promptsNode = findField(node, "prompts", "promptList", "items", "records", "list", "data");
        if (!promptsNode.isMissingNode() && !promptsNode.isNull()) {
            if (promptsNode == node) {
                throw new IllegalArgumentException("导入文件结构不正确");
            }
            return readPrompts(promptsNode);
        }

        if (looksLikePrompt(node)) {
            return List.of(toPromptRequest(node, 1));
        }

        throw new IllegalArgumentException("导入文件缺少 prompts 数组");
    }

    private List<PromptRequest> readPromptArray(JsonNode promptsNode) {
        List<PromptRequest> prompts = new ArrayList<>();
        int index = 1;
        for (JsonNode item : promptsNode) {
            prompts.add(toPromptRequest(item, index++));
        }
        return prompts;
    }

    private PromptRequest toPromptRequest(JsonNode node, int index) {
        if (node.isTextual()) {
            String content = trimTo(node.asText(), 5000);
            return new PromptRequest(defaultTitle(content, index), content, null, List.of());
        }

        String title = textField(node, "title", "name", "promptTitle", "标题");
        String content = textField(node, "content", "prompt", "text", "body", "description", "提示词内容", "内容");

        if (isBlank(title) && !isBlank(content)) {
            title = defaultTitle(content, index);
        }
        if (isBlank(content) && !isBlank(title)) {
            content = title;
        }
        if (isBlank(title) || isBlank(content)) {
            throw new IllegalArgumentException("第 " + index + " 条提示词缺少标题或内容");
        }

        String category = textField(node, "category", "type", "group", "folder", "分类");
        List<String> tags = readTextList(findField(node, "tags", "tagList", "labels", "keywords", "标签"));
        return new PromptRequest(trimTo(title, 120), trimTo(content, 5000), trimTo(category, 80), tags);
    }

    private static boolean looksLikePrompt(JsonNode node) {
        return node.isObject() && (
                node.has("title") || node.has("name") || node.has("content") || node.has("prompt")
                        || node.has("text") || node.has("body") || node.has("category") || node.has("tags")
        );
    }

    private JsonNode findField(JsonNode node, String... names) {
        if (!node.isObject()) {
            return objectMapper.missingNode();
        }
        for (String name : names) {
            if (node.has(name)) {
                return node.get(name);
            }
        }
        return objectMapper.missingNode();
    }

    private String textField(JsonNode node, String... names) {
        JsonNode field = findField(node, names);
        if (field.isMissingNode() || field.isNull()) {
            return null;
        }
        return field.isValueNode() ? field.asText() : null;
    }

    private static List<String> readTextList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                String value = item.isValueNode() ? item.asText() : null;
                if (!isBlank(value)) {
                    values.add(trimTo(value, 40));
                }
            }
            return values;
        }
        if (node.isTextual()) {
            return List.of(node.asText().split("[,，#;；\\s]+")).stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(value -> trimTo(value, 40))
                    .toList();
        }
        return List.of();
    }

    private static String defaultTitle(String content, int index) {
        String value = trimTo(content, 30);
        return value.isBlank() ? "未命名提示词 " + index : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private record ImportedLibrary(
            List<String> categories,
            List<PromptRequest> prompts
    ) {
    }
}
