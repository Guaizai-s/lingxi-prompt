package com.lingxi.promptmanager.dto;

import java.time.Instant;
import java.util.List;

public record PromptResponse(
        Long id,
        String title,
        String content,
        String category,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
