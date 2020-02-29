package com.smart.frame.workflow.controller;

import com.smart.frame.common.utils.ServletUtils;
import com.smart.frame.framework.security.LoginUser;
import com.smart.frame.framework.security.service.TokenService;
import com.smart.frame.framework.web.controller.BaseController;
import com.smart.frame.framework.web.domain.AjaxResult;
import com.smart.frame.framework.web.page.TableDataInfo;
import com.smart.frame.workflow.dto.ProcessDto;
import com.smart.frame.workflow.vo.ProcessVO;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow/process")
public class ProcessController extends BaseController {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ProcessEngineFactoryBean processEngine;
    @Autowired
    private TokenService tokenService;


    /**
     * 启动流程
     * @return
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:start')")
    @PostMapping("/start")
    public AjaxResult start () {

        return AjaxResult.success();
    }

    /**
     * 绘制动态流程图
     * @param processInstanceId
     * @param response
     * @throws IOException
     */
    @GetMapping("/diagram/{processInstanceId}")
    public void diagram (@PathVariable String processInstanceId, HttpServletResponse response) throws IOException {
        // 获取历史流程实例
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        // 获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        ProcessEngineConfigurationImpl processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        Context.setProcessEngineConfiguration((ProcessEngineConfigurationImpl) processEngineConfiguration);

        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

        List<HistoricActivityInstance> highLightedActivitList =  historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
        // 高亮环节id集合
        List<String> highLightedActivitis = new ArrayList<String>();

        // 高亮线路id集合
        List<String> highLightedFlows = getHighLightedFlows(definitionEntity,highLightedActivitList);

        for(HistoricActivityInstance tempActivity : highLightedActivitList){
            String activityId = tempActivity.getActivityId();
            highLightedActivitis.add(activityId);
        }
        //中文显示的是口口口，设置字体就好了
        InputStream imageStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivitis,highLightedFlows,processEngine.getProcessEngineConfiguration().getActivityFontName(),processEngine.getProcessEngineConfiguration().getActivityFontName(),processEngine.getProcessEngineConfiguration().getLabelFontName(),null,1.0);
        // 输出资源内容到相应对象
        byte[] b = new byte[1024];
        int len;
        while ((len = imageStream.read(b, 0, 1024)) != -1) {
            response.getOutputStream().write(b, 0, len);
        }
    }

    /**
     * 绘制流程图高亮部分线条
     * @param processDefinitionEntity
     * @param historicActivityInstances
     * @return
     */
    private List<String> getHighLightedFlows(ProcessDefinitionEntity processDefinitionEntity, List<HistoricActivityInstance> historicActivityInstances) {
        // 用以保存高亮的线flowId
        List<String> highFlows = new ArrayList<String>();
        // 对历史流程节点进行遍历
        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
            // 得到节点定义的详细信息
            ActivityImpl activityImpl = processDefinitionEntity.findActivity(historicActivityInstances.get(i).getActivityId());
            // 用以保存后需开始时间相同的节点
            List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();
            ActivityImpl sameActivityImpl1 = processDefinitionEntity.findActivity(historicActivityInstances.get(i + 1).getActivityId());
            // 将后面第一个节点放在时间相同节点的集合里
            sameStartTimeNodes.add(sameActivityImpl1);
            for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
                HistoricActivityInstance activityImpl1 = historicActivityInstances.get(j);// 后续第一个节点
                HistoricActivityInstance activityImpl2 = historicActivityInstances.get(j + 1);// 后续第二个节点
                if (activityImpl1.getStartTime().equals(activityImpl2.getStartTime())) {
                    // 如果第一个节点和第二个节点开始时间相同保存
                    ActivityImpl sameActivityImpl2 = processDefinitionEntity.findActivity(activityImpl2.getActivityId());
                    sameStartTimeNodes.add(sameActivityImpl2);
                } else {
                    // 有不相同跳出循环
                    break;
                }
            }
            // 取出节点的所有出去的线
            List<PvmTransition> pvmTransitions = activityImpl.getOutgoingTransitions();
            for (PvmTransition pvmTransition : pvmTransitions) {
                // 对所有的线进行遍历
                ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition.getDestination();
                // 如果取出的线的目标节点存在时间相同的节点里;
                if (sameStartTimeNodes.contains(pvmActivityImpl)) {
                    // 保存该线的id，进行高亮显示
                    highFlows.add(pvmTransition.getId());
                }
            }
        }
        return highFlows;
    }

    /**
     * 查询未结束的流程
     * @param processDto
     * @param request
     * @return
     * @throws ParseException
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:unfinished')")
    @GetMapping("/unfinishedProcess")
    public TableDataInfo unfinishedProcess (ProcessDto processDto, HttpServletRequest request) throws ParseException {
        // 获取当前登录用户
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        // 当前用户ID
        Long userId = loginUser.getUser().getUserId();
        // 时间格式化
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 获取流程名称
        String processName = processDto.getProcessName();
        // 获取开始时间
        String begTime = processDto.getBegTime();
        // 获取结束时间
        String endTime = processDto.getEndTime();
        // 查询未完成的流程
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery().includeProcessVariables();
        // 查询个人
        historicProcessInstanceQuery.involvedUser(userId.toString());
        // 未完成的
        historicProcessInstanceQuery.unfinished();
        // 不为空
        if (StringUtils.isNotBlank(processName)) {
            // 名称查询
            historicProcessInstanceQuery.processDefinitionName(processName);
        }
        // 不为空
        if (StringUtils.isNotBlank(begTime)) {
            // 开始时间
            historicProcessInstanceQuery.startedAfter(sdf.parse(begTime));
        }
        // 不为空
        if (StringUtils.isNotBlank(endTime)) {
            // 结束时间
            historicProcessInstanceQuery.startedBefore(sdf.parse(endTime));
        }
        // 当前页
        Integer pageNum = processDto.getPageNum();
        // 分页数量
        Integer pageSize = processDto.getPageSize();
        // 排序
        historicProcessInstanceQuery.orderByProcessInstanceStartTime().desc();
        // 查询数据
        List<HistoricProcessInstance> listPage = historicProcessInstanceQuery.listPage((pageNum - 1) * pageSize, pageSize);
        // 总条数
        long count = historicProcessInstanceQuery.list().size();
        // 定义集合封装VO数据
        List<ProcessVO> dataList = new ArrayList<>();
        // 非空判断
        if (listPage != null && listPage.size() > 0) {
            // 遍历
            for (HistoricProcessInstance process : listPage) {
                // 添加到集合
                dataList.add(this.getProcessVO(process));
            }
        }
        // 返回列表
        return getCustomDataTable(dataList,count);
    }

    /**
     * 组装流程数据
     * @param process
     * @return
     */
    private ProcessVO getProcessVO (HistoricProcessInstance process) {
        ProcessVO processInfo = new ProcessVO();
        Map<String, Object> vars = process.getProcessVariables();
        // 流程ID
        processInfo.setId(process.getId());
        // 流程名称
        processInfo.setName(process.getName());
        // 描述信息
        processInfo.setDescription((String)vars.get("description"));
        // 开始时间
        processInfo.setStartTime(process.getStartTime());
        // 当前任务
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(process.getId()).list();
        // 如果有任务
        if(tasks.size()>0){
            // 获取任务
            Task task = tasks.get(0);
            // 当前任务ID
            processInfo.setCurrentTaskId(task.getId());
            // 当前任务名称
            processInfo.setCurrentTaskName(task.getName());
            // 当前任务办理人
            processInfo.setAssignee(task.getAssignee());
            // 当前任务办理人
            if (StringUtils.isNotBlank(task.getAssignee())) {
                // 设置办理人名称
                processInfo.setAssigneeName(identityService.createUserQuery().userId(processInfo.getAssignee()).singleResult().getFirstName());
            }
            // 当前任务候选人名称集合
            processInfo.setCandidateUserName(getTaskCandidates(task.getId()));
        }
        // 流程定义ID
        processInfo.setProcessDefinitionId(process.getProcessDefinitionId());
        // 流程发起人
        Object applyUserName =  vars.get("applyUserName");
        // 流程发起人名称
        processInfo.setApplyName(applyUserName == null ? "" : applyUserName.toString());
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        ProcessDefinition processDefinition = (ProcessDefinition) processDefinitionQuery.processDefinitionId(process.getProcessDefinitionId()).singleResult();
        // 版本
        processInfo.setVersion(processDefinition.getVersion());
        // 流程名称
        processInfo.setWorkflowName(processDefinition.getName());
        // 流程状态
        processInfo.setStatus(processDefinition.isSuspended() ? "已挂起" : "正常");
        // 返回
        return processInfo;
    }

    /**
     * 分隔的候选人名单
     * @param taskId
     * @return
     */
    private String getTaskCandidates(String taskId) {
        // 用户名称拼接
        StringBuilder namesStr = new StringBuilder();
        // 获取所有用户
        List<User> list = getTaskCandidate(taskId);
        // 如果不为空
        if(!list.isEmpty()){
            // 遍历并用逗号分隔
            for (int i = 0; i < list.size(); i++) {
                // 如果不是最后一个
                if (i != (list.size() - 1)) {
                    // 用竖线分隔用户名称
                    namesStr.append(list.get(i).getFirstName()).append(" | ");
                } else {
                    // 最后一个不拼接竖线
                    namesStr.append(list.get(i).getFirstName());
                }
            }
        }
        // 返回拼接好的名称
        return namesStr.toString();
    }

    /**
     * 任务候选人列表
     * @param taskId
     * @return
     */
    private List<User> getTaskCandidate(String taskId) {
        // 用户List
        List<User> users = new ArrayList<User>();
        // 获取任务候选人
        List identityLinkList = taskService.getIdentityLinksForTask(taskId);
        // 如果不为空
        if (identityLinkList != null && identityLinkList.size() > 0) {
            // 遍历
            for (Iterator iterator = identityLinkList.iterator(); iterator.hasNext();) {
                // 获取下一流程
                IdentityLink identityLink = (IdentityLink) iterator.next();
                // 如果不为空
                if (identityLink.getUserId() != null) {
                    // 获取该用户
                    User user = getUser(identityLink.getUserId());
                    // 不为空
                    if (user != null) {
                        // 添加到集合
                        users.add(user);
                    }
                }
                // 如果组ID不为ong
                if (identityLink.getGroupId() != null) {
                    // 根据组获得对应人员
                    List userList = identityService.createUserQuery().memberOfGroup(identityLink.getGroupId()).list();
                    // 如果List不为空
                    if (userList != null && userList.size() > 0) {
                        // 添加到用户集合
                        users.addAll(userList);
                    }
                }
            }
        }
        // 返回所有用户
        return users;
    }

    /**
     * 获取流程用户
     * @param userId
     * @return
     */
    private User getUser(String userId) {
        return identityService.createUserQuery().userId(userId).singleResult();
    }
}
