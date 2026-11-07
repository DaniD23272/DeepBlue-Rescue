import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeCaseStatus } from '../api/rescueCases'
import ErrorMessage from './ErrorMessage'

const STATUSES = [
  'ADMITTED',
  'UNDER_EVALUATION',
  'IN_REHABILITATION',
  'READY_FOR_RELEASE',
  'RELEASED',
  'CLOSED',
]

export default function ChangeStatus({ caseItem, onChanged }) {
  const [status, setStatus] = useState(caseItem.status ?? 'ADMITTED')
  const [message, setMessage] = useState('')
  const queryClient = useQueryClient()

  useEffect(() => {
    setStatus(caseItem.status ?? 'ADMITTED')
    setMessage('')
  }, [caseItem])

  const mutation = useMutation({
    mutationFn: ({ caseCode, status }) =>
      changeCaseStatus(caseCode, status),

    onSuccess: (result, variables) => {
      queryClient.invalidateQueries({ queryKey: ['rescue-cases'] })

      const updated =
        result?.data ?? result?.case ?? result

      onChanged({
        ...caseItem,
        ...(updated && typeof updated === 'object' ? updated : {}),
        status: variables.status,
      })

      setMessage('Estado actualizado correctamente.')
    },
  })

  const caseCode = caseItem.caseCode ?? caseItem.code ?? caseItem.id

  function handleSubmit(event) {
    event.preventDefault()
    setMessage('')

    if (caseCode == null) {
      setMessage('Este caso no tiene un código identificable.')
      return
    }

    mutation.mutate({ caseCode, status })
  }

  return (
    <section>
      <h3>Cambiar estado del caso</h3>

      <form onSubmit={handleSubmit}>
        <label>
          Nuevo estado
          <select value={status} onChange={(event) => setStatus(event.target.value)}>
            {STATUSES.map((item) => (
              <option key={item} value={item}>
                {item.replaceAll('_', ' ')}
              </option>
            ))}
          </select>
        </label>

        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Guardando...' : 'Actualizar estado'}
        </button>
      </form>

      {message && <p className="ok">{message}</p>}
      <ErrorMessage error={mutation.error} />
    </section>
  )
}
