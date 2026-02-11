import request from '@/utils/request'

export function checkTableQuality(dataSourceId, tableName) {
  return request({
    url: `/api/data-quality/check/${dataSourceId}`,
    method: 'get',
    params: { tableName }
  })
}
