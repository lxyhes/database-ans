import request from '@/utils/request'

export function createTemplate(data) {
  return request({
    url: '/api/reports/templates',
    method: 'post',
    data
  })
}

export function updateTemplate(id, data) {
  return request({
    url: `/api/reports/templates/${id}`,
    method: 'put',
    data
  })
}

export function getTemplates(dataSourceId) {
  return request({
    url: '/api/reports/templates',
    method: 'get',
    params: { dataSourceId }
  })
}

export function getTemplate(id) {
  return request({
    url: `/api/reports/templates/${id}`,
    method: 'get'
  })
}

export function deleteTemplate(id) {
  return request({
    url: `/api/reports/templates/${id}`,
    method: 'delete'
  })
}

export function executeReport(id) {
  return request({
    url: `/api/reports/templates/${id}/execute`,
    method: 'post'
  })
}

export function getReportInstances(id) {
  return request({
    url: `/api/reports/templates/${id}/instances`,
    method: 'get'
  })
}

export function downloadReport(instanceId) {
  return request({
    url: `/api/reports/download/${instanceId}`,
    method: 'get',
    responseType: 'blob'
  })
}
