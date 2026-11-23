// Pontos de atenção entre medicamentos de uma mesma Box.
// Tabela de demonstração, baseada em orientações de bula; NÃO substitui a avaliação da farmacêutica responsável,
// que é quem valida a lista final. Os avisos informam e orientam a conversa: nunca bloqueiam o pedido.

const GRUPOS = {
  levotiroxina: ['levotiroxina'],
  ibp: ['omeprazol', 'pantoprazol', 'esomeprazol', 'lansoprazol'],
  calcioFerro: ['carbonato de cálcio', 'sulfato ferroso'],
  sinvastatina: ['sinvastatina'],
  anlodipino: ['anlodipino'],
  bloqueadoresDoSistemaRenina: ['losartana', 'valsartana', 'olmesartana', 'candesartana', 'enalapril', 'captopril', 'ramipril', 'lisinopril'],
  potassio: ['espironolactona', 'cloreto de potássio'],
  aas: ['ácido acetilsalicílico', 'aas '],
  antitromboticos: ['varfarina', 'clopidogrel'],
  varfarina: ['varfarina'],
  amiodarona: ['amiodarona'],
}

export const REGRAS = [
  { a: 'levotiroxina', b: 'ibp', titulo: 'Levotiroxina com inibidor de bomba de prótons',
    texto: 'Omeprazol e similares podem reduzir a absorção da levotiroxina. A farmacêutica avalia se a dose da tireoide precisa de acompanhamento.' },
  { a: 'levotiroxina', b: 'calcioFerro', titulo: 'Levotiroxina com cálcio ou ferro',
    texto: 'Cálcio e ferro atrapalham a absorção da levotiroxina quando tomados juntos. Costuma-se separar por cerca de 4 horas; confira os horários.' },
  { a: 'sinvastatina', b: 'anlodipino', titulo: 'Sinvastatina com anlodipino',
    texto: 'O anlodipino aumenta a exposição à sinvastatina. A bula recomenda limitar a sinvastatina a 20 mg por dia nessa combinação.' },
  { a: 'bloqueadoresDoSistemaRenina', b: 'potassio', titulo: 'Losartana e similares com espironolactona ou potássio',
    texto: 'A combinação pode elevar o potássio no sangue. Pode ser preciso exame de acompanhamento.' },
  { a: 'aas', b: 'antitromboticos', titulo: 'AAS com varfarina ou clopidogrel',
    texto: 'Usados juntos, aumentam o risco de sangramento. Só devem seguir juntos com indicação médica.' },
  { a: 'varfarina', b: 'amiodarona', titulo: 'Varfarina com amiodarona',
    texto: 'A amiodarona aumenta o efeito da varfarina e exige acompanhamento do INR.' },
]

const normaliza = (t) => ` ${String(t || '').toLowerCase()} `
const pertence = (texto, grupo) => GRUPOS[grupo].some((token) => texto.includes(token))

/** Recebe os nomes (ou nome + princípio ativo) dos itens da Box e devolve os pontos de atenção encontrados. */
export function interacoesDe(itens) {
  const textos = itens.map((i) => ({ original: i, texto: normaliza(i) }))
  const achados = []
  for (const regra of REGRAS) {
    const A = textos.filter((t) => pertence(t.texto, regra.a))
    const B = textos.filter((t) => pertence(t.texto, regra.b) && !A.includes(t))
    if (A.length && B.length) achados.push({ titulo: regra.titulo, texto: regra.texto, itens: [...new Set([...A, ...B].map((t) => t.original))] })
  }
  return achados
}
