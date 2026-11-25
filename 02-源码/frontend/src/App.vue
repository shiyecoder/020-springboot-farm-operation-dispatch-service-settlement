<template>
  <!-- 登录页独立布局，其余页面统一走后台管理外壳（侧边栏 + 顶栏 + 内容区） -->
  <router-view v-if="$route.path === '/login'" />
  <MainLayout v-else />
</template>

<script setup>
import MainLayout from './layout/MainLayout.vue'
</script>
