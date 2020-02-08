import request from '@/utils/request'

// 查询模型列表
export function listModel(query) {
  return request({
    url: '/workflow/model/list',
    method: 'get',
    params: query
  })
}

// 新增模型
export function addModel(data) {
  return request({
    url: '/workflow/model/add',
    method: 'post',
    data: data
  })
}

// 部署模型
export function deployModel(modelId) {
  return request({
    url: '/workflow/model/deploy/' + modelId,
    method: 'get'
  })
}

// 删除模型
export function delModel(modelIds) {
  return request({
    url: '/workflow/model/' + modelIds,
    method: 'delete'
  })
}
