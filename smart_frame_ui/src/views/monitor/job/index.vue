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
      <el-form-item label="任务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择任务状态" clearable size="small">
          <el-option
            v-for="dict in statusOptions"
            :key="dict.dictValue"
            :label="dict.dictLabel"
            :value="dict.dictValue"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:job:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:job:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:job:remove']"
        >删除</el-button>
      </el-col>
    </el-row>

    <el-table border v-loading="loading" :data="jobList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务ID" align="center" prop="jobId" />
      <el-table-column label="任务名称" align="center" prop="jobName" />
      <el-table-column label="任务分组" align="center" prop="jobGroup" :formatter="jobGroupFormat"></el-table-column>
      <el-table-column label="调用目标字符串" align="center" prop="invokeTarget" />
      <el-table-column label="执行表达式" align="center" prop="cronExpression" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleRun(scope.row)"
            v-hasPermi="['monitor:job:run']"
          >执行任务</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['monitor:job:detail']"
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

    <!-- 添加或修改定时任务调度对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px">
      <el-form ref="form" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="form.jobName" type="input" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务分组" prop="jobGroup" label-width="140px">
          <el-select v-model="form.jobGroup" placeholder="请选择">
            <el-option v-for="item in jobGroupOptions" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="调用目标字符串" prop="invokeTarget" label-width="140px">
          <el-input v-model="form.invokeTarget" placeholder="调用目标字符串" />
            <span class="el-icon-warning" style="font-size:12px; color:red;"> Bean调用示例：ryTask.ryParams('ry') </span><br/>
            <span class="el-icon-warning" style="font-size:12px; color:red;"> Class类调用示例：com.ruoyi.quartz.task.RyTask.ryParams('ry') </span><br/>
            <span class="el-icon-warning" style="font-size:12px; color:red;"> 参数说明：支持字符串，布尔类型，长整型，浮点型，整型 </span>
        </el-form-item>
        <el-form-item label="cron执行表达式" prop="cronExpression" label-width="140px">
          <el-input v-model="form.cronExpression" placeholder="请输入cron执行表达式" />
        </el-form-item>
        <el-form-item label="执行策略" prop="misfirePolicy" label-width="140px">
          <el-radio-group v-model="form.misfirePolicy">
            <el-radio label="1">立即执行</el-radio>
            <el-radio label="2">执行一次</el-radio>
            <el-radio label="3">放弃执行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否并发执行" prop="concurrent" label-width="140px">
          <el-radio-group v-model="form.concurrent">
            <el-radio label="0">允许</el-radio>
            <el-radio label="1">禁止</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status" label-width="140px">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in statusOptions" :key="dict.dictValue" :label="dict.dictValue">{{dict.dictLabel}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark" label-width="140px">
          <el-input v-model="form.remark" type="textarea" placeholder="请填写备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 定时任务详细 -->
    <el-dialog title="定时任务详细" :visible.sync="detail" width="700px">
      <el-form ref="form" :model="form" label-width="100px" size="mini">
        <el-row>
          <el-col :span="12">
            <el-form-item label="任务ID：">{{ form.jobId }}</el-form-item>
          </el-col>
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
            <el-form-item label="时间表达式："><el-tag type="danger">{{ form.cronExpression }}</el-tag></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务状态：">
              <div v-if="form.status == 0"><el-tag type="success">启用</el-tag></div>
              <div v-else-if="form.status == 1"><el-tag type="danger">停用</el-tag></div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="并发执行：">
              <div v-if="form.concurrent == 0"><el-tag type="success">允许</el-tag></div>
              <div v-else-if="form.concurrent == 1"><el-tag type="danger">禁止</el-tag></div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次执行时间："><el-tag type="danger">{{ parseTime(form.nextValidTime) }}</el-tag></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标字符串：">{{ form.invokeTarget }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="创建时间：">{{ parseTime(form.createTime) }}</el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detail = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listJob, getJob, deleteJob, addJob, updateJob, exportJob, changeJobStatus, runJob, checkCronExpression } from "@/api/monitor/job";

export default {
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
      // 定时任务调度表格数据
      jobList: [],
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
      // cron表达式校验标识
      cronFlag: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        jobName: undefined,
        jobGroup: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        jobName: [
          { required: true, message: "请填写任务名称", trigger: "blur" }
        ],
        jobGroup: [
          { required: true, message: "请选择任务分组", trigger: "blur" }
        ],
        invokeTarget: [
          { required: true, message: "请填写调用目标字符串", trigger: "blur" }
        ],
        cronExpression: [
          { required: true, message: "请填写corn时间表达式", trigger: "blur" }
        ]
       }
    };
  },
  created() {
    this.getList();
    this.getDicts("sys_job_status").then(response => {
      this.statusOptions = response.data;
    });
    this.getDicts("sys_job_group").then(response => {
      this.jobGroupOptions = response.data;
    });
  },
  methods: {
    /** 查询定时任务调度列表 */
    getList() {
      this.loading = true;
      listJob(this.queryParams).then(response => {
        this.jobList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 菜单显示状态字典翻译
    jobGroupFormat(row, column) {
      return this.selectDictLabel(this.jobGroupOptions, row.jobGroup);
    },
    // 表单重置
    reset() {
      this.form = {
        jobId: undefined,
        jobName: undefined,
        jobGroup: "DEFAULT",
        invokeTarget: undefined,
        cronExpression: undefined,
        misfirePolicy: "1",
        concurrent: "1",
        status: "0",
        createBy: undefined,
        createTime: undefined,
        updateBy: undefined,
        updateTime: undefined,
        remark: undefined
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
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.jobId)
      this.single = selection.length!=1
      this.multiple = !selection.length
    },
    /** 详细按钮操作 */
    handleView(row) {
      this.detail = true;
      this.form = row;
    },
    // 定时任务状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$confirm('确认要"' + text + '""' + row.jobName + '"任务吗?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return changeJobStatus(row.jobId, row.status);
        }).then(() => {
          this.msgSuccess(text + "成功");
        }).catch(function() {
          row.status = row.status === "0" ? "1" : "0";
        });
    },
    // 执行任务
    handleRun(row) {
      this.$confirm('确认立即执行一次任务吗?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return runJob(row.jobId, row.jobGroup);
        }).then(() => {
          this.msgSuccess("执行成功");
        }).catch(function() {
          this.msgError(response.msg);
        });
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加定时任务调度";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const jobId = row.jobId || this.ids
      getJob(jobId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改定时任务调度";
      });
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 校验时间表达式
          checkCronExpression(this.form.cronExpression).then(response => {
            if (response.code == 200) {
              // 校验成功
              if (this.form.jobId != undefined) {
                updateJob(this.form).then(response => {
                  if (response.code === 200) {
                    this.msgSuccess("修改成功");
                    this.open = false;
                    this.getList();
                  } else {
                    this.msgError(response.msg);
                  }
                });
              } else {
                addJob(this.form).then(response => {
                  if (response.code === 200) {
                    this.msgSuccess("新增成功");
                    this.open = false;
                    this.getList();
                  } else {
                    this.msgError(response.msg);
                  }
                });
              }
            } else {
              this.msgError("时间表达式格式错误,请重新填写");
            }
          });
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const jobIds = row.jobId || this.ids;
      this.$confirm('是否确认删除定时任务调度编号为"' + jobIds + '"的数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return deleteJob(jobIds);
        }).then(() => {
          this.getList();
          this.msgSuccess("删除成功");
        }).catch(function() {});
    },
    /** 导出按钮操作 */
    handleExport() {
      const queryParams = this.queryParams;
      this.$confirm('是否确认导出所有定时任务调度数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return exportJob(queryParams);
        }).then(response => {
          this.download(response.msg);
        }).catch(function() {});
    },
    // 校验时间表达式
    handleCronExpression (cronExpression) {
      var that = this;
      checkCronExpression(cronExpression).then(response => {
        if (response.data) {
          that.cronFlag = true
        } else {
          that.cronFlag = false
        }
      });
    }
  }
};
</script>
