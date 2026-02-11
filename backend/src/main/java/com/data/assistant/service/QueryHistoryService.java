package com.data.assistant.service;

import com.data.assistant.model.QueryHistory;
import com.data.assistant.repository.QueryHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 查询历史服务
 */
@Service
public class QueryHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(QueryHistoryService.class);

    private final QueryHistoryRepository queryHistoryRepository;

    @Autowired
    public QueryHistoryService(QueryHistoryRepository queryHistoryRepository) {
        this.queryHistoryRepository = queryHistoryRepository;
    }

    /**
     * 保存查询记录
     */
    public QueryHistory saveQuery(String queryText, String sqlQuery, String queryType, String tags) {
        QueryHistory history = new QueryHistory();
        history.setQueryText(queryText);
        history.setSqlQuery(sqlQuery);
        history.setQueryType(queryType);
        history.setTags(tags);
        history.setIsFavorite(false);
        history.setCreatedAt(LocalDateTime.now());
        return queryHistoryRepository.save(history);
    }

    /**
     * 获取所有查询历史
     */
    public List<QueryHistory> getAllQueries() {
        return queryHistoryRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 获取收藏的查询
     */
    public List<QueryHistory> getFavoriteQueries() {
        return queryHistoryRepository.findByIsFavoriteTrueOrderByCreatedAtDesc();
    }

    /**
     * 切换收藏状态
     */
    public QueryHistory toggleFavorite(Long id) {
        Optional<QueryHistory> optional = queryHistoryRepository.findById(id);
        if (optional.isPresent()) {
            QueryHistory history = optional.get();
            history.setIsFavorite(!history.getIsFavorite());
            return queryHistoryRepository.save(history);
        }
        return null;
    }

    /**
     * 删除查询记录
     */
    public void deleteQuery(Long id) {
        queryHistoryRepository.deleteById(id);
    }

    /**
     * 根据类型查询
     */
    public List<QueryHistory> getQueriesByType(String queryType) {
        return queryHistoryRepository.findByQueryTypeOrderByCreatedAtDesc(queryType);
    }

    /**
     * 根据标签查询
     */
    public List<QueryHistory> getQueriesByTag(String tag) {
        return queryHistoryRepository.findByTagsContainingOrderByCreatedAtDesc(tag);
    }
}