import request from '@/utils/request'

// 查询审批记录
export function getAuditRecord(processInstanceId) {
  return request({
    url: '/workflow/task/record/' + processInstanceId,
    method: 'get'
  })
}

// 任务流程图
export function getDiagram(processInstanceId) {
  return request({
    url: '/workflow/process/diagram/' + processInstanceId,
    method: 'get'
  })
}

// 流程管理
export function unfinishedProcess(query) {
  return request({
    url: '/workflow/process/unfinishedProcess',
    method: 'get',
    params: query
  })
}
