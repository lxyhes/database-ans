import request from '@/utils/request'

// 自然语言转 SQL
export function naturalLanguageToSQL(data) {
  return request({
    url: '/api/nl2sql/convert',
    method: 'post',
    data
  })
}

// 智能查询（转换+执行）
export function smartQuery(data) {
  return request({
    url: '/api/nl2sql/query',
    method: 'post',
    data
  })
}

// 解释 SQL
export function explainSQL(data) {
  return request({
    url: '/api/nl2sql/explain',
    method: 'post',
    data
  })
}

// 优化 SQL
export function optimizeSQL(data) {
  return request({
    url: '/api/nl2sql/optimize',
    method: 'post',
    data
  })
}
