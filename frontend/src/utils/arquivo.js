// Salva um Blob como arquivo no computador do usuário (link temporário + clique).
export function salvarBlob(blob, nome) {
  const url = URL.createObjectURL(blob)
  const a = Object.assign(document.createElement('a'), { href: url, download: nome })
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
