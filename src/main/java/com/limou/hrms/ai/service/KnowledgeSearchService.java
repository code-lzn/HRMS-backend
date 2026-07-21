package com.limou.hrms.ai.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limou.hrms.ai.mapper.AiKnowledgeDocMapper;
import com.limou.hrms.ai.model.entity.AiKnowledgeDoc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库检索服务
 * 当前基于 MySQL LIKE 全文匹配，后续可升级为 Elasticsearch 向量检索
 */
@Service
@Slf4j
public class KnowledgeSearchService {

    @Resource
    private AiKnowledgeDocMapper knowledgeDocMapper;

    /**
     * 检索知识库，返回最相关的文档片段
     *
     * @param query 用户查询
     * @param topK  返回 Top-K 条结果
     * @return 知识库文档列表（按相关度排序）
     */
    public List<AiKnowledgeDoc> search(String query, int topK) {
        if (StrUtil.isBlank(query)) {
            return new ArrayList<>();
        }

        // 分词：按常见分隔符拆分 query
        List<String> keywords = splitKeywords(query);
        if (keywords.isEmpty()) {
            keywords.add(query);
        }

        // 用第一个关键词做 LIKE 查询（后续可升级为 ES match）
        LambdaQueryWrapper<AiKnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeDoc::getStatus, 1);
        // 拼接 OR LIKE 条件
        wrapper.and(w -> {
            for (String kw : keywords) {
                w.or().like(AiKnowledgeDoc::getTitle, kw);
                w.or().like(AiKnowledgeDoc::getContent, kw);
            }
        });
        wrapper.last("LIMIT " + (topK * 3)); // 多取一些做二次排序

        List<AiKnowledgeDoc> candidates = knowledgeDocMapper.selectList(wrapper);
        if (candidates.isEmpty()) {
            return candidates;
        }

        // 二次排序：按关键词命中次数排序
        return candidates.stream()
                .sorted(Comparator.comparingInt((AiKnowledgeDoc doc) -> countKeywordHits(doc, keywords)).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 按分类检索知识库
     */
    public List<AiKnowledgeDoc> searchByCategory(String category, int topK) {
        LambdaQueryWrapper<AiKnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeDoc::getStatus, 1)
                .eq(AiKnowledgeDoc::getCategory, category)
                .last("LIMIT " + topK);
        return knowledgeDocMapper.selectList(wrapper);
    }

    /**
     * 获取所有启用的知识库文档（用于构建 Prompt 前缀）
     */
    public List<AiKnowledgeDoc> getAllEnabled() {
        LambdaQueryWrapper<AiKnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeDoc::getStatus, 1);
        return knowledgeDocMapper.selectList(wrapper);
    }

    // ========== 私有方法 ==========

    /**
     * 拆分查询关键词
     */
    private List<String> splitKeywords(String query) {
        // 按空格、标点符号拆分，过滤掉停用词
        return Arrays.stream(query.split("[\\s，,。.!！?？、；;：:]+"))
                .filter(s -> s.length() >= 2)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 统计文档中关键词命中次数
     */
    private int countKeywordHits(AiKnowledgeDoc doc, List<String> keywords) {
        int hits = 0;
        String title = doc.getTitle() != null ? doc.getTitle() : "";
        String content = doc.getContent() != null ? doc.getContent() : "";

        for (String kw : keywords) {
            if (title.contains(kw)) {
                hits += 3; // 标题命中权重更高
            }
            if (content.contains(kw)) {
                hits += 1;
            }
        }
        return hits;
    }
}
