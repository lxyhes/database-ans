import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'

const isConnected = ref(true)
const lastCheckTime = ref<Date | null>(null)
const checkInterval = ref<number | null>(null)

export function useBackendStatus() {
  const checkStatus = async () => {
    try {
      await request.get('/api/health/ping', { timeout: 3000 })
      isConnected.value = true
      lastCheckTime.value = new Date()
    } catch (error) {
      isConnected.value = false
      lastCheckTime.value = new Date()
    }
  }

  const startMonitoring = (intervalMs: number = 30000) => {
    checkStatus()
    checkInterval.value = window.setInterval(checkStatus, intervalMs)
  }

  const stopMonitoring = () => {
    if (checkInterval.value) {
      clearInterval(checkInterval.value)
      checkInterval.value = null
    }
  }

  return {
    isConnected,
    lastCheckTime,
    checkStatus,
    startMonitoring,
    stopMonitoring
  }
}
