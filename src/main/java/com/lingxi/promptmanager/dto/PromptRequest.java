package com.lingxi.promptmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PromptRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 5000) String content,
        @Size(max = 80) String category,
        List<@Size(max = 40) String> tags
) {
}
