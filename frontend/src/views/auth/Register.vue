<template>
  <div class="register-wrapper">
    <!-- 背景动画 -->
    <div class="background-blobs">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
    </div>

    <div class="register-container">
      <!-- 左侧品牌展示区 -->
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
            <h1 class="animate-text serif-text">Join the <i>Creative</i> Hub</h1>
            <h2 class="animate-text">加入 · 创造 · 共鸣</h2>
          </div>
          <p class="animate-text">在这里，你可以分享你的见解，发现有趣的灵魂，并利用我们的可视化工具让你的文章更具吸引力。</p>
          <div class="brand-features">
            <div class="feature-item animate-item">
              <div class="dot"></div>
              <span>全栈可视化组件库</span>
            </div>
            <div class="feature-item animate-item">
              <div class="dot"></div>
              <span>沉浸式创作工作台</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧注册表单区 -->
      <div class="form-section">
        <div class="glass-card auth-card">
          <div class="form-header">
            <h3>创建账号</h3>
            <p>开启您的智见之旅</p>
          </div>
          
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="register-form"
            @keyup.enter="handleRegister"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="设置用户名"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>

            <el-form-item prop="email">
              <el-input
                v-model="form.email"
                placeholder="设置邮箱"
                prefix-icon="Message"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="设置密码"
                prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="确认密码"
                prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="submit-btn"
                @click="handleRegister"
              >
                立 即 注 册
              </el-button>
            </el-form-item>
            
            <div class="login-link">
              已有账号？ <router-link to="/login">立即登录</router-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
    
    <!-- 底部备案信息 -->
    <div class="register-footer">
      <p>© 2026 松山湖可视化博客平台 | 粤ICP备12345678号</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validatePassword = (_rule: any, value: any, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      await authApi.register({
        username: form.username,
        email: form.email,
        password: form.password
      })
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error: any) {
      console.error('注册失败:', error)
      ElMessage.error(error.message || '注册失败，请稍后再试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.register-wrapper {
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

.register-container {
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

/* 注册表单区 */
.form-section {
  width: 460px;
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

.register-form {
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

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: var(--primary-700);
  border: none;
  transition: all 0.3s;
  margin-top: 10px;
  
  &:hover {
    background: var(--primary);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(14, 165, 233, 0.26);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.login-link {
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

/* 底部 */
.register-footer {
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
  
  .register-container {
    padding: 0;
  }
  
  .form-section {
    width: 100%;
    max-width: 460px;
    padding: 24px;
  }
  
  .auth-card {
    padding: 40px 32px;
  }
}
</style>
