<template>
  <el-dialog :title="title" :visible.sync="isShow" :before-close="onOkBtnClick" :append-to-body="true">
    <el-table :data="approveList" border stripe height="200px">
      <el-table-column align="center" label="任务名称">
        <template slot-scope="scope">
          <span>{{ scope.row.taskName }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="审批时间">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.approveTime) }}</span>
        </template>
      </el-table-column>

      <el-table-column align="center" label="审批人">
        <template slot-scope="scope">
          <span>{{ scope.row.approveName }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="审批操作">
        <template slot-scope="scope">
          <span>{{ scope.row.approveAdvice }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="审批意见">
        <template slot-scope="scope">
          <span>{{ scope.row.approveComment }}</span>
        </template>
      </el-table-column>
    </el-table>
    <img :src="pic" style="height:auto; width:100%">
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="onOkBtnClick">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getAuditRecord, getDiagram } from '@/api/workflow/process'
export default {
  name: 'DoTask',
  props: {
    value: {
      type: Object,
      default: () => Object.create({})
    }
  },
  data() {
    return {
      isShow: true,
      title: this.value.workflowName,
      pic: null,
      approveList: null
    }
  },
  watch: {
    'isShow': function() {
      this.$emit('input', null)
    }
  },
  created() {
    this.isShow = true
    this.init(this.value.processInstanceId)
  },
  methods: {
    init(processInstanceId) {
      this.getDetail(processInstanceId)
    },
    async getDetail(processInstanceId) {
      const res = await getApproveRecords(processInstanceId)
      const pic = await getProcessPic(processInstanceId)
      this.approveList = res.data.approveRecordList
      this.pic = 'data:image/png;base64,' + btoa(new Uint8Array(pic.data).reduce((data, byte) => data + String.fromCharCode(byte), ''))
    },
    onOkBtnClick() {
      this.$emit('on-close')
      this.isShow = false
    }
  }
}
</script>
