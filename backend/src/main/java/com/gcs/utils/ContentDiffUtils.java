package com.gcs.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 内容差异计算工具类
 * 用于计算 TipTap JSON 内容的新增、修改、删除行数
 */
@Slf4j
public class ContentDiffUtils {

    /**
     * 计算两个内容之间的差异
     *
     * @param oldContent 旧内容（TipTap JSON）
     * @param newContent 新内容（TipTap JSON）
     * @return 差异结果 {added: 新增行数，modified: 修改行数，deleted: 删除行数}
     */
    public static Map<String, Integer> calculateDiff(Map<String, Object> oldContent, 
                                                      Map<String, Object> newContent) {
        // 1. 将 TipTap JSON 转换为纯文本
        String oldText = convertToJsonString(oldContent);
        String newText = convertToJsonString(newContent);

        // 2. 使用简化版 diff 算法
        return calculateSimpleDiff(oldText, newText);
    }

    /**
     * 计算两个字符串之间的差异（简化版）
     * 只统计新增和删除，不单独统计修改
     *
     * @param oldText 旧文本
     * @param newText 新文本
     * @return 差异结果 {added: 新增行数，modified: 0, deleted: 删除行数}
     */
    private static Map<String, Integer> calculateSimpleDiff(String oldText, String newText) {
        Map<String, Integer> result = new HashMap<>();
        
        // 按行分割
        List<String> oldLines = splitIntoLines(oldText);
        List<String> newLines = splitIntoLines(newText);

        // 使用 LCS（最长公共子序列）算法计算差异
        int[][] dp = new int[oldLines.size() + 1][newLines.size() + 1];

        // 构建 DP 表
        for (int i = 1; i <= oldLines.size(); i++) {
            for (int j = 1; j <= newLines.size(); j++) {
                if (oldLines.get(i - 1).equals(newLines.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // 回溯找出差异
        int added = 0;
        int deleted = 0;
        int i = oldLines.size();
        int j = newLines.size();

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines.get(i - 1).equals(newLines.get(j - 1))) {
                // 相同的行
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                // 新增的行
                added++;
                j--;
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                // 删除的行
                deleted++;
                i--;
            }
        }

        result.put("added", added);
        result.put("modified", 0); // 简化版不统计修改
        result.put("deleted", deleted);

        log.debug("差异计算完成：added={}, modified={}, deleted={}", added, 0, deleted);
        
        return result;
    }

    /**
     * 将 TipTap JSON 转换为纯文本
     *
     * @param content TipTap JSON 内容
     * @return 纯文本（保留换行）
     */
    private static String convertToJsonString(Map<String, Object> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        convertNodeToText(content, text, 0);
        return text.toString();
    }

    /**
     * 递归将 TipTap 节点转换为文本
     *
     * @param node TipTap 节点
     * @param text 文本构建器
     * @param depth 当前深度
     */
    private static void convertNodeToText(Map<String, Object> node, 
                                          StringBuilder text, 
                                          int depth) {
        if (node == null) {
            return;
        }

        String type = (String) node.get("type");
        
        // 如果是文本节点，直接添加内容
        if ("text".equals(type)) {
            text.append(node.getOrDefault("text", ""));
            return;
        }

        // 处理段落节点，添加换行
        if ("paragraph".equals(type)) {
            List<Map<String, Object>> children = getChildNodes(node);
            for (Map<String, Object> child : children) {
                convertNodeToText(child, text, depth + 1);
            }
            text.append("\n");
            return;
        }

        // 处理标题节点
        if ("heading".equals(type)) {
            List<Map<String, Object>> children = getChildNodes(node);
            for (Map<String, Object> child : children) {
                convertNodeToText(child, text, depth + 1);
            }
            text.append("\n");
            return;
        }

        // 处理列表项
        if ("listItem".equals(type) || "taskItem".equals(type)) {
            List<Map<String, Object>> children = getChildNodes(node);
            for (Map<String, Object> child : children) {
                convertNodeToText(child, text, depth + 1);
            }
            return;
        }

        // 处理代码块
        if ("codeBlock".equals(type)) {
            List<Map<String, Object>> children = getChildNodes(node);
            for (Map<String, Object> child : children) {
                convertNodeToText(child, text, depth + 1);
            }
            text.append("\n");
            return;
        }

        // 处理引用块
        if ("blockquote".equals(type)) {
            List<Map<String, Object>> children = getChildNodes(node);
            for (Map<String, Object> child : children) {
                convertNodeToText(child, text, depth + 1);
            }
            return;
        }

        // 递归处理所有子节点
        List<Map<String, Object>> children = getChildNodes(node);
        for (Map<String, Object> child : children) {
            convertNodeToText(child, text, depth + 1);
        }
    }

    /**
     * 获取节点的子节点列表
     *
     * @param node TipTap 节点
     * @return 子节点列表
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getChildNodes(Map<String, Object> node) {
        Object content = node.get("content");
        if (content instanceof List) {
            return (List<Map<String, Object>>) content;
        }
        return new ArrayList<>();
    }

    /**
     * 将文本按行分割
     *
     * @param text 文本
     * @return 行列表
     */
    private static List<String> splitIntoLines(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 按换行符分割，并过滤空行
        String[] lines = text.split("\n");
        List<String> result = new ArrayList<>();
        
        for (String line : lines) {
            String trimmed = line.trim();
            // 可以选择是否过滤空行
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        
        return result;
    }

    /**
     * 计算贡献分数
     *
     * @param added 新增行数
     * @param modified 修改行数
     * @param deleted 删除行数
     * @return 贡献分数
     */
    public static double calculateScore(int added, int modified, int deleted) {
        return added + modified * 0.5 + deleted * 0.1;
    }

    /**
     * 格式化贡献分数
     *
     * @param score 贡献分数
     * @return 格式化后的分数（保留 2 位小数）
     */
    public static String formatScore(double score) {
        return String.format("%.2f", score);
    }
}
