<template>
  <a-breadcrumb>
    <a-breadcrumb-item @click="$router.push('/')">
      <icon-home />
      首页
    </a-breadcrumb-item>
    <a-breadcrumb-item v-if="$route.meta.title">
      {{ $route.meta.title }}
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<script setup lang="ts">
import { IconHome } from '@arco-design/web-vue/es/icon'
</script>
