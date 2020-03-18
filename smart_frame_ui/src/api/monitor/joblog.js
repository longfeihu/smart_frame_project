import request from '@/utils/request'

// 查询定时任务调度日志列表
export function listLog(query) {
  return request({
    url: '/monitor/joblog/list',
    method: 'get',
    params: query
  })
}

// 删除定时任务调度日志
export function delLog(jobLogIds) {
  return request({
    url: '/monitor/joblog/' + jobLogIds,
    method: 'delete'
  })
}

// 清空定时任务调度日志
export function cleanLog() {
  return request({
    url: '/monitor/joblog/clean',
    method: 'post'
  })
}

// 导出定时任务调度日志
export function exportLog(query) {
  return request({
    url: '/monitor/joblog/export',
    method: 'get',
    params: query
  })
}
