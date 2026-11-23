import { test } from 'node:test'
import assert from 'node:assert/strict'
import { interacoesDe } from '../src/domain/interacoes.js'

test('Interações — levotiroxina com omeprazol e com cálcio geram avisos; Box sem pares não gera nada', () => {
  const avisos = interacoesDe(['Levotiroxina sódica 50 mcg', 'Omeprazol 20 mg', 'Carbonato de cálcio com vitamina D 500 mg + 400 UI'])
  assert.deepEqual(avisos.map((a) => a.titulo).sort(), ['Levotiroxina com cálcio ou ferro', 'Levotiroxina com inibidor de bomba de prótons'])
  assert.deepEqual(interacoesDe(['Losartana potássica 50 mg', 'Metformina 850 mg', 'Sinvastatina 20 mg']), [])
})

test('Interações — omeprazol com dipirona NÃO é alerta; AAS com varfarina e sinvastatina com anlodipino são', () => {
  assert.deepEqual(interacoesDe(['Omeprazol 20 mg', 'Dipirona 500 mg']), [])
  assert.equal(interacoesDe(['AAS 100 mg', 'Varfarina 5 mg']).length, 1)
  assert.equal(interacoesDe(['Sinvastatina 20 mg', 'Anlodipino 5 mg']).length, 1)
  assert.equal(interacoesDe(['Losartana potássica 50 mg', 'Espironolactona 25 mg']).length, 1)
})

test('Interações — um item sozinho, ou dois do mesmo grupo, não geram aviso', () => {
  assert.deepEqual(interacoesDe(['Levotiroxina sódica 50 mcg']), [])
  assert.deepEqual(interacoesDe(['Omeprazol 20 mg', 'Pantoprazol 40 mg']), [])
})
