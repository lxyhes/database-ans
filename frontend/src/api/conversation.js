import request from '@/utils/request'

export function createConversation(data) {
  return request({
    url: '/api/conversations',
    method: 'post',
    data
  })
}

export function getConversations(dataSourceId) {
  return request({
    url: '/api/conversations',
    method: 'get',
    params: { dataSourceId }
  })
}

export function getConversation(sessionId) {
  return request({
    url: `/api/conversations/${sessionId}`,
    method: 'get'
  })
}

export function getConversationMessages(sessionId) {
  return request({
    url: `/api/conversations/${sessionId}/messages`,
    method: 'get'
  })
}

export function addMessage(sessionId, content) {
  return request({
    url: `/api/conversations/${sessionId}/messages`,
    method: 'post',
    data: { content }
  })
}

export function deleteConversation(sessionId) {
  return request({
    url: `/api/conversations/${sessionId}`,
    method: 'delete'
  })
}
