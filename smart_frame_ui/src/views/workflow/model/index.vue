<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true">
      <el-form-item label="模型名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入模型名称"
          clearable
          size="small"
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模型KEY" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="请输入权模型KEY"
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['workflow:model:add']"
        >新建模型</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['workflow:model:remove']"
        >删除模型</el-button>
      </el-col>
    </el-row>

    <el-table border v-loading="loading" :data="modelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模型ID" prop="id" align="center" width="80" />
      <el-table-column label="模型名称" prop="name" align="center" width="120" />
      <el-table-column label="模型KEY" prop="key" :show-overflow-tooltip="true" align="center" width="180" />
      <el-table-column label="模型版本" prop="version" align="center" width="100" />
      <el-table-column label="模型状态" align="center" prop="deploymentId" :formatter="deploymentIdFormat" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="lastUpdateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.lastUpdateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="onEditBtnClick(scope.row)"
            v-hasPermi="['workflow:model:edit']"
          >编辑模型</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-check"
            @click="handleDeploy(scope.row.id)"
          >部署模型</el-button>
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

    <!-- 添加或修改模型对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="模型KEY" prop="key">
          <el-input v-model="form.key" placeholder="请输入模型KEY" />
        </el-form-item>
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 绘制模型 -->
    <processOn v-if="editProcess" v-model="editProcess" @on-close="handleProcessClose" />
  </div>
</template>

<script>
import { listModel, delModel, addModel, deployModel } from "@/api/workflow/model";
import processOn from '../process/process'

export default {
  name: "Model",
  components: {
    processOn
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
      modelList: [],
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
        key: undefined
      },
      editProcess: undefined,
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "模型名称不能为空", trigger: "blur" }
        ],
        key: [
          { required: true, message: "模型KEY不能为空", trigger: "blur" }
        ]
      },
      defaultProps: {
        children: "children",
        label: "label"
      }
    };
  },
  created() {
    this.getList();
    this.getDicts("wf_model_status").then(response => {
      this.statusOptions = response.data;
    });
  },
  methods: {
    /** 查询模型列表 */
    getList() {
      this.loading = true;
      listModel(this.addDateRange(this.queryParams, this.dateRange)).then(
        response => {
          this.modelList = response.rows;
          this.total = response.total;
          this.loading = false;
        }
      );
    },
    // 字典状态字典翻译
    deploymentIdFormat(row, column) {
      return row.deploymentId ? "已部署" : "未部署";
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        modelId: undefined,
        name: undefined,
        key: undefined,
        description: undefined
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
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新建模型";
    },
    /** 提交按钮 */
    submitForm: function() {
      // 表单校验
      this.$refs["form"].validate(valid => {
        // 校验通过
        if (valid) {
          // 如果是新增
          if (this.form.modelId == undefined) {
            addModel(this.form).then(response => {
              if (response.code === 200) {
                this.msgSuccess("新增模型成功");
                this.open = false;
                this.getList();
              } else {
                this.msgError(response.msg);
              }
            });
          } else {
            // 修改模型
          }
        }
      });
    },
    handleDeploy: function(modelId) {
      // 如果不为空
      if (modelId) {
        // 部署
        deployModel(modelId).then(response => {
          if (response.code === 200) {
            this.msgSuccess("部署成功");
            this.getList();
          } else {
            this.msgError(response.msg);
          }
        });
      }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const modelIds = row.id || this.ids;
      this.$confirm('是否确认删除模型ID为"' + modelIds + '"的数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(function() {
          return delModel(modelIds);
        }).then(() => {
          this.getList();
          this.msgSuccess("删除成功");
        }).catch(function() {});
    }
  }
};
</script>
