package com.lingxi.promptmanager.service;

import com.lingxi.promptmanager.dto.PromptRequest;
import com.lingxi.promptmanager.dto.PromptResponse;
import com.lingxi.promptmanager.model.Prompt;
import com.lingxi.promptmanager.repository.PromptRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final CategoryService categoryService;

    public PromptService(PromptRepository promptRepository, CategoryService categoryService) {
        this.promptRepository = promptRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<PromptResponse> search(String keyword, String category, String tag) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);
        String normalizedTag = normalize(tag);

        return promptRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(prompt -> matchesKeyword(prompt, normalizedKeyword))
                .filter(prompt -> normalizedCategory == null || equalsIgnoreCase(prompt.getCategory(), normalizedCategory))
                .filter(prompt -> normalizedTag == null || prompt.getTags().stream().anyMatch(item -> equalsIgnoreCase(item, normalizedTag)))
                .map(PromptService::toResponse)
                .toList();
    }

    @Transactional
    public PromptResponse create(PromptRequest request) {
        Prompt prompt = new Prompt();
        apply(prompt, request);
        Prompt saved = promptRepository.save(prompt);
        categoryService.ensureCategory(saved.getCategory());
        return toResponse(saved);
    }

    @Transactional
    public PromptResponse update(Long id, PromptRequest request) {
        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("提示词不存在：" + id));
        apply(prompt, request);
        Prompt saved = promptRepository.save(prompt);
        categoryService.ensureCategory(saved.getCategory());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!promptRepository.existsById(id)) {
            throw new EntityNotFoundException("提示词不存在：" + id);
        }
        promptRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> listTags() {
        return promptRepository.findAll().stream()
                .flatMap(prompt -> prompt.getTags().stream())
                .filter(tag -> !tag.isBlank())
                .distinct()
                .sorted(Comparator.comparing(tag -> tag.toLowerCase(Locale.ROOT)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PromptResponse> listAll() {
        return promptRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(PromptService::toResponse)
                .toList();
    }

    @Transactional
    public void replaceAll(List<PromptRequest> prompts) {
        promptRepository.deleteAll();
        prompts.forEach(this::create);
    }

    @Transactional
    public void renameCategory(String oldName, String newName) {
        promptRepository.findByCategoryIgnoreCase(oldName).forEach(prompt -> prompt.setCategory(newName));
    }

    @Transactional
    public void clearCategory(String categoryName) {
        promptRepository.findByCategoryIgnoreCase(categoryName).forEach(prompt -> prompt.setCategory(null));
    }

    static PromptResponse toResponse(Prompt prompt) {
        return new PromptResponse(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getContent(),
                prompt.getCategory(),
                prompt.getTags().stream().toList(),
                prompt.getCreatedAt(),
                prompt.getUpdatedAt()
        );
    }

    private void apply(Prompt prompt, PromptRequest request) {
        prompt.setTitle(request.title().trim());
        prompt.setContent(request.content().trim());
        prompt.setCategory(normalize(request.category()));
        prompt.setTags(normalizeTags(request.tags()));
    }

    private static Set<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new LinkedHashSet<>();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private static boolean matchesKeyword(Prompt prompt, String keyword) {
        if (keyword == null) {
            return true;
        }
        String title = prompt.getTitle() == null ? "" : prompt.getTitle().toLowerCase(Locale.ROOT);
        String content = prompt.getContent() == null ? "" : prompt.getContent().toLowerCase(Locale.ROOT);
        String needle = keyword.toLowerCase(Locale.ROOT);
        return title.contains(needle) || content.contains(needle);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
