import request from './request'
import { Message } from '@arco-design/web-vue'

/**
 * 心跳检测管理器
 * 定期检查后端服务状态
 */
class HeartbeatManager {
  constructor() {
    this.interval = null
    this.heartbeatInterval = 30000 // 默认 30 秒检测一次
    this.isOnline = true
    this.listeners = []
  }

  /**
   * 开始心跳检测
   */
  start(interval = 30000) {
    this.heartbeatInterval = interval
    this.stop() // 先停止之前的
    
    // 立即执行一次
    this.check()
    
    // 定时检测
    this.interval = setInterval(() => {
      this.check()
    }, this.heartbeatInterval)
    
    console.log('Heartbeat started, interval:', interval)
  }

  /**
   * 停止心跳检测
   */
  stop() {
    if (this.interval) {
      clearInterval(this.interval)
      this.interval = null
      console.log('Heartbeat stopped')
    }
  }

  /**
   * 执行心跳检测
   */
  async check() {
    try {
      const response = await request.get('/api/health/ping', {
        timeout: 5000 // 心跳检测超时 5 秒
      })
      
      if (response.status === 'ok') {
        if (!this.isOnline) {
          // 从离线恢复到在线
          this.isOnline = true
          this.notifyListeners('online')
          Message.success('服务连接恢复')
        }
      }
    } catch (error) {
      if (this.isOnline) {
        // 从在线变为离线
        this.isOnline = false
        this.notifyListeners('offline')
        console.warn('Heartbeat failed:', error.message)
      }
    }
  }

  /**
   * 获取系统状态
   */
  async getStatus() {
    try {
      const response = await request.get('/api/health/status')
      return response
    } catch (error) {
      console.error('Failed to get status:', error)
      return null
    }
  }

  /**
   * 添加状态变化监听器
   */
  addListener(callback) {
    this.listeners.push(callback)
  }

  /**
   * 移除状态变化监听器
   */
  removeListener(callback) {
    const index = this.listeners.indexOf(callback)
    if (index > -1) {
      this.listeners.splice(index, 1)
    }
  }

  /**
   * 通知所有监听器
   */
  notifyListeners(status) {
    this.listeners.forEach(callback => {
      try {
        callback(status)
      } catch (e) {
        console.error('Listener error:', e)
      }
    })
  }

  /**
   * 获取当前在线状态
   */
  getOnlineStatus() {
    return this.isOnline
  }
}

// 导出单例
export const heartbeat = new HeartbeatManager()
export default heartbeat
