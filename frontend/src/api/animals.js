import { request } from './client'

export function getAnimal(animalCode) {
  return request(`/api/animals/${encodeURIComponent(animalCode)}`)
}

export function getAnimalTreatments(animalCode) {
  return request(`/api/animals/${encodeURIComponent(animalCode)}/treatments`)
}

export function getTreatmentEligibility(animalCode) {
  return request(`/api/animals/${encodeURIComponent(animalCode)}/treatment-eligibility`)
}
