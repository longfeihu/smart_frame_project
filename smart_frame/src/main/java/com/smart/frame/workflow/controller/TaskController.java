package com.smart.frame.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.smart.frame.common.constant.HttpStatus;
import com.smart.frame.common.utils.ServletUtils;
import com.smart.frame.framework.aspectj.lang.annotation.Log;
import com.smart.frame.framework.aspectj.lang.enums.BusinessType;
import com.smart.frame.framework.security.LoginUser;
import com.smart.frame.framework.security.service.TokenService;
import com.smart.frame.framework.web.controller.BaseController;
import com.smart.frame.framework.web.page.TableDataInfo;
import com.smart.frame.workflow.dto.ActModelDto;
import com.smart.frame.workflow.dto.ActTaskDto;
import com.smart.frame.workflow.vo.ActTaskVO;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.NativeExecutionQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskInfo;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow/task")
public class TaskController extends BaseController {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private FormService formService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private HistoryService historyService;

    @PreAuthorize("@ss.hasPermi('workflow:task:todo')")
    @GetMapping("/todoTasks")
    public TableDataInfo todoTaskList (ActTaskDto actTaskDto, HttpServletRequest request) {
        // 获取当前登录用户
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        // 当前用户ID
        Long userId = loginUser.getUser().getUserId();
        // 任务查询对象
        TaskQuery taskQuery = taskService.createTaskQuery().includeProcessVariables().taskCandidateOrAssigned(String.valueOf(userId));
        // 任务名称不为空
        if (StringUtils.isNotBlank(actTaskDto.getTaskName())) {
            // 任务名称查询
            taskQuery.taskName(actTaskDto.getTaskName());
            taskQuery.processDefinitionName(actTaskDto.getProcessDefinitionName());
        }
        // 流程名称
        if (StringUtils.isNotBlank(actTaskDto.getProcessDefinitionName())) {
            // 流程名称查询
            taskQuery.processDefinitionName(actTaskDto.getProcessDefinitionName());
        }
        // 排序
        taskQuery.orderByTaskCreateTime().desc();
        // 当前页
        Integer pageNum = actTaskDto.getPageNum();
        // 分页数量
        Integer pageSize = actTaskDto.getPageSize();
        // 查询数据
        List<Task> list = taskQuery.listPage((pageNum - 1) * pageSize, pageSize);
        // 定义集合封装VO数据
        List<ActTaskVO> dataList = new ArrayList<>();
        // 非空判断
        if (list != null && list.size() > 0) {
            // 遍历
            for (Task task : list) {
                // 添加到集合
                dataList.add(this.getActTaskVO(task));
            }
        }
        // 获取总条数
        Long count = taskQuery.count();
        // 返回列表
        return getCustomDataTable(dataList,count);
    }

    private ActTaskVO getActTaskVO (TaskInfo task) {
        ActTaskVO taskInfo = new ActTaskVO();
        taskInfo.setFrom("act");
        Map<String, Object> vars = task.getProcessVariables();
        taskInfo.setId(task.getId());//任务ID
        taskInfo.setName(task.getName());//任务名称
        taskInfo.setCreateTime(task.getCreateTime());//创建时间
        taskInfo.setAssignee(task.getAssignee());//任务办理人
        taskInfo.setProcessInstanceId(task.getProcessInstanceId());//流程实例ID
        taskInfo.setExecutionId(task.getExecutionId());//执行对象ID
        taskInfo.setProcessDefinitionId(task.getProcessDefinitionId());//流程定义ID
        taskInfo.setParentTaskId(task.getParentTaskId());
        Object applyUserName =  vars.get("applyUserName");
        taskInfo.setApplyName(applyUserName == null ? "" : applyUserName.toString());
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        ProcessDefinition processDefinition = (ProcessDefinition) processDefinitionQuery.processDefinitionId(task.getProcessDefinitionId()).singleResult();
        taskInfo.setProcessDefinitionName(processDefinition.getName());
        taskInfo.setVersion(processDefinition.getVersion());
        taskInfo.setStatus(processDefinition.isSuspended() ? "已挂起" : "正常");
        String defKey = processDefinition.getKey();
        //通过流程实例ID查询项目名称
        String buniessId =  (String)vars.get("id");
        if (StringUtils.isNotBlank(task.getAssignee())) {
            taskInfo.setAssigneeName(identityService.createUserQuery().userId(taskInfo.getAssignee()).singleResult().getFirstName());
        }
        return taskInfo;
    }
}
