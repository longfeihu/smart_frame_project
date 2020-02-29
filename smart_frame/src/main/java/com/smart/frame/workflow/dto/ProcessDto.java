package com.smart.frame.workflow.dto;

import java.io.Serializable;


public class ProcessDto implements Serializable {

	private static final long serialVersionUID = 1L;
	// 流程ID
	private String processId;
	// 流程名称
	private String processName;
	// 开始时间
	private String begTime;
	// 截止时间
	private String endTime;
	// 当前页
	private Integer pageNum;
	// 每页显示数量
	private Integer pageSize;

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public String getBegTime() {
		return begTime;
	}

	public void setBegTime(String begTime) {
		this.begTime = begTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

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
}