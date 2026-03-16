package com.gcs.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.OrderItem;

/**
 * 查询参数封装类
 * 支持分页查询参数处理
 */
public class Query<T> extends LinkedHashMap<String, Object> {
	private static final long serialVersionUID = 1L;
    /**
     * mybatis-plus分页参数
     */
    private Page<T> page;  // 改为具体的Page类型
    /**
     * 当前页码
     */
    private long currPage = 1;
    /**
     * 每页条数
     */
    private int limit = 10;

    public Query(Map<String, Object> params) {
        this.putAll(params);

        //分页参数
        if(params.get("page") != null){
            Object pageObj = params.get("page");
            if(pageObj instanceof String){
                currPage = Long.parseLong((String)pageObj);
            }else if(pageObj instanceof Integer){
                currPage = ((Integer)pageObj).longValue();
            }else if(pageObj instanceof Long){
                currPage = (Long)pageObj;
            }
        }
        if(params.get("limit") != null){
            Object limitObj = params.get("limit");
            if(limitObj instanceof String){
                limit = Integer.parseInt((String)limitObj);
            }else if(limitObj instanceof Integer){
                limit = (Integer)limitObj;
            }else if(limitObj instanceof Long){
                limit = ((Long)limitObj).intValue();
            }
        }

        this.put("offset", (currPage - 1) * limit);
        this.put("page", currPage);
        this.put("limit", limit);

        //防止SQL注入（因为sidx、order是通过拼接SQL实现排序的，会有SQL注入风险）
        String sidx = SQLFilter.sqlInject((String)params.get("sidx"));
        String order = SQLFilter.sqlInject((String)params.get("order"));
        this.put("sidx", sidx);
        this.put("order", order);

        //mybatis-plus分页
        this.page = new Page<>(currPage, limit);

        //排序
        if(StringUtils.isNotBlank(sidx) && StringUtils.isNotBlank(order)){
            if("asc".equalsIgnoreCase(order)) {
                this.page.addOrder(OrderItem.asc(sidx));
            }else {
                this.page.addOrder(OrderItem.desc(sidx));
            }
        }

    }

    public Page<T> getPage() {  // 返回类型也相应修改
        return page;
    }

    public long getCurrPage() {
        return currPage;
    }

    public int getLimit() {
        return limit;
    }
}
