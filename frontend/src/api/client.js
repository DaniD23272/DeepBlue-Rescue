export class ApiError extends Error {
  constructor({ status, message, details }) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details ?? {}
  }
}

export async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)

    throw new ApiError({
      status: body?.status ?? response.status,
      message: body?.message ?? body?.detail ?? body?.error ?? 'No se pudo contactar el servidor',
      details: body?.details,
    })
  }

  if (response.status === 204) return null

  return response.json()
}
