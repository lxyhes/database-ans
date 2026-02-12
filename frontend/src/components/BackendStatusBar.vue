<template>
  <Transition name="slide-down">
    <div v-if="!isConnected" class="backend-status-bar">
      <div class="status-content">
        <el-icon class="warning-icon"><WarningFilled /></el-icon>
        <span class="status-text">后端服务连接中断</span>
        <span class="last-check" v-if="lastCheckTime">
          最后检测: {{ formatTime(lastCheckTime) }}
        </span>
      </div>
      <div class="status-actions">
        <el-button size="small" @click="checkStatus" :loading="checking">
          <el-icon><Refresh /></el-icon>
          重新连接
        </el-button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useBackendStatus } from '@/composables/useBackendStatus'
import { WarningFilled, Refresh } from '@element-plus/icons-vue'

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
  background: linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 9999;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;

  .status-content {
    display: flex;
    align-items: center;
    gap: 10px;

    .warning-icon {
      font-size: 18px;
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
    .el-button {
      background: rgba(255, 255, 255, 0.2);
      border-color: rgba(255, 255, 255, 0.3);
      color: white;

      &:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    }
  }
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
