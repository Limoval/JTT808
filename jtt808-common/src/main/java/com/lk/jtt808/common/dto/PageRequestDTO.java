package com.lk.jtt808.common.dto;

import lombok.Data;

/**
 * 分页请求DTO
 */
@Data
public class PageRequestDTO {

    /**
     * 当前页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 是否升序
     */
    private Boolean asc = false;

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
