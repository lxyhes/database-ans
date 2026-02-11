import request from '@/utils/request'

export function scanTable(dataSourceId, tableName) {
  return request({
    url: `/api/sensitive-data/scan/${dataSourceId}`,
    method: 'post',
    data: { tableName }
  })
}

export function getSensitiveColumns(dataSourceId) {
  return request({
    url: `/api/sensitive-data/${dataSourceId}`,
    method: 'get'
  })
}

export function getTableSensitiveColumns(dataSourceId, tableName) {
  return request({
    url: `/api/sensitive-data/${dataSourceId}/table/${tableName}`,
    method: 'get'
  })
}

export function getSensitiveDataOverview(dataSourceId) {
  return request({
    url: `/api/sensitive-data/${dataSourceId}/overview`,
    method: 'get'
  })
}

export function updateMaskConfig(columnId, isMasked, maskRule) {
  return request({
    url: `/api/sensitive-data/config/${columnId}`,
    method: 'put',
    data: { isMasked, maskRule }
  })
}

export function deleteSensitiveColumn(columnId) {
  return request({
    url: `/api/sensitive-data/${columnId}`,
    method: 'delete'
  })
}
