<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true">
      <el-form-item label="流程名称" prop="processDefinitionName">
        <el-input
          v-model="queryParams.processDefinitionName"
          placeholder="请输入流程名称"
          clearable
          size="small"
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入任务名称"
          clearable
          size="small"
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 待办任务列表 -->
    <el-table border v-loading="loading" :data="todoTaskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务ID" prop="id" align="center" width="120" />
      <el-table-column label="流程名称" prop="processDefinitionName" align="center" width="200" />
      <el-table-column label="任务名称" prop="name" align="center" width="200" />
      <el-table-column label="模型状态" align="center" prop="status" :formatter="statusFormat" />
      <el-table-column label="创建时间" align="center" prop="startTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="办理人" prop="applyName" align="center" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            v-if="!scope.row.assignee"
            size="mini"
            type="text"
            icon="el-icon-star-off"
            @click="onSignBtnClick(scope.row)"
            v-hasPermi="['workflow:task:sign']"
          >签收任务</el-button>
          <el-button
            v-if="scope.row.assignee"
            size="mini"
            type="text"
            icon="el-icon-check"
            @click="handleTask(scope.row.id)"
          >办理任务</el-button>
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
    <!-- 审批记录 -->
    <approveRecord v-if="toRecord" v-model="toRecord" @on-close="handleRecordClose" />
  </div>
</template>

<script>
import { todoTasks, signTask } from "@/api/workflow/task";
import approveRecord from '../process/record'

export default {
  name: "todoTask",
  components: {
    approveRecord
  },
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
      // 模型表格数据
      todoTaskList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否显示弹出层（数据权限）
      openDataScope: false,
      // 日期范围
      dateRange: [],
      // 状态数据字典
      statusOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined,
        processDefinitionName: undefined
      },
      toRecord: undefined,
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "label"
      }
    };
  },
  created() {
    this.getList();
    this.getDicts("wf_task_status").then(response => {
      this.statusOptions = response.data;
    });
  },
  methods: {
    /** 查询模型列表 */
    getList() {
      this.loading = true;
      todoTasks(this.addDateRange(this.queryParams, this.dateRange)).then(
        response => {
          this.todoTaskList = response.rows;
          this.total = response.total;
          this.loading = false;
        }
      );
    },
    // 字典状态字典翻译
    statusFormat(row, column) {
      return row.status ? "已部署" : "未部署";
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        name: undefined,
        processDefinitionName: undefined
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 关闭编辑器 */
    handleProcessClose() {
      this.getList()
    },
    /** 编辑模型按钮 */
    onEditBtnClick(row) {
      this.editProcess = row
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!=1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleTask(row) {
      this.reset();
      this.open = true;
      this.title = "新建模型";
    },
    handleSign: function(taskId) {
      // 如果不为空
      if (taskId) {
        // 部署
        signTask(taskId).then(response => {
          if (response.code === 200) {
            this.msgSuccess("签收成功");
            this.getList();
          } else {
            this.msgError(response.msg);
          }
        });
      }
    }
  }
};
</script>
