export default function ErrorMessage({ error }) {
  if (!error) return null

  const details =
    error.details && typeof error.details === 'object'
      ? Object.entries(error.details)
      : []

  return (
    <div role="alert" className="error">
      <strong>
        {error.status ? `Error ${error.status}: ` : 'Error: '}
      </strong>
      {error.message || 'Ocurrió un error inesperado.'}

      {details.length > 0 && (
        <ul>
          {details.map(([field, message]) => (
            <li key={field}>
              {field}: {String(message)}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
