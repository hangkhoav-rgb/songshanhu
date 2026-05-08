<template>
  <footer class="site-footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-brand">
          <div class="premium-logo">
            <div class="logo-mark">
              <span class="dot"></span>
              <span class="line"></span>
            </div>
            <span class="logo-text">SSL<i>.</i></span>
          </div>
          <p>基于 Spring Boot 3 & Vue 3 的多维可视化内容创作平台</p>
        </div>
        <div class="footer-links">
          <div class="link-group">
            <h4>产品</h4>
            <router-link to="/article">文章广场</router-link>
            <button type="button" class="footer-link" @click="handleDashboardClick">数据看板</button>
          </div>
          <div class="link-group">
            <h4>支持</h4>
            <a href="#">使用文档</a>
            <a href="#">常见问题</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2026 松山湖可视化博客平台 | 粤ICP备12345678号</p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const handleDashboardClick = () => {
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm(
      '数据看板仅对登录用户开放，请先登录以查看您的个性化数据。',
      '提示',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info',
        roundButton: true
      }
    ).then(() => {
      router.push('/login')
    }).catch(() => {})
  } else {
    router.push('/dashboard')
  }
}
</script>

<style scoped lang="scss">
.site-footer {
  background: var(--surface-2);
  padding: 80px 0 40px;
  border-top: 1px solid var(--border-soft);
  
  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
  }
}

.footer-content {
  display: flex;
  justify-content: space-between;
  margin-bottom: 60px;
}

.footer-brand {
  .premium-logo {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--primary);
    margin-bottom: 20px;

    .logo-mark {
      width: 24px;
      height: 24px;
      position: relative;
      background: var(--primary);
      border-radius: 5px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .dot {
        width: 5px;
        height: 5px;
        background: white;
        border-radius: 50%;
      }
      
      .line {
        position: absolute;
        width: 2px;
        height: 10px;
        background: rgba(255, 255, 255, 0.5);
        right: 5px;
        bottom: 5px;
        transform: rotate(-45deg);
      }
    }

    .logo-text {
      font-family: 'Montserrat', sans-serif;
      font-size: 18px;
      font-weight: 900;
      letter-spacing: -0.5px;
      color: var(--text);
      i { color: var(--primary); font-style: normal; }
    }
  }
  
  p {
    font-size: 14px;
    color: var(--muted);
    max-width: 240px;
    line-height: 1.6;
  }
}

.footer-links {
  display: flex;
  gap: 80px;
  
  .link-group {
    h4 {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 20px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    
    a,
    .footer-link {
      display: block;
      text-decoration: none;
      color: var(--muted);
      font-size: 14px;
      margin-bottom: 12px;
      transition: color 0.3s;
      cursor: pointer;
      border: none;
      background: transparent;
      padding: 0;
      text-align: left;
      
      &:hover {
        color: var(--primary);
      }
    }
  }
}

.footer-bottom {
  text-align: center;
  padding-top: 40px;
  border-top: 1px solid var(--border-soft);
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 768px) {
  .footer-content { flex-direction: column; gap: 40px; }
}
</style>
