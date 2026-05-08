<template>
  <div class="login-wrapper">
    <!-- 背景动画 -->
    <div class="background-blobs">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌展示区 (简约版) -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="premium-logo animate-text">
            <div class="logo-icon">
              <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="10" y="10" width="20" height="20" rx="2" stroke="currentColor" stroke-width="2.5"/>
                <circle cx="20" cy="20" r="4" fill="currentColor"/>
              </svg>
            </div>
            <span class="logo-text">SSL<i>.</i></span>
          </div>
          <div class="brand-hero">
            <h1 class="animate-text serif-text">Record <i>Every</i> Moment</h1>
            <h2 class="animate-text">记录 · 分享 · 见证</h2>
          </div>
          <p class="animate-text">在松山湖的波光粼粼中，捕捉文字的深度。这不仅是一个博客，更是一个将数据、地理与灵感无缝融合的数字艺术空间。</p>
          <div class="brand-features">
            <div class="feature-item animate-item">
              <div class="dot"></div>
              <span>动态数据流式渲染</span>
            </div>
            <div class="feature-item animate-item">
              <div class="dot"></div>
              <span>LBS 空间叙事交互</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="form-section">
        <div class="glass-card auth-card">
          <div class="form-header">
            <h3>账号登录</h3>
            <p>欢迎回来，请登录您的账号</p>
          </div>
          
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名或邮箱"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
            
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>
            
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="submit-btn"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
            
            <div class="register-link">
              还没有账号？ <router-link to="/register">立即注册</router-link>
            </div>

            <div v-if="false" class="third-party-login" />
          </el-form>
        </div>
      </div>
    </div>
    
    <!-- 底部备案信息 -->
    <div class="login-footer">
      <p>© 2026 松山湖可视化博客平台 | 粤ICP备12345678号</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const result = await authApi.login(form)
      if (result.mustResetPassword) {
        sessionStorage.setItem('first_reset_token', result.resetToken || '')
        ElMessage.warning('为保障安全，请先重置密码')
        router.push('/first-reset')
        return
      }
      if (!result.token) {
        throw new Error('登录失败：缺少令牌')
      }
      userStore.setToken(result.token)
      userStore.setUser(result.user)
      ElMessage.success('登录成功')
      router.push('/')
    } catch (error: any) {
      console.error('登录失败:', error)
      ElMessage.error(error.message || '登录失败，请检查用户名或密码')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--surface-2);
  position: relative;
  overflow: hidden;
}

/* 背景模糊色块动画 */
.background-blobs {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  
  .blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.5;
  }
  
  .blob-1 {
    width: 500px;
    height: 500px;
    background: rgba(14, 165, 233, 0.16);
    top: -100px;
    right: -100px;
    animation: blob-float 20s infinite alternate;
  }
  
  .blob-2 {
    width: 400px;
    height: 400px;
    background: rgba(16, 185, 129, 0.12);
    bottom: -100px;
    left: -100px;
    animation: blob-float 15s infinite alternate-reverse;
  }
  
  .blob-3 {
    width: 300px;
    height: 300px;
    background: rgba(245, 158, 11, 0.12);
    top: 40%;
    left: 20%;
    animation: blob-float 18s infinite alternate;
  }
}

@keyframes blob-float {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, 60px) scale(1.1); }
}

.login-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  z-index: 1;
}

