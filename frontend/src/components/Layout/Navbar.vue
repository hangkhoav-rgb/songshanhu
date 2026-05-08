<template>
  <header class="navbar" :class="{ 'navbar-scrolled': isScrolled || forceScrolled }">
    <div class="container">
      <div class="logo-area" @click="router.push('/')">
        <div class="premium-logo">
          <div class="logo-mark">
            <span class="dot"></span>
            <span class="line"></span>
          </div>
          <span class="logo-text">SSL<i>.</i></span>
        </div>
      </div>
      
      <nav class="nav-links">
        <router-link to="/" class="nav-item" active-class="" exact-active-class="is-active">首页</router-link>
        <router-link to="/article" class="nav-item" active-class="is-active">文章广场</router-link>
        <router-link to="/dashboard" custom v-slot="{ navigate, isActive }">
          <button type="button" class="nav-item" :class="{ 'is-active': isActive }" @click="() => handleDashboardClick(navigate)">
            数据看板
          </button>
        </router-link>
      </nav>

      <div class="nav-search">
        <el-input
          v-model="navKeyword"
          size="default"
          clearable
          placeholder="搜索文章..."
          @keyup.enter="goSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="user-control">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-profile">
              <el-avatar :size="32" :src="userStore.user?.avatar">
                {{ userStore.user?.username?.[0]?.toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="custom-dropdown">
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item command="create">
                  <el-icon><EditPen /></el-icon>发布文章
                </el-dropdown-item>
                <el-dropdown-item command="mine">
                  <el-icon><Document /></el-icon>我的文章
                </el-dropdown-item>
                <el-dropdown-item command="actions">
                  <el-icon><Star /></el-icon>我的收藏/点赞
                </el-dropdown-item>
                <el-dropdown-item command="dashboard">
                  <el-icon><DataLine /></el-icon>我的看板
                </el-dropdown-item>
                <el-dropdown-item divided command="logout" class="logout-item">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <div class="auth-buttons">
            <el-button link @click="router.push('/login')">登录</el-button>
            <el-button type="primary" round @click="router.push('/register')">立即加入</el-button>
          </div>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { User, Document, Star } from '@element-plus/icons-vue'

const props = defineProps<{
  forceScrolled?: boolean
}>()

const router = useRouter()
const userStore = useUserStore()
const navKeyword = ref('')

function goSearch() {
  const kw = navKeyword.value.trim()
  router.push({ path: '/article', query: kw ? { keyword: kw } : {} })
}

function handleUserCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
    return
  }
  if (cmd === 'create') {
    router.push('/article/create')
    return
  }
  if (cmd === 'mine') {
    router.push('/article/mine')
    return
  }
  if (cmd === 'actions') {
    router.push('/article/actions')
    return
  }
  if (cmd === 'dashboard') {
    router.push('/dashboard')
    return
  }
  if (cmd === 'logout') {
    handleLogout()
  }
}
const isScrolled = ref(false)

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

const handleDashboardClick = (navigate?: () => void) => {
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
    if (navigate) {
      navigate()
    } else {
      router.push('/dashboard')
    }
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    roundButton: true
  }).then(() => {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped lang="scss">
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  z-index: var(--z-navbar);
  transition: all var(--motion-slow) var(--ease);
  background: var(--nav-bg);
  
  &.navbar-scrolled {
    background: var(--nav-bg-scrolled);
    backdrop-filter: saturate(180%) blur(18px);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    border-bottom: 1px solid var(--border);
    height: 64px;
  }
  
  .container {
    height: 100%;
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.logo-area {
  display: flex;
  align-items: center;
  cursor: pointer;
  
  .premium-logo {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--primary);

    .logo-mark {
      width: 28px;
      height: 28px;
      position: relative;
      background: var(--primary);
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .dot {
        width: 6px;
        height: 6px;
        background: white;
        border-radius: 50%;
      }
      
      .line {
        position: absolute;
        width: 2px;
        height: 12px;
        background: rgba(255, 255, 255, 0.5);
        right: 6px;
        bottom: 6px;
        transform: rotate(-45deg);
      }
    }

    .logo-text {
      font-family: 'Montserrat', sans-serif;
      font-size: 20px;
      font-weight: 900;
      letter-spacing: -0.5px;
      color: var(--text);
      i { color: var(--primary); font-style: normal; }
    }
  }
}

.nav-links {
  display: flex;
  gap: 32px;
  
  .nav-item {
    text-decoration: none;
    color: #262626;
    font-size: 16px;
    font-weight: 500;
    transition: background-color 0.3s, color 0.3s;
    cursor: pointer;
    border: none;
    background: none;
    padding: 0;
    position: relative;
    
    &:hover {
      color: #262626;
    }

    &.router-link-active,
    &.is-active {
      color: #262626;
    }

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -10px;
      height: 2px;
      border-radius: 2px;
      background: var(--nav-active);
      opacity: 0;
      transform: translateY(2px);
      transition: opacity 200ms ease-out, transform 200ms ease-out;
    }

    &:hover::after {
      opacity: 0.45;
      transform: translateY(0);
    }

    &.router-link-active::after,
    &.is-active::after {
      opacity: 1;
      transform: translateY(0);
    }
  }
}

.nav-search {
  width: 320px;
  margin: 0 18px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 20px;
  transition: background 0.3s;
  
  &:hover {
    background: rgba(0, 0, 0, 0.05);
  }
  
  .user-name {
    font-size: 14px;
    font-weight: 500;
  }
}

.auth-buttons {
  display: flex;
  gap: 12px;
}


@media (max-width: 992px) {
  .nav-search {
    display: none;
  }
}

.custom-dropdown {
  border-radius: 12px;
  padding: 8px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
}

.logout-item {
  color: var(--danger);
  &:hover {
    background-color: rgba(239, 68, 68, 0.10) !important;
    color: var(--danger) !important;
  }
}
</style>
