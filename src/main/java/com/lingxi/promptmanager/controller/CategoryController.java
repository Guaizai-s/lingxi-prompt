package com.lingxi.promptmanager.controller;

import com.lingxi.promptmanager.dto.CategoryRequest;
import com.lingxi.promptmanager.service.CategoryService;
import com.lingxi.promptmanager.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService categoryService;
    private final PromptService promptService;

    public CategoryController(CategoryService categoryService, PromptService promptService) {
        this.categoryService = categoryService;
        this.promptService = promptService;
    }

    @GetMapping("/api/categories")
    public List<String> categories() {
        return categoryService.list();
    }

    @PostMapping("/api/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public String create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/api/categories/{name}")
    public String update(@PathVariable String name, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(name, request);
    }

    @DeleteMapping("/api/categories/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        categoryService.delete(name);
    }

    @GetMapping("/api/tags")
    public List<String> tags() {
        return promptService.listTags();
    }
}
