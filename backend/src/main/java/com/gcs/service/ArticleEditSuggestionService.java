package com.gcs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.ArticleEditSuggestion;

import java.util.Map;

/**
 * 文章修改建议服务接口
 */
public interface ArticleEditSuggestionService extends IService<ArticleEditSuggestion> {

    /**
     * 提交修改建议
     *
     * @param articleId 文章 ID
     * @param proposerId 建议者 ID
     * @param title 建议标题
     * @param content 建议内容（JSON）
     * @param changeSummary 修改摘要
     */
    void submitSuggestion(Long articleId, Long proposerId, String title, 
                         Map<String, Object> content, String changeSummary);

    /**
     * 获取建议列表（分页）
     *
     * @param articleId 文章 ID
     * @param status 状态（null 表示全部）
     * @param page 页码
     * @param limit 每页数量
     * @return 建议列表
     */
    IPage<ArticleEditSuggestion> getSuggestions(Long articleId, Integer status, 
                                                Integer page, Integer limit);

    /**
     * 获取建议详情
     *
     * @param suggestionId 建议 ID
     * @return 建议详情
     */
    ArticleEditSuggestion getSuggestionDetail(Long suggestionId);

    /**
     * 审核建议
     *
     * @param suggestionId 建议 ID
     * @param reviewerId 审核人 ID
     * @param approved 是否通过
     * @param reason 拒绝理由（可选）
     */
    void reviewSuggestion(Long suggestionId, Long reviewerId, boolean approved, String reason);

    /**
     * 统计待审核建议数量
     *
     * @param articleId 文章 ID
     * @return 待审核数量
     */
    Integer countPendingSuggestions(Long articleId);

    /**
     * 获取用户提出的建议列表（分页）
     *
     * @param proposerId 建议者 ID
     * @param status 状态（null 表示全部）
     * @param page 页码
     * @param limit 每页数量
     * @return 建议列表
     */
    IPage<ArticleEditSuggestion> getSuggestionsByProposer(Long proposerId, Integer status,
                                                          Integer page, Integer limit);

    /**
     * 获取用户的文章收到的建议列表（分页）
     *
     * @param authorId 文章作者 ID
     * @param status 状态（null 表示全部）
     * @param page 页码
     * @param limit 每页数量
     * @return 建议列表
     */
    IPage<ArticleEditSuggestion> getSuggestionsByAuthor(Long authorId, Integer status,
                                                        Integer page, Integer limit);
}
