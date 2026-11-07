import { request } from './client'

export function getCasesByStatus(status) {
  return request(`/api/rescue-cases?status=${encodeURIComponent(status)}`)
}

export function changeCaseStatus(caseCode, status) {
  return request(`/api/rescue-cases/${encodeURIComponent(caseCode)}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}
