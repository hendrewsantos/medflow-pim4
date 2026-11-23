using MedFlow.Application.Abstracoes;
using MedFlow.Application.Excecoes;
using MedFlow.Domain.Comum;
using MedFlow.Domain.Entidades;
using MedFlow.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace MedFlow.Application.Dtos
{
    public sealed record AssinaturaDto(bool Ativa, DateTime? AtivadaEm, DateTime? ProximaRenovacaoEm, int DiasAntecedencia, DateTime? UltimaRenovacaoEm);
    public sealed record RenovacaoDto(bool Renovou, string? Motivo, AssinaturaDto Assinatura);
}

namespace MedFlow.Application.Servicos
{
    using MedFlow.Application.Dtos;

    /// <summary>Assinatura simulada: renovação automática da Box. Reaproveita os mesmos serviços do pedido manual.</summary>
    public sealed class AssinaturaService
    {
        public const int DiasAntecedenciaPadrao = 5;
        private static readonly StatusPedido[] EmAndamento =
        {
            StatusPedido.AguardandoPagamento, StatusPedido.Pendente, StatusPedido.EmSeparacao, StatusPedido.AguardandoEntregador, StatusPedido.EmRota,
        };

        private readonly IAppDbContext _db;
        private readonly IRelogio _relogio;
        private readonly PedidoService _pedidos;

        public AssinaturaService(IAppDbContext db, IRelogio relogio, PedidoService pedidos)
        {
            _db = db;
            _relogio = relogio;
            _pedidos = pedidos;
        }

        public async Task<AssinaturaDto> ObterAsync(UsuarioLogado u, CancellationToken ct = default)
        {
            GarantirPaciente(u);
            return await MontarAsync(u.Id, ct);
        }

        public async Task<AssinaturaDto> AtivarAsync(UsuarioLogado u, CancellationToken ct = default)
        {
            GarantirPaciente(u);
            var agora = _relogio.Agora;
            var atual = await _db.Assinaturas.FirstOrDefaultAsync(a => a.PacienteId == u.Id, ct);
            if (atual is null) _db.Assinaturas.Add(new Assinatura(u.Id, agora, DiasAntecedenciaPadrao));
            else if (!atual.Ativa) atual.Ativar(agora);
            await _db.SaveChangesAsync(ct);
            return await MontarAsync(u.Id, ct);
        }

        public async Task<AssinaturaDto> CancelarAsync(UsuarioLogado u, CancellationToken ct = default)
        {
            GarantirPaciente(u);
            var atual = await _db.Assinaturas.FirstOrDefaultAsync(a => a.PacienteId == u.Id, ct);
            if (atual is { Ativa: true })
            {
                atual.Cancelar(_relogio.Agora);
                await _db.SaveChangesAsync(ct);
            }
            return await MontarAsync(u.Id, ct);
        }

        /// <summary>Quando a próxima Box será pedida: fim da Box atual menos a antecedência (nulo se ainda não houve entrega).</summary>
        public async Task<DateTime?> ProximaRenovacaoAsync(int pacienteId, int diasAntecedencia, CancellationToken ct = default)
        {
            var ultimo = await UltimaEntregaAsync(pacienteId, ct);
            return ultimo is null ? null : ultimo.AtualizadoEm.Date.AddDays(ultimo.DiasCiclo).AddDays(-diasAntecedencia);
        }

