import request from '@/utils/request'

export function analyzeRelations(dataSourceId) {
  return request({
    url: `/api/table-relations/analyze/${dataSourceId}`,
    method: 'post'
  })
}

export function getRelations(dataSourceId) {
  return request({
    url: `/api/table-relations/${dataSourceId}`,
    method: 'get'
  })
}

export function getRelationGraph(dataSourceId) {
  return request({
    url: `/api/table-relations/${dataSourceId}/graph`,
    method: 'get'
  })
}

export function getTableRelations(dataSourceId, tableName) {
  return request({
    url: `/api/table-relations/${dataSourceId}/table/${tableName}`,
    method: 'get'
  })
}

export function addManualRelation(data) {
  return request({
    url: '/api/table-relations',
    method: 'post',
    data
  })
}

export function deleteRelation(relationId) {
  return request({
    url: `/api/table-relations/${relationId}`,
    method: 'delete'
  })
}