/* 品牌展示区 */
.brand-section {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  
  .premium-logo {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 60px;
    color: var(--primary);

    .logo-icon {
      width: 40px;
      height: 40px;
      svg {
        width: 100%;
        height: 100%;
      }
    }

    .logo-text {
      font-family: 'Montserrat', sans-serif;
      font-size: 24px;
      font-weight: 900;
      letter-spacing: -1px;
      color: var(--text);
      
      i {
        color: var(--primary);
        font-style: normal;
      }
    }
  }

  .brand-hero {
    margin-bottom: 32px;
    
    h1 {
      font-family: 'Playfair Display', serif;
      font-size: 72px;
      font-weight: 700;
      color: var(--text);
      line-height: 1;
      margin: 0;
      letter-spacing: -2px;
      background: linear-gradient(135deg, #1d1d1f 0%, #434343 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      
      i {
        font-family: 'Playfair Display', serif;
        font-style: italic;
        font-weight: 400;
        color: var(--primary);
        -webkit-text-fill-color: var(--primary);
      }
    }
    
    h2 {
      font-size: 24px;
      font-weight: 500;
      color: var(--primary);
      margin-top: 12px;
      letter-spacing: 8px;
      text-transform: uppercase;
    }
  }
  
  p {
    font-size: 18px;
    color: rgba(15, 23, 42, 0.78);
    line-height: 1.8;
    margin-bottom: 48px;
    max-width: 500px;
    font-weight: 400;
  }
}

.animate-text {
  animation: fadeInUp 0.8s ease-out forwards;
  opacity: 0;
}

.brand-hero h1.animate-text { animation-delay: 0.2s; }
.brand-hero h2.animate-text { animation-delay: 0.3s; }
p.animate-text { animation-delay: 0.4s; }

.brand-features {
  .feature-item {
    display: flex;
    align-items: center;
    margin-bottom: 24px;
    color: var(--muted);
    
    .dot {
      width: 6px;
      height: 6px;
      background: var(--primary);
      border-radius: 50%;
      margin-right: 16px;
      box-shadow: 0 0 12px rgba(14, 165, 233, 0.30);
    }
    
    span {
      font-size: 16px;
      font-weight: 500;
      letter-spacing: 1px;
    }
  }
}

.animate-item {
  animation: fadeInUp 0.8s ease-out forwards;
  opacity: 0;
  &:nth-child(1) { animation-delay: 0.6s; }
  &:nth-child(2) { animation-delay: 0.7s; }
}

@keyframes fadeInUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* 登录表单区 */
.form-section {
  width: 440px;
}

.auth-card {
  padding: 48px;
  border-radius: 24px;
}

.form-header {
  margin-bottom: 32px;
  
  h3 {
    font-size: 24px;
    font-weight: 700;
    color: var(--text);
    margin-bottom: 8px;
  }
  
  p {
    font-size: 14px;
    color: var(--muted);
  }
}

.login-form {
  :deep(.el-input__wrapper) {
    background-color: rgba(245, 245, 247, 0.6);
    box-shadow: none !important;
    border: 1px solid transparent;
    transition: all 0.3s;
    border-radius: 12px;
    padding: 4px 15px;
    
    &:hover {
      background-color: rgba(245, 245, 247, 0.8);
    }
    
    &.is-focus {
      background-color: #ffffff;
      border-color: var(--primary);
    }
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 24px;
  
  :deep(.el-checkbox__label) {
    color: var(--muted);
    font-size: 13px;
  }
  
  .el-link {
    font-size: 13px;
    font-weight: 500;
  }
}

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: var(--primary-700);
  border: none;
  transition: all 0.3s;
  
  &:hover {
    background: var(--primary);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(14, 165, 233, 0.26);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.register-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--muted);
  
  a {
    color: var(--primary-700);
    text-decoration: none;
    font-weight: 600;
    margin-left: 4px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.third-party-login {
  margin-top: 40px;
  
  .divider {
    display: flex;
    align-items: center;
    margin-bottom: 24px;
    
    &::before, &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: rgba(0, 0, 0, 0.05);
    }
    
    span {
      padding: 0 16px;
      font-size: 12px;
      color: var(--muted);
    }
  }
}

.login-icons {
  display: flex;
  justify-content: center;
  gap: 24px;
  
  .icon-circle {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: var(--surface);
    border: 1px solid rgba(0, 0, 0, 0.05);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    color: rgba(15, 23, 42, 0.78);
    
    &:hover {
      border-color: var(--primary);
      color: var(--primary);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }
  }
}

/* 底部 */
.login-footer {
  padding: 32px;
  text-align: center;
  color: var(--muted);
  font-size: 12px;
  position: relative;
  z-index: 1;
}

/* 响应式适配 */
@media (max-width: 960px) {
  .brand-section {
    display: none;
  }
  
  .login-container {
    padding: 0;
  }
  
  .form-section {
    width: 100%;
    max-width: 440px;
    padding: 24px;
  }
  
  .auth-card {
    padding: 40px 32px;
  }
}
</style>
