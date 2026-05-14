package com.lingxi.promptmanager;

import com.lingxi.promptmanager.dto.PromptRequest;
import com.lingxi.promptmanager.repository.PromptRepository;
import com.lingxi.promptmanager.service.CategoryService;
import com.lingxi.promptmanager.service.PromptService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final PromptRepository promptRepository;
    private final CategoryService categoryService;
    private final PromptService promptService;

    public DataInitializer(PromptRepository promptRepository, CategoryService categoryService, PromptService promptService) {
        this.promptRepository = promptRepository;
        this.categoryService = categoryService;
        this.promptService = promptService;
    }

    @Override
    public void run(String... args) {
        if (promptRepository.count() > 0) {
            return;
        }

        List.of("设计", "代码", "写作").forEach(categoryService::ensureCategory);
        promptService.create(new PromptRequest(
                "海报视觉方案",
                "请为一个校园 AI 分享会设计海报视觉方案，包含主视觉、配色、字体风格和版式建议。",
                "设计",
                List.of("海报", "视觉")
        ));
        promptService.create(new PromptRequest(
                "Spring Boot 接口生成",
                "请根据实体字段生成 Spring Boot REST API，包括 Controller、Service、Repository 和基础异常处理。",
                "代码",
                List.of("Java", "后端")
        ));
        promptService.create(new PromptRequest(
                "期末汇报润色",
                "请把下面的项目说明润色成适合课堂期末展示的讲稿，语言自然，结构清晰。",
                "写作",
                List.of("汇报", "润色")
        ));
    }
}
