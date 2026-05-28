package com.lingxi.promptmanager.repository;

import com.lingxi.promptmanager.model.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    List<Prompt> findAllByOrderByUpdatedAtDesc();

    List<Prompt> findByCategoryIgnoreCase(String category);

    @Query("""
            select distinct p from Prompt p
            left join p.tags tag
            where (:keyword is null
                or lower(p.title) like lower(concat('%', :keyword, '%'))
                or lower(p.content) like lower(concat('%', :keyword, '%')))
              and (:category is null or lower(p.category) = lower(:category))
              and (:tag is null or lower(tag) = lower(:tag))
            order by p.updatedAt desc
            """)
    List<Prompt> search(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("tag") String tag
    );
}
