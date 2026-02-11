/**
 * 导出数据到 Excel
 * @param {Array} data - 要导出的数据
 * @param {string} filename - 文件名
 */
export function exportToExcel(data, filename = 'export') {
  if (!data || data.length === 0) {
    return
  }

  // 获取表头
  const headers = Object.keys(data[0])
  
  // 创建 CSV 内容
  const csvContent = [
    headers.join(','), // 表头
    ...data.map(row => 
      headers.map(header => {
        const value = row[header]
        // 处理包含逗号或引号的值
        if (typeof value === 'string' && (value.includes(',') || value.includes('"'))) {
          return `"${value.replace(/"/g, '""')}"`
        }
        return value
      }).join(',')
    )
  ].join('\n')

  // 创建 Blob
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  
  // 创建下载链接
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${filename}.csv`
  
  // 触发下载
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  // 释放 URL 对象
  URL.revokeObjectURL(link.href)
}

/**
 * 导出 JSON 数据
 * @param {Object|Array} data - 要导出的数据
 * @param {string} filename - 文件名
 */
export function exportToJSON(data, filename = 'export') {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${filename}.json`
  
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  URL.revokeObjectURL(link.href)
}
