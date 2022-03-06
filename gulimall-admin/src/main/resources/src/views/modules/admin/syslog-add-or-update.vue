<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    :visible.sync="visible">
    <el-form :model="dataForm" :rules="dataRule" ref="dataForm" @keyup.enter.native="dataFormSubmit()" label-width="80px">
    <el-form-item label="用户名" prop="username">
      <el-input v-model="dataForm.username" placeholder="用户名"></el-input>
    </el-form-item>
    <el-form-item label="用户操作" prop="operation">
      <el-input v-model="dataForm.operation" placeholder="用户操作"></el-input>
    </el-form-item>
    <el-form-item label="请求方法" prop="method">
      <el-input v-model="dataForm.method" placeholder="请求方法"></el-input>
    </el-form-item>
    <el-form-item label="请求参数" prop="params">
      <el-input v-model="dataForm.params" placeholder="请求参数"></el-input>
    </el-form-item>
    <el-form-item label="执行时长(毫秒)" prop="time">
      <el-input v-model="dataForm.time" placeholder="执行时长(毫秒)"></el-input>
    </el-form-item>
    <el-form-item label="IP地址" prop="ip">
      <el-input v-model="dataForm.ip" placeholder="IP地址"></el-input>
    </el-form-item>
    <el-form-item label="创建时间" prop="createDate">
      <el-input v-model="dataForm.createDate" placeholder="创建时间"></el-input>
    </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
  export default {
    data () {
      return {
        visible: false,
        dataForm: {
          id: 0,
          username: '',
          operation: '',
          method: '',
          params: '',
          time: '',
          ip: '',
          createDate: ''
        },
        dataRule: {
          username: [
            { required: true, message: '用户名不能为空', trigger: 'blur' }
          ],
          operation: [
            { required: true, message: '用户操作不能为空', trigger: 'blur' }
          ],
          method: [
            { required: true, message: '请求方法不能为空', trigger: 'blur' }
          ],
          params: [
            { required: true, message: '请求参数不能为空', trigger: 'blur' }
          ],
          time: [
            { required: true, message: '执行时长(毫秒)不能为空', trigger: 'blur' }
          ],
          ip: [
            { required: true, message: 'IP地址不能为空', trigger: 'blur' }
          ],
          createDate: [
            { required: true, message: '创建时间不能为空', trigger: 'blur' }
          ]
        }
      }
    },
    methods: {
      init (id) {
        this.dataForm.id = id || 0
        this.visible = true
        this.$nextTick(() => {
          this.$refs['dataForm'].resetFields()
          if (this.dataForm.id) {
            this.$http({
              url: this.$http.adornUrl(`/admin/syslog/info/${this.dataForm.id}`),
              method: 'get',
              params: this.$http.adornParams()
            }).then(({data}) => {
              if (data && data.code === 0) {
                this.dataForm.username = data.sysLog.username
                this.dataForm.operation = data.sysLog.operation
                this.dataForm.method = data.sysLog.method
                this.dataForm.params = data.sysLog.params
                this.dataForm.time = data.sysLog.time
                this.dataForm.ip = data.sysLog.ip
                this.dataForm.createDate = data.sysLog.createDate
              }
            })
          }
        })
      },
      // 表单提交
      dataFormSubmit () {
        this.$refs['dataForm'].validate((valid) => {
          if (valid) {
            this.$http({
              url: this.$http.adornUrl(`/admin/syslog/${!this.dataForm.id ? 'save' : 'update'}`),
              method: 'post',
              data: this.$http.adornData({
                'id': this.dataForm.id || undefined,
                'username': this.dataForm.username,
                'operation': this.dataForm.operation,
                'method': this.dataForm.method,
                'params': this.dataForm.params,
                'time': this.dataForm.time,
                'ip': this.dataForm.ip,
                'createDate': this.dataForm.createDate
              })
            }).then(({data}) => {
              if (data && data.code === 0) {
                this.$message({
                  message: '操作成功',
                  type: 'success',
                  duration: 1500,
                  onClose: () => {
                    this.visible = false
                    this.$emit('refreshDataList')
                  }
                })
              } else {
                this.$message.error(data.msg)
              }
            })
          }
        })
      }
    }
  }
</script>
