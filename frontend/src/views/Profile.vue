<template>
  <ArtLayout force-navbar-scrolled>
    <div class="profile-container site-container">
      <div class="glass-card profile-card">
        <header class="header">
          <h1 class="serif-text">个人资料</h1>
          <p>管理头像、昵称与个人信息</p>
        </header>

        <el-form label-position="top" :model="form" class="form" v-loading="loading">
          <div class="avatar-row">
            <div class="avatar-preview">
              <el-avatar :size="88" :src="avatarPreview || profile?.avatarUrls?.lg">
                {{ (profile?.nickname || profile?.username || 'U')[0] }}
              </el-avatar>
            </div>
            <div class="avatar-actions">
              <input ref="fileInputRef" class="file-input" type="file" accept="image/*" @change="onPickFile" />
              <el-button type="primary" plain @click="fileInputRef?.click()">选择头像</el-button>
              <el-button
                type="primary"
                :disabled="!avatarFile"
                :loading="uploading"
                @click="uploadAvatar"
              >
                上传
              </el-button>
              <div class="tips">支持 JPG/PNG/WebP，最大 2MB</div>
            </div>
          </div>

          <el-form-item label="昵称">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit placeholder="2-20 字，支持中文/字母/数字/下划线" />
          </el-form-item>

          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio-button label="secret">保密</el-radio-button>
              <el-radio-button label="male">男</el-radio-button>
              <el-radio-button label="female">女</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="个人简介">
            <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="一句话介绍你自己" />
          </el-form-item>

          <el-form-item label="扩展字段（JSON，可选）">
            <el-input v-model="form.extra" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder='{"city":"东莞"}' />
          </el-form-item>

          <div class="actions">
            <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import { userApi, type UserProfile, type ProfileUpdateRequest } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const profile = ref<UserProfile | null>(null)

const form = reactive<ProfileUpdateRequest>({
  nickname: '',
  gender: 'secret',
  bio: '',
  extra: ''
})

const fileInputRef = ref<HTMLInputElement>()
const avatarFile = ref<File | null>(null)
const avatarPreview = ref<string>('')

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await userApi.getProfile()
    profile.value = res
    form.nickname = res.nickname || ''
    form.gender = res.gender || 'secret'
    form.bio = res.bio || ''
    form.extra = res.extra || ''
  } catch (e) {
    ElMessage.error('加载个人资料失败')
  } finally {
    loading.value = false
  }
}

const onPickFile = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB')
    input.value = ''
    return
  }
  avatarFile.value = await compressToSquareJpeg(file, 512, 0.88)
  avatarPreview.value = URL.createObjectURL(avatarFile.value)
}

const uploadAvatar = async () => {
  if (!avatarFile.value) return
  uploading.value = true
  try {
    const urls = await userApi.uploadAvatar(avatarFile.value)
    const v = `&v=${Date.now()}`
    if (profile.value) {
      profile.value.avatarUrls = {
        ...urls,
        lg: urls.lg ? urls.lg + v : urls.lg,
        md: urls.md ? urls.md + v : urls.md,
        sm: urls.sm ? urls.sm + v : urls.sm,
        orig: urls.orig ? urls.orig + v : urls.orig
      }
    }
    if (userStore.user && urls.lg) {
      userStore.setUser({ ...userStore.user, avatar: urls.lg + v })
    }
    avatarPreview.value = ''
    avatarFile.value = null
    ElMessage.success('头像已更新')
  } catch (e) {
    ElMessage.error('头像上传失败')
  } finally {
    uploading.value = false
  }
}

const save = async () => {
  saving.value = true
  try {
    await userApi.updateProfile(form)
    await fetchProfile()
    if (userStore.user) {
      userStore.setUser({
        ...userStore.user,
        nickname: form.nickname || userStore.user.nickname
      })
    }
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const readImage = (file: File) =>
  new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = URL.createObjectURL(file)
  })

const compressToSquareJpeg = async (file: File, size: number, quality: number) => {
  const img = await readImage(file)
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!

  const sw = img.naturalWidth
  const sh = img.naturalHeight
  const side = Math.min(sw, sh)
  const sx = Math.floor((sw - side) / 2)
  const sy = Math.floor((sh - side) / 2)

  ctx.drawImage(img, sx, sy, side, side, 0, 0, size, size)

  const blob = await new Promise<Blob>((resolve) =>
    canvas.toBlob((b) => resolve(b!), 'image/jpeg', quality)
  )
  return new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
}

onMounted(fetchProfile)
</script>

<style scoped lang="scss">
.profile-container {
  padding-top: 110px;
  padding-bottom: 70px;
  max-width: 1200px;
}

.profile-card {
  padding: 22px;
}

.header {
  text-align: center;
  margin-bottom: 18px;
  h1 { font-size: 26px; color: var(--text); margin-bottom: 6px; }
  p { color: #606266; }
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 22px;
  .file-input { display: none; }
  .tips { margin-top: 8px; color: var(--muted); font-size: 12px; }
}

.actions {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.serif-text { font-family: 'Playfair Display', serif; }

@media (max-width: 576px) {
  .glass-card { padding: 22px; }
  .avatar-row { flex-direction: column; align-items: flex-start; }
}
</style>
