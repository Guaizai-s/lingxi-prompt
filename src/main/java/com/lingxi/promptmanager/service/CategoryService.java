package com.lingxi.promptmanager.service;

import com.lingxi.promptmanager.dto.CategoryRequest;
import com.lingxi.promptmanager.model.Category;
import com.lingxi.promptmanager.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PromptService promptService;

    public CategoryService(CategoryRepository categoryRepository, @Lazy PromptService promptService) {
        this.categoryRepository = categoryRepository;
        this.promptService = promptService;
    }

    @Transactional(readOnly = true)
    public List<String> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(Category::getName)
                .toList();
    }

    @Transactional
    public String create(CategoryRequest request) {
        String name = normalizeRequired(request.name());
        ensureNotExists(name);
        return categoryRepository.save(new Category(name)).getName();
    }

    @Transactional
    public String update(String oldName, CategoryRequest request) {
        String nextName = normalizeRequired(request.name());
        Category category = categoryRepository.findByNameIgnoreCase(oldName)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在：" + oldName));
        if (!category.getName().equalsIgnoreCase(nextName)) {
            ensureNotExists(nextName);
        }
        String previousName = category.getName();
        category.setName(nextName);
        promptService.renameCategory(previousName, nextName);
        return category.getName();
    }

    @Transactional
    public void delete(String name) {
        Category category = categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在：" + name));
        promptService.clearCategory(category.getName());
        categoryRepository.delete(category);
    }

    @Transactional
    public void ensureCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return;
        }
        if (!categoryRepository.existsByNameIgnoreCase(categoryName)) {
            categoryRepository.save(new Category(categoryName.trim()));
        }
    }

    @Transactional
    public void replaceAll(List<String> categories) {
        categoryRepository.deleteAll();
        categories.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .forEach(name -> categoryRepository.save(new Category(name)));
    }

    private void ensureNotExists(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new EntityExistsException("分类已存在：" + name);
        }
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        return value.trim();
    }
}
