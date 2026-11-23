"""Roteiro ponta a ponta contra a API real (http://localhost:5080): paciente -> separação -> entrega.
Uso: python scripts/e2e_api.py   (API em Development, banco recém-criado com os dados de demonstração)
A senha das contas de demonstração é lida de DadosIniciais.cs e nunca é impressa."""
import json, re, sys, urllib.request, urllib.error, io, os, ssl

BASE = os.environ.get("MEDFLOW_API", "http://localhost:5080")
FONTE = os.path.join(os.path.dirname(__file__), "..", "backend", "src", "MedFlow.Infrastructure", "Persistencia", "DadosIniciais.cs")
SENHA = re.search(r'SenhaDemonstracao = "([^"]+)"', io.open(FONTE, encoding="utf-8-sig").read()).group(1)
falhas = []
# Só para certificado local do Caddy (DOMINIO=localhost): MEDFLOW_INSEGURO=1 ignora a verificação do certificado.
CTX = ssl._create_unverified_context() if os.environ.get("MEDFLOW_INSEGURO") else None

def chamar(metodo, caminho, corpo=None, token=None):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    req = urllib.request.Request(BASE + caminho, data=dados, method=metodo, headers={"Content-Type": "application/json"})
    if token: req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, context=CTX) as r:
            txt = r.read().decode("utf-8-sig")
            return r.status, (json.loads(txt) if txt and r.headers.get_content_type() == "application/json" else txt)
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8", "ignore")
        try: return e.code, json.loads(txt)
        except Exception: return e.code, txt

def confere(nome, obtido, esperado):
    ok = obtido == esperado
    print(("OK    " if ok else "FALHA ") + f"{nome}: {obtido}" + ("" if ok else f" (esperado {esperado})"))
    if not ok: falhas.append(nome)

def login(email):
    s, r = chamar("POST", "/api/auth/login", {"email": email, "senha": SENHA})
    confere(f"login {email.split('@')[0]}", s, 200)
    return r["token"]

carlos, juliana, bruno, rafael = (login(e) for e in ("carlos@email.com", "juliana@medflow.com.br", "bruno@medflow.com.br", "rafael@medflow.com.br"))

s, meds = chamar("GET", "/api/medicamentos", token=carlos)
nomes = [m["nome"] for m in meds]
confere("paciente não vê antimicrobiano, termolábil nem controlado", [n for n in nomes if n in ("Amoxicilina", "Insulina NPH", "Clonazepam")], [])
s, meds_eq = chamar("GET", "/api/medicamentos", token=bruno)
confere("equipe vê os 81 itens do catálogo", len(meds_eq), 81)
confere("catálogo do paciente tem preço fictício em todos os itens", all(m.get("precoUnitario", 0) > 0 for m in meds), True)
los = next(m for m in meds if m["nome"].startswith("Losartana"))

s, receitas = chamar("GET", "/api/receitas/minhas", token=carlos)
rid = receitas[0]["id"]
confere("receita de demonstração aprovada com prescritor", (receitas[0]["status"], bool(receitas[0].get("prescritorCrm"))), ("Aprovada", True))

box = {"receitaId": rid, "enderecoEntrega": "Rua das Acacias, 120 - Vila Mariana, Sao Paulo/SP", "itens": [{"medicamentoId": los["id"], "horarios": ["08:00", "20:00"], "dosePorHorario": 1}]}
s, sim = chamar("POST", "/api/pedidos/simular", box["itens"], carlos)
confere("simulação mostra disponibilidade", (s, sim["podeConfirmar"]), (200, True))
s, ped = chamar("POST", "/api/pedidos", box, carlos)
PAGAMENTO = ped.get("status") == "AguardandoPagamento"   # pagamento simulado habilitado na API
confere("confirmar Box (reserva feita; com pagamento simulado, aguarda o pagamento)", (s, ped.get("status")), (201, "AguardandoPagamento" if PAGAMENTO else "EmSeparacao"))
pid = ped["id"]
if PAGAMENTO:
    confere("pedido traz valor e prazo do pagamento", (ped["valorMedicamentos"] > 0, ped["valorEntrega"] > 0, bool(ped["pagamentoPrazoEm"])), (True, True, True))
    s, _ = chamar("POST", f"/api/pedidos/{pid}/finalizar-box", token=bruno)
    confere("operador não finaliza Box sem pagamento", s in (409, 422), True)
    s, pago = chamar("POST", f"/api/pedidos/{pid}/pagamento/aprovar", token=carlos)
    confere("aprovar pagamento (receita aprovada: segue para separação)", (s, pago.get("status")), (200, "EmSeparacao"))
