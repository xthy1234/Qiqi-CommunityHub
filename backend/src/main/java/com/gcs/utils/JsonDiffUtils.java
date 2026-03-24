package com.gcs.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * JSON 工具类
 */
@Slf4j
public class JsonDiffUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从 JSON 中提取所有文本内容（用于展示摘要等场景）
     *
     * @param json JSON 对象
     * @return 提取的文本
     */
    public static String extractText(Map<String, Object> json) {
        if (json == null || json.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        extractTextRecursive(json, text);
        return text.toString().trim();
    }

    /**
     * 递归提取 JSON 中的文本节点
     */
    private static void extractTextRecursive(Object node, StringBuilder text) {
        if (node instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) node;
            for (Object value : map.values()) {
                extractTextRecursive(value, text);
            }
        } else if (node instanceof Iterable) {
            for (Object item : (Iterable<?>) node) {
                extractTextRecursive(item, text);
            }
        } else if (node instanceof String) {
            String str = ((String) node).trim();
            if (!str.isEmpty()) {
                text.append(str).append(" ");
            }
        }
    }
}
