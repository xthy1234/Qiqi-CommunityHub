package com.gcs.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Mybatis-Plus工具类
 * 提供查询条件构建的通用方法
 * @author 
 * @date 2026-04-16
 */
@Slf4j
public class MPUtil {
    public static final char UNDERLINE = '_';

    /**
     * 将Bean转换为下划线格式的Map（带前缀）
     *
     * @param bean 对象
     * @param pre 前缀
     * @return 转换后的Map
     */
    public static Map<String, Object> allEQMapPre(Object bean, String pre) {
        if (bean == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> map = BeanUtil.beanToMap(bean);
        return camelToUnderlineMap(map, pre);
    }

    /**
     * 将Bean转换为下划线格式的Map
     *
     * @param bean 对象
     * @return 转换后的Map
     */
    public static Map<String, Object> allEQMap(Object bean) {
        return allEQMapPre(bean, "");
    }

    /**
     * 构建全模糊查询条件（带前缀）
     *
     * @param wrapper 查询包装器
     * @param bean 对象
     * @param pre 前缀
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> allLikePre(QueryWrapper<T> wrapper, Object bean, String pre) {
        if (wrapper == null || bean == null) {
            return wrapper;
        }
        
        Map<String, Object> map = BeanUtil.beanToMap(bean);
        Map<String, Object> result = camelToUnderlineMap(map, pre);
        return genLike(wrapper, result);
    }

    /**
     * 构建全模糊查询条件
     *
     * @param wrapper 查询包装器
     * @param bean 对象
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> allLike(QueryWrapper<T> wrapper, Object bean) {
        return allLikePre(wrapper, bean, "");
    }

    /**
     * 生成模糊查询条件
     *
     * @param wrapper 查询包装器
     * @param param 参数Map
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> genLike(QueryWrapper<T> wrapper, Map<String, Object> param) {
        if (wrapper == null || param == null || param.isEmpty()) {
            return wrapper;
        }

        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null && StringUtils.isNotBlank(value.toString())) {
                wrapper.like(key, value.toString());
            }
        }
        return wrapper;
    }

    /**
     * 构建模糊或等于查询条件
     *
     * @param wrapper 查询包装器
     * @param bean 对象
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> likeOrEq(QueryWrapper<T> wrapper, Object bean) {
        if (wrapper == null || bean == null) {
            return wrapper;
        }
        
        Map<String, Object> result = BeanUtil.beanToMap(bean, true, true);
        return genLikeOrEq(wrapper, result);
    }

    /**
     * 生成模糊或等于查询条件
     *
     * @param wrapper 查询包装器
     * @param param 参数Map
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> genLikeOrEq(QueryWrapper<T> wrapper, Map<String, Object> param) {
        if (wrapper == null || param == null || param.isEmpty()) {
            return wrapper;
        }

        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null && StringUtils.isNotBlank(value.toString())) {
                String valueStr = value.toString();
                if (valueStr.contains("%")) {
                    wrapper.like(key, valueStr.replace("%", ""));
                } else {
                    wrapper.eq(key, value);
                }
            }
        }
        return wrapper;
    }

    /**
     * 构建等于查询条件
     *
     * @param wrapper 查询包装器
     * @param bean 对象
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> allEq(QueryWrapper<T> wrapper, Object bean) {
        if (wrapper == null || bean == null) {
            return wrapper;
        }
        
        Map<String, Object> result = BeanUtil.beanToMap(bean, true, true);
        return genEq(wrapper, result);
    }

    /**
     * 生成等于查询条件
     *
     * @param wrapper 查询包装器
     * @param param 参数Map
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> genEq(QueryWrapper<T> wrapper, Map<String, Object> param) {
        if (wrapper == null || param == null || param.isEmpty()) {
            return wrapper;
        }

        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                wrapper.eq(key, value);
            }
        }
        return wrapper;
    }

    /**
     * 构建范围查询条件
     *
     * @param wrapper 查询包装器
     * @param params 参数Map
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> between(QueryWrapper<T> wrapper, Map<String, Object> params) {
        if (wrapper == null || params == null || params.isEmpty()) {
            return wrapper;
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null || StringUtils.isBlank(value.toString())) {
                continue;
            }

            String columnName = "";
            if (key.endsWith("_start")) {
                columnName = key.substring(0, key.indexOf("_start"));
                wrapper.ge(columnName, value);
            } else if (key.endsWith("_end")) {
                columnName = key.substring(0, key.indexOf("_end"));
                wrapper.le(columnName, value);
            }
        }
        return wrapper;
    }

    /**
     * 构建排序条件
     *
     * @param wrapper 查询包装器
     * @param params 参数Map
     * @return 查询包装器
     */
    public static <T> QueryWrapper<T> sort(QueryWrapper<T> wrapper, Map<String, Object> params) {
        if (wrapper == null || params == null || params.isEmpty()) {
            return wrapper;
        }

        String sortField = (String) params.get("sort");
        String order = (String) params.get("order");

        System.out.println("sortField:" + sortField);
        System.out.println("order:" + order);

        if (StringUtils.isNotBlank(sortField)) {
            List<String> sortFields = Arrays.asList(sortField.split(","));
            if ("desc".equalsIgnoreCase(order)) {
                sortFields.forEach(field -> wrapper.orderByDesc(camelToUnderline(field)));
            } else {
                sortFields.forEach(field -> wrapper.orderByAsc(camelToUnderline(field)));
            }
        }
        return wrapper;
    }

    /**
     * 驼峰格式字符串转换为下划线格式字符串
     *
     * @param param 输入字符串
     * @return 转换后的字符串
     */
    public static String camelToUnderline(String param) {
        if (StringUtils.isBlank(param)) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(param.length());
        for (int i = 0; i < param.length(); i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append(UNDERLINE);
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将Map的key从驼峰格式转换为下划线格式
     *
     * @param param 原始Map
     * @param pre 前缀
     * @return 转换后的Map
     */
    public static Map<String, Object> camelToUnderlineMap(Map<String, Object> param, String pre) {
        if (param == null || param.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> newMap = new HashMap<>(param.size());
        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newKey = camelToUnderline(key);

            if (StringUtils.isBlank(pre)) {
                newMap.put(newKey, value);
            } else if (pre.endsWith(".")) {
                newMap.put(pre + newKey, value);
            } else {
                newMap.put(pre + "." + newKey, value);
            }
        }
        return newMap;
    }

    public static void main(String[] args) {
        System.out.println(camelToUnderline("ABCddfANM"));
    }
}
