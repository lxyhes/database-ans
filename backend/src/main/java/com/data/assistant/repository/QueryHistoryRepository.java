package com.data.assistant.repository;

import com.data.assistant.model.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 查询历史仓库
 */
@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    /**
     * 查找所有查询并按创建时间倒序
     */
    List<QueryHistory> findAllByOrderByCreatedAtDesc();

    /**
     * 查找收藏的查询
     */
    List<QueryHistory> findByIsFavoriteTrueOrderByCreatedAtDesc();

    /**
     * 根据类型查找查询
     */
    List<QueryHistory> findByQueryTypeOrderByCreatedAtDesc(String queryType);

    /**
     * 根据标签查找查询
     */
    List<QueryHistory> findByTagsContainingOrderByCreatedAtDesc(String tag);
}