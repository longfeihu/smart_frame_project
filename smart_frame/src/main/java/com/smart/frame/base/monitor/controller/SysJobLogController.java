package com.smart.frame.base.monitor.controller;

import java.util.List;

import com.smart.frame.base.monitor.domain.SysJobLog;
import com.smart.frame.base.monitor.service.ISysJobLogService;
import com.smart.frame.common.utils.poi.ExcelUtil;
import com.smart.frame.framework.aspectj.lang.annotation.Log;
import com.smart.frame.framework.aspectj.lang.enums.BusinessType;
import com.smart.frame.framework.web.controller.BaseController;
import com.smart.frame.framework.web.domain.AjaxResult;
import com.smart.frame.framework.web.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.smart.frame.framework.web.domain.AjaxResult.success;

/**
 * 调度日志操作处理
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/monitor/jobLog")
public class SysJobLogController extends BaseController {

    private String prefix = "monitor/job";

    @Autowired
    private ISysJobLogService jobLogService;

    @PreAuthorize("@ss.hasPermi('monitor:job:view')")
    @GetMapping()
    public String jobLog()
    {
        return prefix + "/jobLog";
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SysJobLog jobLog)
    {
        startPage();
        List<SysJobLog> list = jobLogService.selectJobLogList(jobLog);
        return getDataTable(list);
    }

    @Log(title = "调度日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:job:export')")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SysJobLog jobLog)
    {
        List<SysJobLog> list = jobLogService.selectJobLogList(jobLog);
        ExcelUtil<SysJobLog> util = new ExcelUtil<SysJobLog>(SysJobLog.class);
        return util.exportExcel(list, "调度日志");
    }

    @Log(title = "调度日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:job:remove')")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(jobLogService.deleteJobLogByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:detail')")
    @GetMapping("/detail/{jobLogId}")
    public String detail(@PathVariable("jobLogId") Long jobLogId, ModelMap mmap)
    {
        mmap.put("name", "jobLog");
        mmap.put("jobLog", jobLogService.selectJobLogById(jobLogId));
        return prefix + "/detail";
    }

    @Log(title = "调度日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:job:remove')")
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean()
    {
        jobLogService.cleanJobLog();
        return success();
    }
}
