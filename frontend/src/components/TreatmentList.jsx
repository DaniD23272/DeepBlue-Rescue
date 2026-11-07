export default function TreatmentList({ treatments }) {
  const items = Array.isArray(treatments)
    ? treatments
    : treatments?.data ?? treatments?.content ?? treatments?.items ?? []

  if (!Array.isArray(items) || items.length === 0) {
    return <p>Este animal todavía no tiene tratamientos registrados.</p>
  }

  return (
    <div className="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Tipo</th>
            <th>Especialista</th>
            <th>Descripción</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item, index) => (
            <tr key={item.id ?? item.treatmentCode ?? index}>
              <td>{item.performedAt ?? item.date ?? '—'}</td>
              <td>{item.type ?? item.treatmentType ?? '—'}</td>
              <td>{item.specialistCode ?? item.specialist?.code ?? '—'}</td>
              <td>{item.description ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
