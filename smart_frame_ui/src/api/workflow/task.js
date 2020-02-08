import request from '@/utils/request'

// 查询待办任务列表
export function todoTasks(data) {
  return request({
    url: '/workflow/task/todoTasks',
    method: 'get',
    params: data
  })
}

// 签收任务
export function signTask(taskId) {
  return request({
    url: '/workflow/task/sign/' + taskId,
    method: 'get',
    params: data
  })
}
