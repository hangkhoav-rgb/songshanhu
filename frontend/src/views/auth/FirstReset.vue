<template>
  <div class="first-reset-wrapper">
    <div class="card glass-card">
      <h1 class="title">首次重置密码</h1>
      <div class="muted desc">检测到你的账号使用了历史弱密码，请先设置 6–32 位新密码后再进入系统。</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" size="large" />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" size="large" />
        </el-form-item>

        <el-button type="primary" size="large" :loading="loading" class="submit" @click="handleSubmit">保存并登录</el-button>
        <div class="actions">
          <el-button link @click="backToLogin">返回登录</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6–32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, cb) => {
        if (value !== form.newPassword) cb(new Error('两次输入的密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

function backToLogin() {
  sessionStorage.removeItem('first_reset_token')
  router.push('/login')
}

async function handleSubmit() {
  if (!formRef.value) return
  const resetToken = sessionStorage.getItem('first_reset_token') || ''
  if (!resetToken) {
    ElMessage.error('缺少重置令牌，请重新登录')
    router.push('/login')
    return
  }

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const result = await authApi.firstReset({ resetToken, newPassword: form.newPassword })
      userStore.setToken(result.token)
      userStore.setUser(result.user)
      sessionStorage.removeItem('first_reset_token')
      ElMessage.success('密码已更新')
      router.push('/')
    } catch (e: any) {
      ElMessage.error(e?.message || '重置失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.first-reset-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--surface-2);
}

.card {
  width: 100%;
  max-width: 520px;
  padding: 22px;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
}

.desc {
  margin-top: 10px;
  font-size: 13px;
}

.form {
  margin-top: 18px;
}

.submit {
  width: 100%;
  height: 46px;
  border-radius: 12px;
}

.actions {
  margin-top: 10px;
  display: flex;
  justify-content: center;
}
</style>

