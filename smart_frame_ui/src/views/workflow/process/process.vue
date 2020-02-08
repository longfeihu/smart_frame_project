<template>
  <el-dialog :title="title" :visible.sync="isShow" :before-close="handleClose" width="80%">
    <div class="act-form">
      <iframe :src="src" ref="iframe"></iframe>
    </div>
    <div slot="footer" class="dialog-footer"></div>
  </el-dialog>
</template>

<script>
export default {
  name: 'processOn',
  props: {
    value: {
      type: Object,
      default: () => Object.create({})
    }
  },
  data() {
    return {
      isShow: true,
      title: '编辑模型',
      src: '/modeler.html?modelId=' + this.value.id
    }
  },
  watch: {
    'isShow': function() {
      this.$emit('input', null)
    }
  },
  methods: {
    handleClose(done) {
      this.$confirm('确认关闭？').then(() => {
          this.$emit('on-close')
          this.isShow = false
      }).catch(_ => {})
    }
  }
}
</script>
<style scoped>
  .act-form {
    width:90%;
    height:60%;
  }
  iframe {
    display: block; /* iframes are inline by default */
    background: #000;
    border: none; /* Reset default border */
    height: 100vh; /* Viewport-relative units */
    width: 77.2vw;
    height: 35vw;
  }
  .el-dialog {
    width:90%;
    height:60%;
    margin-top: 50px!important;
  }
</style>
