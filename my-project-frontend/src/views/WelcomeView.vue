<script setup lang="ts">
</script>

<template>
  <div class="welcome-shell">
    <router-view v-slot="{ Component }">
      <transition name="fade">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style scoped>
.welcome-shell {
  position: relative;
  min-height: 100vh;
}
</style>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-active {
  position: absolute;
  inset: 0;
}

.fade-leave-active {
  position: absolute;
  inset: 0;
}

.fade-enter-from {
  opacity: 0;
}

.fade-leave-to {
  opacity: 0;
}
</style>
