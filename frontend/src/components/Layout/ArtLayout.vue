<template>
  <div class="art-layout">
    <Navbar :force-scrolled="forceNavbarScrolled" />
    <main class="main-content">
      <slot />
    </main>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import Navbar from './Navbar.vue'
import Footer from './Footer.vue'

defineProps<{
  forceNavbarScrolled?: boolean
}>()
</script>

<style scoped lang="scss">
.art-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg);
}

.main-content {
  flex: 1;
}
</style>
