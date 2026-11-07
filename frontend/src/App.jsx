import { useState } from 'react'
import CaseList from './components/CaseList'
import CaseDetail from './components/CaseDetail'

export default function App() {
  const [selected, setSelected] = useState(null)

  return (
    <>
      <header>
        <h1>DeepBlue Rescue</h1>
        <p>Gestión de casos de rescate y tratamientos</p>
      </header>

      <main className="layout">
        <CaseList
          selected={selected}
          onSelect={setSelected}
        />

        <div>
          {selected ? (
            <CaseDetail
              caseItem={selected}
              onChanged={setSelected}
            />
          ) : (
            <section>
              <h2>Bienvenido</h2>
              <p>
                Selecciona un caso de rescate para consultar
                su información.
              </p>
            </section>
          )}
        </div>
      </main>
    </>
  )
}
