import { useAnimal } from '../hooks/useAnimal'
import ChangeStatus from './ChangeStatus'
import TreatmentList from './TreatmentList'
import TreatmentForm from './TreatmentForm'
import ErrorMessage from './ErrorMessage'

function unwrap(result) {
  return result?.data ?? result?.animal ?? result
}

export default function CaseDetail({ caseItem, onChanged }) {
  const animalCode =
    caseItem.animalCode ??
    caseItem.animal?.animalCode ??
    caseItem.animal?.code

  const { animal, treatments, eligibility } = useAnimal(animalCode)
  const animalData = unwrap(animal.data)
  const eligibilityData = unwrap(eligibility.data)

  return (
    <div>
      <section>
        <h2>Detalle del caso</h2>
        <p><strong>Código:</strong> {caseItem.caseCode ?? caseItem.code ?? caseItem.id ?? '—'}</p>
        <p><strong>Estado:</strong> {caseItem.status ?? '—'}</p>
        <p><strong>Fecha de rescate:</strong> {caseItem.rescueDate ?? '—'}</p>
        <p><strong>Ubicación:</strong> {caseItem.rescueLocation ?? '—'}</p>
        <p><strong>Código del animal:</strong> {animalCode ?? 'No disponible'}</p>
      </section>

      <ChangeStatus caseItem={caseItem} onChanged={onChanged} />

      <section>
        <h2>Información del animal</h2>
        {animal.isPending && <p>Cargando información del animal...</p>}
        <ErrorMessage error={animal.error} />

        {animalData && (
          <dl>
            {Object.entries(animalData).map(([key, value]) => (
              <div className="detail-row" key={key}>
                <dt>{key}</dt>
                <dd>
                  {value != null && typeof value === 'object'
                    ? JSON.stringify(value)
                    : String(value ?? '—')}
                </dd>
              </div>
            ))}
          </dl>
        )}
      </section>

      <section>
          {eligibility.isPending && <p>Consultando elegibilidad...</p>}

          {eligibility.isError && (
              <ErrorMessage error={eligibility.error} />
          )}

          {eligibility.data && (
              <p>
                  Puede recibir tratamiento:{' '}
                  <span className={eligibility.data.eligible ? 'ok' : 'bad'}>
      {eligibility.data.eligible ? 'Sí' : 'No'}
    </span>
              </p>
          )}
      </section>

      <section>
        <h2>Historial de tratamientos</h2>
        {treatments.isPending && <p>Cargando tratamientos...</p>}
        <ErrorMessage error={treatments.error} />

        {!treatments.isPending && !treatments.error && (
          <TreatmentList treatments={unwrap(treatments.data)} />
        )}
      </section>

      {animalCode ? (
        <TreatmentForm animalCode={animalCode} />
      ) : (
        <p>No se puede registrar un tratamiento porque el caso no tiene código de animal.</p>
      )}
    </div>
  )
}
