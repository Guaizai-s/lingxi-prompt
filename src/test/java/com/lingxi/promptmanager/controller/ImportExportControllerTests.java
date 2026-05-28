package com.lingxi.promptmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.promptmanager.dto.PromptRequest;
import com.lingxi.promptmanager.service.CategoryService;
import com.lingxi.promptmanager.service.PromptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportExportControllerTests {

    private PromptService promptService;
    private CategoryService categoryService;
    private ImportExportController controller;

    @BeforeEach
    void setUp() {
        promptService = mock(PromptService.class);
        categoryService = mock(CategoryService.class);
        controller = new ImportExportController(promptService, categoryService, new ObjectMapper());
        when(categoryService.list()).thenReturn(List.of());
        when(promptService.listAll()).thenReturn(List.of());
    }

    @Test
    void importsJsonMissingOpeningBrace() {
        String json = """
                "exportedAt": "2026-05-20T12:00:00.000Z",
                "categories": ["设计"],
                "prompts": [
                  {"title": "UI 规范", "content": "生成一套 UI 设计规范", "category": "设计", "tags": ["UI", "规范"]}
                ]
                }
                """;

        controller.importJson(json);

        verify(categoryService).replaceAll(List.of("设计"));
        ArgumentCaptor<List<PromptRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(promptService).replaceAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().title()).isEqualTo("UI 规范");
        assertThat(captor.getValue().getFirst().tags()).containsExactly("UI", "规范");
    }

    @Test
    void importsPromptArrayAndDerivesCategories() {
        String json = """
                [
                  {"name": "代码助手", "prompt": "帮我生成 Controller", "type": "代码", "tags": "Java, Spring"}
                ]
                """;

        controller.importJson(json);

        verify(categoryService).replaceAll(List.of("代码"));
        ArgumentCaptor<List<PromptRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(promptService).replaceAll(captor.capture());
        assertThat(captor.getValue().getFirst().title()).isEqualTo("代码助手");
        assertThat(captor.getValue().getFirst().content()).isEqualTo("帮我生成 Controller");
        assertThat(captor.getValue().getFirst().tags()).containsExactly("Java", "Spring");
    }
}
