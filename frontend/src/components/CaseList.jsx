import { useState } from 'react'
import { useRescueCases } from '../hooks/useRescueCases'
import ErrorMessage from './ErrorMessage'

const STATUSES = [
  'ADMITTED',
  'UNDER_EVALUATION',
  'IN_REHABILITATION',
  'READY_FOR_RELEASE',
  'RELEASED',
  'CLOSED',
]

function getItems(result) {
  const value = result?.data ?? result?.content ?? result?.items ?? result
  return Array.isArray(value) ? value : []
}

function getCode(item) {
  return item.caseCode ?? item.code ?? item.id
}

export default function CaseList({ selected, onSelect }) {
  const [status, setStatus] = useState('ADMITTED')
  const { data, isPending, error, refetch, isFetching } =
    useRescueCases(status)

  const cases = getItems(data)

  return (
    <aside>
      <h2>Casos de rescate</h2>

      <label>
        Filtrar por estado
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          {STATUSES.map((item) => (
            <option key={item} value={item}>
              {item.replaceAll('_', ' ')}
            </option>
          ))}
        </select>
      </label>

      <button type="button" onClick={() => refetch()} disabled={isFetching}>
        {isFetching ? 'Actualizando...' : 'Actualizar casos'}
      </button>

      {isPending && <p>Cargando casos...</p>}
      <ErrorMessage error={error} />

      {!isPending && !error && cases.length === 0 && (
        <p>No se encontraron casos para este estado.</p>
      )}

      {cases.map((item, index) => {
        const code = getCode(item)
        const active =
          code != null && String(code) === String(getCode(selected ?? {}))

        return (
          <button
            type="button"
            key={code ?? index}
            className={`case-item ${active ? 'active' : ''}`}
            onClick={() => onSelect(item)}
          >
            <strong>{code ?? `Caso ${index + 1}`}</strong>
            <br />
            <span>{item.animalCode ?? item.animal?.animalCode ?? 'Animal sin código'}</span>
            <br />
            <small>{item.status ?? status}</small>
          </button>
        )
      })}
    </aside>
  )
}
