package com.smart.frame.workflow.dto;

import java.io.Serializable;

public class ActModelDto implements Serializable {

    // 当前页
    private Integer pageNum;
    // 每页显示记录数
    private Integer pageSize;
    // 模型key
    private String key;
    // 模型名称
    private String name;
    // 模型描述
    private String description;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
