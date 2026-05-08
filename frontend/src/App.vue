<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  try {
    localStorage.removeItem('ssl_theme_mode_v1')
    if (localStorage.getItem('theme') === 'dark') {
      localStorage.removeItem('theme')
    }
  } catch {
  }
  document.documentElement.removeAttribute('data-theme')
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>
