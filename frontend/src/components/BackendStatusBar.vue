<template>
  <a-collapse :active-key="!isConnected ? ['1'] : []">
    <a-collapse-item key="1" :show-expand-icon="false">
      <div class="backend-status-bar">
        <div class="status-content">
          <icon-exclamation-circle-fill size="18" class="warning-icon" />
          <span class="status-text">后端服务连接中断</span>
          <span v-if="lastCheckTime" class="last-check">
            最后检测: {{ formatTime(lastCheckTime) }}
          </span>
        </div>
        <div class="status-actions">
          <a-button size="small" @click="handleReconnect" :loading="checking">
            <template #icon><icon-refresh /></template>
            重新连接
          </a-button>
        </div>
      </div>
    </a-collapse-item>
  </a-collapse>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useBackendStatus } from '@/composables/useBackendStatus'
import { IconExclamationCircleFill, IconRefresh } from '@arco-design/web-vue/es/icon'

const { isConnected, lastCheckTime, checkStatus } = useBackendStatus()
const checking = ref(false)

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const handleReconnect = async () => {
  checking.value = true
  await checkStatus()
  checking.value = false
}
</script>

<style scoped lang="scss">
.backend-status-bar {
  height: 40px;
  background: linear-gradient(135deg, #f53f3f 0%, #ff7d00 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;

  .status-content {
    display: flex;
    align-items: center;
    gap: 10px;

    .warning-icon {
      animation: pulse 1.5s infinite;
    }

    .status-text {
      font-weight: 500;
      font-size: 14px;
    }

    .last-check {
      font-size: 12px;
      opacity: 0.8;
    }
  }

  .status-actions {
    :deep(.arco-btn) {
      background: rgba(255, 255, 255, 0.2);
      border-color: rgba(255, 255, 255, 0.3);
      color: white;

      &:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

:deep(.arco-collapse) {
  border: none;
  background: transparent;
}

:deep(.arco-collapse-item) {
  border: none;
}

:deep(.arco-collapse-item-header) {
  display: none;
}

:deep(.arco-collapse-item-content) {
  padding: 0;
  background: transparent;
}
</style>
