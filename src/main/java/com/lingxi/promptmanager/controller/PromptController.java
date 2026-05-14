package com.lingxi.promptmanager.controller;

import com.lingxi.promptmanager.dto.PromptRequest;
import com.lingxi.promptmanager.dto.PromptResponse;
import com.lingxi.promptmanager.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public List<PromptResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag
    ) {
        return promptService.search(keyword, category, tag);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromptResponse create(@Valid @RequestBody PromptRequest request) {
        return promptService.create(request);
    }

    @PutMapping("/{id}")
    public PromptResponse update(@PathVariable Long id, @Valid @RequestBody PromptRequest request) {
        return promptService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        promptService.delete(id);
    }
}
