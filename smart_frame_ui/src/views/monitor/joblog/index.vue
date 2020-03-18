<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px">
      <el-form-item label="任务名称" prop="jobName">
        <el-input
          v-model="queryParams.jobName"
          placeholder="请输入任务名称"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务分组" prop="jobGroup">
        <el-select v-model="queryParams.jobGroup" placeholder="请选择任务分组" clearable size="small">
          <el-option
            v-for="dict in jobGroupOptions"
            :key="dict.dictValue"
            :label="dict.dictLabel"
            :value="dict.dictValue"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="执行状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择任务状态" clearable size="small">
          <el-option
            v-for="dict in statusOptions"
            :key="dict.dictValue"
            :label="dict.dictLabel"
            :value="dict.dictValue"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="执行时间">
        <el-date-picker
          v-model="dateRange"
          size="small"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['monitor:joblog:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete-solid"
          size="mini"
          @click="handleClean"
          v-hasPermi="['monitor:joblog:clean']"
        >清空</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['monitor:joblog:export']"
        >导出</el-button>
      </el-col>
    </el-row>

    <el-table border v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="日志编号" align="center" prop="jobLogId" />
      <el-table-column label="任务名称" align="center" prop="jobName" />
      <el-table-column label="任务分组" align="center" prop="jobGroup" :formatter="jobGroupFormat"/>
      <el-table-column label="目标字符串" align="center" prop="invokeTarget" />
      <el-table-column label="日志信息" align="center" prop="jobMessage" />
      <el-table-column label="执行状态" align="center" prop="status">
        <template slot-scope="scope">
          <div v-if="scope.row.status == 0"><el-tag type="success">成功</el-tag></div>
          <div v-else-if="scope.row.status == 1"><el-tag type="danger">失败</el-tag></div>
        </template>
      </el-table-column>
      <el-table-column label="执行时间" align="center" prop="createTime" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleView(scope.row)"
            v-hasPermi="['system:log:edit']"
          >详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 调度日志详情 -->
    <el-dialog title="调度日志详情" :visible.sync="detail" width="800px">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="任务名称：">{{ form.jobName }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务分组：">
              <div v-if="form.jobGroup == 'DEFAULT'">默认</div>
              <div v-else-if="form.jobGroup == 'SYSTEM'">系统</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标字符串：">{{ form.invokeTarget }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日志信息：">{{ form.jobMessage }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="执行状态：">
              <div v-if="form.status == 0"><el-tag type="success">成功</el-tag></div>
              <div v-else-if="form.status == 1"><el-tag type="danger">失败</el-tag></div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="异常信息：">{{ form.exceptionInfo }}</el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listLog, getLog, delLog, exportLog, cleanLog } from "@/api/monitor/joblog";

export default {
  name: "Log",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 总条数
      total: 0,
      // 定时任务调度日志表格数据
      logList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否显示详情
      detail: false,
      // 任务分组
      jobGroupOptions: [],
      // 任务状态
      statusOptions: [],
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        jobName: undefined,
        jobGroup: undefined,
        invokeTarget: undefined,
        jobMessage: undefined,
        status: undefined,
        exceptionInfo: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        jobName: [
          { required: true, message: "任务名称不能为空", trigger: "blur" }
        ],        jobGroup: [
          { required: true, message: "任务组名不能为空", trigger: "blur" }
        ],        invokeTarget: [
          { required: true, message: "调用目标字符串不能为空", trigger: "blur" }
        ],      }
    };
  },
  created() {
    this.getList();
    this.getDicts("sys_job_group").then(response => {
      this.jobGroupOptions = response.data;
    });
    this.getDicts("sys_common_status").then(response => {
      this.statusOptions = response.data;
    });
  },
  methods: {
    /** 查询定时任务调度日志列表 */
    getList() {
      this.loading = true;
      listLog(this.queryParams).then(response => {
        this.logList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 任务分组字典翻译
    jobGroupFormat(row, column) {
      return this.selectDictLabel(this.jobGroupOptions, row.jobGroup);
    },
    // 表单重置
    reset() {
      this.form = {
        jobLogId: undefined,
        jobName: undefined,
        jobGroup: undefined,
        invokeTarget: undefined,
        jobMessage: undefined,
        status: "0",
        exceptionInfo: undefined,
        createTime: undefined
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.jobLogId)
      this.single = selection.length!=1
      this.multiple = !selection.length
    },
    /** 详细按钮操作 */
    handleView(row) {
      this.detail = true;
      this.form = row;
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.jobLogId != undefined) {
            updateLog(this.form).then(response => {
              if (response.code === 200) {
                this.msgSuccess("修改成功");
                this.open = false;
                this.getList();
              } else {
                this.msgError(response.msg);
              }
            });
          } else {
            addLog(this.form).then(response => {
              if (response.code === 200) {
                this.msgSuccess("新增成功");
                this.open = false;
                this.getList();
              } else {
                this.msgError(response.msg);
              }
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const jobLogIds = row.jobLogId || this.ids;
      this.$confirm('是否确认删除定时任务调度日志编号为"' + jobLogIds + '"的数据项?', "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(function() {
        return delLog(jobLogIds);
      }).then(() => {
        this.getList();
        this.msgSuccess("删除成功");
      }).catch(function() {});
    },
    /** 清空按钮操作 */
    handleClean() {
      this.$confirm('是否确清空定时任务调度日志的数据项?', "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(function() {
        return cleanLog();
      }).then(() => {
        this.getList();
        this.msgSuccess("清空成功");
      }).catch(function() {});
    },
    /** 导出按钮操作 */
    handleExport() {
      const queryParams = this.queryParams;
      this.$confirm('是否确认导出所有定时任务调度日志数据项?', "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(function() {
        return exportLog(queryParams);
      }).then(response => {
        this.download(response.msg);
      }).catch(function() {});
    },
    /** 返回定时任务列表 */
    backJob() {
      this.$router.push({ path: '/monitor/job'})
    }
  }
};
</script>