confere("reserva por lote (FEFO) registrada", len(ped["itens"][0]["lotes"]) >= 1, True)

s, _ = chamar("POST", "/api/pedidos", box, carlos)
confere("segunda Box enquanto há uma em preparação: conflito", s, 409)
s, _ = chamar("POST", f"/api/pedidos/{pid}/finalizar-box", token=carlos)
confere("paciente não finaliza Box", s, 403)
s, fin = chamar("POST", f"/api/pedidos/{pid}/finalizar-box", token=bruno)
confere("operador finaliza Box com baixa", (s, fin.get("status")), (200, "AguardandoEntregador"))

s, lista = chamar("GET", "/api/entregas/disponiveis", token=rafael)
confere("entregador vê a Box pronta", (s, len(lista)), (200, 1))
chaves = set(lista[0].keys())
confere("entregador recebe só dados mínimos (sem itens, lotes ou receita)", [k for k in chaves if k in ("itens", "lotes", "receitaId", "medicamentos")], [])
s, _ = chamar("GET", f"/api/pedidos/{pid}", token=rafael)
confere("entregador não abre o detalhe do pedido", s, 403)
s, e = chamar("POST", f"/api/entregas/{pid}/iniciar", token=rafael)
confere("iniciar rota", (s, e.get("status")), (200, "EmRota"))
s, _ = chamar("POST", f"/api/entregas/{pid}/confirmar", {"recebedor": ""}, rafael)
confere("confirmar sem recebedor é recusado", s in (400, 422), True)
s, e = chamar("POST", f"/api/entregas/{pid}/confirmar", {"recebedor": "Carlos Mendes"}, rafael)
confere("confirmar entrega", (s, e.get("status")), (200, "Entregue"))

s, det = chamar("GET", f"/api/pedidos/{pid}", token=carlos)
confere("linha do tempo do paciente", [h["para"] for h in det["historico"]], (["AguardandoPagamento"] if PAGAMENTO else ["Pendente"]) + ["EmSeparacao", "AguardandoEntregador", "EmRota", "Entregue"])
s, ren = chamar("GET", "/api/pedidos/proxima-renovacao", token=carlos)
confere("próxima renovação", (s, ren["temBox"], ren["diasRestantes"]), (200, True, 30))

s, prev = chamar("GET", "/api/previsao-reposicao", token=bruno)
l = next(x for x in prev if x["nome"].startswith("Losartana"))
confere("previsão de reposição calcula consumo da baixa", (s, l["consumoMedioMensal"]), (200, 20.0))
s, csv = chamar("GET", "/api/relatorios/estoque.csv", token=bruno)
confere("CSV de estoque", (s, "Medicamento;Concentracao" in csv), (200, True))
s, _ = chamar("GET", "/api/relatorios/estoque.csv", token=carlos)
confere("paciente não baixa relatório", s, 403)
s, pn = chamar("GET", "/api/painel", token=bruno)
confere("painel da equipe", (s, pn["pedidosPorStatus"]["Entregue"]), (200, 1))

s, _ = chamar("POST", "/api/auth/alterar-senha", {"senhaAtual": "errada123", "novaSenha": "NovaSenha123"}, carlos)
confere("alterar senha com senha atual errada", s, 400)
s, _ = chamar("POST", "/api/privacidade/revogar", {"tipo": "DadosDeSaude"}, carlos)
confere("revogar consentimento de saúde", s, 200)
s, _ = chamar("POST", "/api/pedidos", box, carlos)
confere("novo pedido após revogar é recusado", s in (422, 409), True)

print("\nRESULTADO:", "tudo certo" if not falhas else f"{len(falhas)} falha(s): {falhas}")
sys.exit(1 if falhas else 0)
