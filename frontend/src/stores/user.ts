import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, RegisterRequest } from '@/types'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref<string | null>(localStorage.getItem('token'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || 'ROLE_USER')

  async function login(loginData: LoginRequest) {
    const res = await authApi.login(loginData)
    if (res.token) {
      setToken(res.token)
      setUser(res.user)
      return true
    }
    return false
  }

  async function register(registerData: RegisterRequest) {
    await authApi.register(registerData)
    return true
  }

  function setUser(userData: User) {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    user,
    token,
    isLoggedIn,
    role,
    login,
    register,
    setUser,
    setToken,
    logout
  }
})
