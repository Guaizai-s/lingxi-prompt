package com.lingxi.promptmanager.dto;

import java.time.Instant;
import java.util.List;

public record LibraryExport(
        Instant exportedAt,
        List<String> categories,
        List<PromptResponse> prompts
) {
}
