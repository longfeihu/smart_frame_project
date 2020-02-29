<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true">
      <el-form-item label="流程名称" prop="processId">
        <el-input
          v-model="queryParams.processId"
          placeholder="请输入流程Id"
          clearable
          size="small"
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="流程名称" prop="processName">
        <el-input
          v-model="queryParams.processName"
          placeholder="请输入流程名称"
          clearable
          size="small"
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker
          v-model="queryParams.begTime"
          align="center"
          type="date"
          placeholder="请选择开始日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="截止日期">
        <el-date-picker
          v-model="queryParams.endTime"
          align="center"
          type="date"
          placeholder="请选择开始日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
    </el-row>

    <el-table border v-loading="loading" :data="processList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" align="center" />
      <el-table-column label="流程ID" align="center" prop="id" />
      <el-table-column label="流程名称" align="center" prop="workflowName" />
      <el-table-column label="流程说明" align="center" prop="description" />
      <el-table-column label="流程版本" align="center" prop="version" />
      <el-table-column label="流程状态" align="center" prop="status" />
      <el-table-column label="启动时间" align="center" prop="startTime" />
      <el-table-column label="当前节点" align="center" prop="currentTaskName" />
      <el-table-column label="节点执行/候选人员" align="center" prop="assigneeName ? assigneeName : candidateUserName" />
      <el-table-column
        label="操作"
        align="center"
        width="180"
        class-name="small-padding fixed-width"
      >
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['workflow:process:editAgassigne']">指定执行人</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script>
import { unfinishedProcess } from "@/api/workflow/process";
import { getToken } from "@/utils/auth";
export default {
  name: "Manage",
  components: {},
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
      // 用户表格数据
      processList: null,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      // 表单参数
      form: {},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        processId: undefined,
        processName: undefined,
        begTime: undefined,
        endTime: undefined
      }
    };
  },
  watch: {
  },
  created() {
    // 获取表单数据
    this.getList();
  },
  methods: {
    /** 查询流程列表 */
    getList() {
      this.loading = true;
      unfinishedProcess(this.queryParams).then(response => {
          this.processList = response.rows;
          this.total = response.total;
          this.loading = false;
        }
      );
    },
    // 表单重置
    reset() {
      this.form = {
        processId: undefined,
        processName: undefined,
        begTime: undefined,
        endTime: undefined
      };
      this.resetForm("form");
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.page = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    }
  }
};
</script>
