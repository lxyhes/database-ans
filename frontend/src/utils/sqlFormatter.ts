/**
 * SQL 格式化工具
 * 简单的 SQL 格式化实现，无需额外依赖
 */

export function formatSql(sql: string): string {
  if (!sql) return ''
  
  // 标准化空白字符
  let formatted = sql.trim()
  
  // SQL 关键字列表
  const keywords = [
    'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT', 'IN', 'EXISTS',
    'JOIN', 'LEFT', 'RIGHT', 'INNER', 'OUTER', 'ON', 'AS',
    'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'OFFSET',
    'INSERT INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE FROM',
    'CREATE TABLE', 'ALTER TABLE', 'DROP TABLE', 'INDEX',
    'UNION', 'UNION ALL', 'INTERSECT', 'EXCEPT',
    'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
    'DISTINCT', 'COUNT', 'SUM', 'AVG', 'MAX', 'MIN',
    'INNER JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'FULL JOIN', 'CROSS JOIN',
    'WITH', 'RECURSIVE'
  ]
  
  // 在主要关键字前添加换行
  const majorKeywords = [
    'SELECT', 'FROM', 'WHERE', 'GROUP BY', 'ORDER BY', 'HAVING',
    'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'JOIN', 'ON',
    'AND', 'OR', 'UNION', 'UNION ALL', 'LIMIT', 'OFFSET'
  ]
  
  // 先标准化空格
  formatted = formatted.replace(/\s+/g, ' ')
  
  // 在主要关键字前换行
  majorKeywords.forEach(keyword => {
    const regex = new RegExp(`\\s${keyword}\\s`, 'gi')
    formatted = formatted.replace(regex, `\n${keyword} `)
  })
  
  // 处理 SELECT 后的字段列表
  formatted = formatted.replace(/SELECT\s+/gi, 'SELECT\n  ')
  
  // 处理逗号分隔的字段
  formatted = formatted.replace(/,\s*/g, ',\n  ')
  
  // 清理多余的缩进
  formatted = formatted.replace(/\n\s+\n/g, '\n')
  
  // 关键字大写
  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b${keyword}\\b`, 'gi')
    formatted = formatted.replace(regex, keyword)
  })
  
  // 处理子查询的括号
  let indentLevel = 0
  const lines = formatted.split('\n')
  const indentedLines = lines.map(line => {
    const trimmedLine = line.trim()
    if (trimmedLine.startsWith(')')) {
      indentLevel = Math.max(0, indentLevel - 1)
    }
    const indentedLine = '  '.repeat(indentLevel) + trimmedLine
    if (trimmedLine.endsWith('(')) {
      indentLevel++
    }
    return indentedLine
  })
  
  formatted = indentedLines.join('\n')
  
  // 最终清理
  formatted = formatted
    .replace(/\n\s*\n/g, '\n')
    .trim()
  
  return formatted
}

/**
 * 高亮 SQL 关键字
 */
export function highlightSql(sql: string): string {
  if (!sql) return ''
  
  const keywords = [
    'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT', 'IN', 'EXISTS',
    'JOIN', 'LEFT', 'RIGHT', 'INNER', 'OUTER', 'ON', 'AS',
    'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'OFFSET',
    'INSERT INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE FROM',
    'UNION', 'UNION ALL', 'DISTINCT', 'BETWEEN', 'LIKE', 'IS NULL', 'IS NOT NULL',
    'COUNT', 'SUM', 'AVG', 'MAX', 'MIN', 'COALESCE', 'NULLIF',
    'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'IF', 'IFNULL',
    'ASC', 'DESC', 'NULL', 'TRUE', 'FALSE'
  ]
  
  let highlighted = sql
  
  // 高亮关键字
  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b${keyword}\\b`, 'gi')
    highlighted = highlighted.replace(regex, `<span class="sql-keyword">${keyword}</span>`)
  })
  
  // 高亮字符串
  highlighted = highlighted.replace(/'([^']*)'/g, '<span class="sql-string">\'$1\'</span>')
  
  // 高亮数字
  highlighted = highlighted.replace(/\b(\d+)\b/g, '<span class="sql-number">$1</span>')
  
  // 高亮注释
  highlighted = highlighted.replace(/(--.*$)/gm, '<span class="sql-comment">$1</span>')
  highlighted = highlighted.replace(/(\/\*[\s\S]*?\*\/)/g, '<span class="sql-comment">$1</span>')
  
  return highlighted
}
