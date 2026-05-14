package com.lingxi.promptmanager.repository;

import com.lingxi.promptmanager.model.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    List<Prompt> findAllByOrderByUpdatedAtDesc();

    List<Prompt> findByCategoryIgnoreCase(String category);
}
