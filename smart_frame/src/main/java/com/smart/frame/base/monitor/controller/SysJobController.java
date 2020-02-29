package com.smart.frame.base.monitor.controller;

import java.util.List;

import com.smart.frame.base.monitor.domain.SysJob;
import com.smart.frame.base.monitor.service.ISysJobService;
import com.smart.frame.common.exception.job.TaskException;
import com.smart.frame.common.utils.poi.ExcelUtil;
import com.smart.frame.framework.aspectj.lang.annotation.Log;
import com.smart.frame.framework.aspectj.lang.enums.BusinessType;
import com.smart.frame.framework.web.controller.BaseController;
import com.smart.frame.framework.web.domain.AjaxResult;
import com.smart.frame.framework.web.page.TableDataInfo;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.smart.frame.framework.web.domain.AjaxResult.success;

/**
 * 调度任务信息操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/job")
public class SysJobController extends BaseController
{
    private String prefix = "monitor/job";

    @Autowired
    private ISysJobService jobService;

    @PreAuthorize("@ss.hasPermi('monitor:job:view')")
    @GetMapping()
    public String job()
    {
        return prefix + "/job";
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysJob job) {
        startPage();
        List<SysJob> list = jobService.selectJobList(job);
        return getDataTable(list);
    }

    @Log(title = "导出定时任务", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:job:export')")
    @PostMapping("/export")
    public AjaxResult export(SysJob job) {
        List<SysJob> list = jobService.selectJobList(job);
        ExcelUtil<SysJob> util = new ExcelUtil<SysJob>(SysJob.class);
        return util.exportExcel(list, "定时任务");
    }

    @Log(title = "删除定时任务", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:job:remove')")
    @DeleteMapping("/remove/{jobIds}")
    public AjaxResult remove(@PathVariable Long[] jobIds) throws SchedulerException {
        jobService.deleteJobByIds(jobIds);
        return success();
    }

    /**
     * 任务调度状态修改
     */
    @Log(title = "修改定时任务状态", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('monitor:job:changeStatus')")
    @PostMapping("/changeStatus")
    public AjaxResult changeStatus(SysJob job) throws SchedulerException {
        SysJob newJob = jobService.selectJobById(job.getJobId());
        newJob.setStatus(job.getStatus());
        return toAjax(jobService.changeStatus(newJob));
    }

    /**
     * 任务调度立即执行一次
     */
    @Log(title = "执行一次定时任务", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('monitor:job:run')")
    @PostMapping("/run")
    public AjaxResult run(SysJob job) throws SchedulerException {
        jobService.run(job);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:query')")
    @GetMapping(value = "/{jobId}")
    public AjaxResult getJob (@PathVariable Long jobId) {
        return AjaxResult.success(jobService.selectJobById(jobId));
    }

    /**
     * 新增保存调度
     */
    @Log(title = "新增定时任务", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('monitor:job:add')")
    @PostMapping("/add")
    public AjaxResult addSave(@Validated SysJob job) throws SchedulerException, TaskException {
        return toAjax(jobService.insertJob(job));
    }

    /**
     * 修改保存调度
     */
    @Log(title = "修改定时任务", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('monitor:job:edit')")
    @PostMapping("/edit")
    public AjaxResult editSave(@Validated SysJob job) throws SchedulerException, TaskException {
        return toAjax(jobService.updateJob(job));
    }

    /**
     * 校验cron表达式是否有效
     */
    @PostMapping("/checkCronExpressionIsValid")
    public AjaxResult checkCronExpressionIsValid(SysJob job) {
        Boolean checkFlag = jobService.checkCronExpressionIsValid(job.getCronExpression());
        // 校验成功
        if (checkFlag) {
            return AjaxResult.success();
        } else {
            return AjaxResult.error("cron时间表达式格式错误");
        }
    }
}
