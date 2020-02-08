package com.smart.frame.workflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smart.frame.framework.aspectj.lang.annotation.Log;
import com.smart.frame.framework.aspectj.lang.enums.BusinessType;
import com.smart.frame.framework.web.controller.BaseController;
import com.smart.frame.framework.web.domain.AjaxResult;
import com.smart.frame.framework.web.page.TableDataInfo;
import com.smart.frame.workflow.dto.ActModelDto;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.FormService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ModelQuery;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow/model")
public class ModelController extends BaseController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private FormService formService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 模型列表
     * @param actModelDto
     * @param request
     * @return
     */
    @GetMapping("/list")
    public TableDataInfo modelList (ActModelDto actModelDto, HttpServletRequest request) {
        // 创建模型查询对象
        ModelQuery modelQuery = repositoryService.createModelQuery();
        // 查询条件不未空
        if (actModelDto != null) {
            // 模型KEY部位空
            if (!StringUtils.isEmpty(actModelDto.getKey())) {
                modelQuery.modelKey(actModelDto.getKey());
            }
            // 模型名称不为空
            if (!StringUtils.isEmpty(actModelDto.getName())) {
                modelQuery.modelNameLike("%" + actModelDto.getName() + "%");
            }
            // 分页查询
            int i = actModelDto.getPageSize() * (Integer.valueOf(actModelDto.getPageNum() - 1));
            // 分页查询
            List<Model> modelList = modelQuery.listPage(i, actModelDto.getPageSize());
            // 总条数
            long count = repositoryService.createModelQuery().count();
            // 返回
            return getCustomDataTable(modelList,count);
        } else {
            // 返回空
            return getDataTable(new ArrayList<>());
        }
    }

    @PreAuthorize("@ss.hasPermi('workflow:model:add')")
    @PostMapping("/add")
    public AjaxResult addModel (@RequestBody ActModelDto actModelDto) throws UnsupportedEncodingException {
        // 创建模型对象
        Model model = repositoryService.newModel();
        // 模型名称
        String name = actModelDto.getName();
        // 模型描述
        String description = actModelDto.getDescription();
        // 默认版本
        int revision = 1;
        // 模型KEY
        String key = actModelDto.getKey();
        // 节点信息
        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
        modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
        modelNode.put(ModelDataJsonConstants.MODEL_REVISION, revision);

        model.setName(name);
        model.setKey(key);
        model.setMetaInfo(modelNode.toString());

        repositoryService.saveModel(model);
        String id = model.getId();

        // 完善ModelEditorSource
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        repositoryService.addModelEditorSource(id, editorNode.toString().getBytes("utf-8"));
        Map<String, Object> map = new HashMap(2);
        map.put("id",id);

        return AjaxResult.success("操作成功",map);
    }

    @GetMapping("/deploy/{modelId}")
    public AjaxResult deployModel (@PathVariable("modelId") String modelId) throws IOException {
        // 如果id不为空
        if (!StringUtils.isEmpty(modelId)) {
            // 获取模型对象
            Model model = repositoryService.getModel(modelId);
            // 获取模型二进制数据
            byte[] bytes = repositoryService.getModelEditorSource(model.getId());
            // 如果不为空
            if (bytes == null) {
                // 返回失败
                return AjaxResult.error("模型数据为空,无法部署");
            }

            JsonNode modelNode = new ObjectMapper().readTree(bytes);
            BpmnModel bpmModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            if (bpmModel.getProcesses().size() == 0) {
                // 返回失败
                return AjaxResult.error("模型对应流程为空,无法部署");
            }
            byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(bpmModel);
            // 发布流程
            String processName = model.getName() + ".bpmn20.xml";
            Deployment deployment = repositoryService.createDeployment().name(model.getName()).addString(processName, new String(bpmnBytes, "UTF-8")).deploy();
            model.setDeploymentId(deployment.getId());
            repositoryService.saveModel(model);
            return AjaxResult.success("部署成功");
        } else {
            return AjaxResult.error("模型ID为空");
        }
    }

    @Log(title = "删除模型", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('workflow:model:remove')")
    @DeleteMapping("/{modelIds}")
    public AjaxResult removeModel(@PathVariable String[] modelIds) {
        // 如果模型ID不为空
        if (modelIds != null) {
            // 创建模型查询对象
            ModelQuery modelQuery = repositoryService.createModelQuery();
            // 遍历modelId
            for (String modelId : modelIds) {
                // 删除model
                repositoryService.deleteModel(modelId);
            }
        }
        return AjaxResult.success("操作成功");
    }
}
