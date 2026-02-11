import request from '@/utils/request'

// 获取所有数据源
export function getDataSources() {
  return request({
    url: '/api/datasources',
    method: 'get'
  })
}

// 获取单个数据源
export function getDataSource(id) {
  return request({
    url: `/api/datasources/${id}`,
    method: 'get'
  })
}

// 创建数据源
export function createDataSource(data) {
  return request({
    url: '/api/datasources',
    method: 'post',
    data
  })
}

// 更新数据源
export function updateDataSource(id, data) {
  return request({
    url: `/api/datasources/${id}`,
    method: 'put',
    data
  })
}

// 删除数据源
export function deleteDataSource(id) {
  return request({
    url: `/api/datasources/${id}`,
    method: 'delete'
  })
}

// 测试数据源连接
export function testDataSourceConnection(data) {
  return request({
    url: '/api/datasources/test',
    method: 'post',
    data
  })
}

// 设置默认数据源
export function setDefaultDataSource(id) {
  return request({
    url: `/api/datasources/${id}/default`,
    method: 'put'
  })
}

// 获取支持的数据库类型
export function getSupportedDatabaseTypes() {
  return request({
    url: '/api/datasources/types',
    method: 'get'
  })
}
