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
import org.springframework.web.bind.annotation.*;

/**
 * 调度日志操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/joblog")
public class SysJobLogController extends BaseController {

    @Autowired
    private ISysJobLogService jobLogService;

    @PreAuthorize("@ss.hasPermi('monitor:joblog:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysJobLog jobLog) {
        startPage();
        List<SysJobLog> list = jobLogService.selectJobLogList(jobLog);
        return getDataTable(list);
    }

    @Log(title = "调度日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:joblog:export')")
    @PostMapping("/export")
    public AjaxResult export(SysJobLog jobLog) {
        List<SysJobLog> list = jobLogService.selectJobLogList(jobLog);
        ExcelUtil<SysJobLog> util = new ExcelUtil<SysJobLog>(SysJobLog.class);
        return util.exportExcel(list, "调度日志");
    }

    @Log(title = "调度日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:joblog:remove')")
    @DeleteMapping("/{jobLogIds}")
    public AjaxResult remove(@PathVariable Long[] jobLogIds) {
        return toAjax(jobLogService.deleteJobLogByIds(jobLogIds));
    }

    @PreAuthorize("@ss.hasPermi('monitor:joblog:detail')")
    @GetMapping("/detail/{jobLogId}")
    public AjaxResult detail(@PathVariable("jobLogId") Long jobLogId) {
        return AjaxResult.success(jobLogService.selectJobLogById(jobLogId));
    }

    @Log(title = "调度日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:joblog:clean')")
    @PostMapping("/clean")
    public AjaxResult clean() {
        jobLogService.cleanJobLog();
        return AjaxResult.success();
    }
}
