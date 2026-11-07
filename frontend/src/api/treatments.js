import { request } from './client'

export function registerTreatment(payload) {
  return request('/api/treatments', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