        /// <summary>
        /// Renova uma assinatura. Sem <paramref name="forcar"/> só renova quando a data chegou. Falhas esperadas (sem receita válida,
        /// estoque insuficiente, Box em andamento) não derrubam nada: voltam como motivo.
        /// </summary>
        public async Task<RenovacaoDto> RenovarAsync(int pacienteId, bool forcar = false, CancellationToken ct = default)
        {
            var assinatura = await _db.Assinaturas.FirstOrDefaultAsync(a => a.PacienteId == pacienteId && a.Ativa, ct);
            if (assinatura is null) return await Resultado(pacienteId, false, "A assinatura não está ativa.", ct);
            var agora = _relogio.Agora;

            if (await _db.Pedidos.AnyAsync(p => p.PacienteId == pacienteId && EmAndamento.Contains(p.Status), ct))
                return await Resultado(pacienteId, false, "Já existe uma Box em andamento.", ct);
            var ultimo = await UltimaEntregaAsync(pacienteId, ct);
            if (ultimo is null) return await Resultado(pacienteId, false, "Ainda não há uma Box entregue para renovar.", ct);
            var quando = ultimo.AtualizadoEm.Date.AddDays(ultimo.DiasCiclo).AddDays(-assinatura.DiasAntecedencia);
            if (!forcar && agora.Date < quando) return await Resultado(pacienteId, false, "Ainda não é hora de renovar.", ct);

            var receita = await _db.Receitas
                .Where(r => r.PacienteId == pacienteId && r.Status == StatusReceita.Aprovada && r.ValidaAte != null && r.ValidaAte >= agora.Date)
                .OrderByDescending(r => r.ValidaAte).FirstOrDefaultAsync(ct);
            if (receita is null) return await Resultado(pacienteId, false, "A receita venceu: envie uma nova para a renovação automática.", ct);

            var paciente = await _db.Usuarios.FirstAsync(x => x.Id == pacienteId, ct);
            var logado = new UsuarioLogado(pacienteId, PerfilUsuario.Paciente);
            try
            {
                var anterior = await _pedidos.ObterAsync(logado, ultimo.Id, ct);
                var itens = anterior.Itens.Select(i => new ItemBoxRequest(i.MedicamentoId, i.Horarios, i.DosePorHorario)).ToList();
                var novo = await _pedidos.ConfirmarAsync(logado, new ConfirmarPedidoRequest(receita.Id, itens, anterior.EnderecoEntrega), ct);
                // Cobrança automática (simulada): com o pagamento habilitado, a assinatura paga sozinha.
                if (novo.Status == StatusPedido.AguardandoPagamento) await _pedidos.AprovarPagamentoAsync(logado, novo.Id, ct);
            }
            catch (Exception e) when (e is DomainException or ConflitoException)
            {
                return await Resultado(pacienteId, false, e.Message, ct);
            }

            assinatura.RegistrarRenovacao(agora);
            await _db.SaveChangesAsync(ct);
            return await Resultado(pacienteId, true, null, ct);
        }

        /// <summary>Tarefa periódica: renova todas as assinaturas ativas cuja data chegou.</summary>
        public async Task<int> RenovarVencidasAsync(CancellationToken ct = default)
        {
            var ids = await _db.Assinaturas.Where(a => a.Ativa).Select(a => a.PacienteId).ToListAsync(ct);
            var renovadas = 0;
            foreach (var id in ids)
                if ((await RenovarAsync(id, false, ct)).Renovou) renovadas++;
            return renovadas;
        }

        private async Task<Pedido?> UltimaEntregaAsync(int pacienteId, CancellationToken ct) =>
            await _db.Pedidos.Where(p => p.PacienteId == pacienteId && p.Status == StatusPedido.Entregue)
                .OrderByDescending(p => p.AtualizadoEm).FirstOrDefaultAsync(ct);

        private async Task<RenovacaoDto> Resultado(int pacienteId, bool renovou, string? motivo, CancellationToken ct) =>
            new(renovou, motivo, await MontarAsync(pacienteId, ct));

        private async Task<AssinaturaDto> MontarAsync(int pacienteId, CancellationToken ct)
        {
            var a = await _db.Assinaturas.AsNoTracking().FirstOrDefaultAsync(x => x.PacienteId == pacienteId, ct);
            var dias = a?.DiasAntecedencia ?? DiasAntecedenciaPadrao;
            var proxima = a is { Ativa: true } ? await ProximaRenovacaoAsync(pacienteId, dias, ct) : null;
            return new AssinaturaDto(a?.Ativa ?? false, a?.AtivadaEm, proxima, dias, a?.UltimaRenovacaoEm);
        }

        private static void GarantirPaciente(UsuarioLogado u)
        {
            if (u.Perfil != PerfilUsuario.Paciente) throw new AcessoNegadoException("Somente pacientes têm assinatura.");
        }
    }
}
