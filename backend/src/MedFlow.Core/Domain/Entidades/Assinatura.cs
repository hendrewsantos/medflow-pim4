using MedFlow.Domain.Comum;

namespace MedFlow.Domain.Entidades;

/// <summary>
/// Assinatura simulada: com ela ativa, a próxima Box é pedida sozinha alguns dias antes de a atual acabar,
/// com os mesmos medicamentos e horários, e cobrada automaticamente (pagamento simulado, sem cobrança real).
/// </summary>
public sealed class Assinatura : Entidade
{
    private Assinatura() { }

    public Assinatura(int pacienteId, DateTime agora, int diasAntecedencia = 5)
    {
        if (diasAntecedencia < 1 || diasAntecedencia > 15) throw new DomainException("A antecedência da renovação deve ficar entre 1 e 15 dias.");
        PacienteId = pacienteId;
        DiasAntecedencia = diasAntecedencia;
        Ativar(agora);
    }

    public int PacienteId { get; private set; }
    public Usuario? Paciente { get; private set; }
    public bool Ativa { get; private set; }
    public int DiasAntecedencia { get; private set; }
    public DateTime AtivadaEm { get; private set; }
    public DateTime? CanceladaEm { get; private set; }
    public DateTime? UltimaRenovacaoEm { get; private set; }

    public void Ativar(DateTime agora)
    {
        Ativa = true;
        AtivadaEm = agora;
        CanceladaEm = null;
    }

    public void Cancelar(DateTime agora)
    {
        Ativa = false;
        CanceladaEm = agora;
    }

    public void RegistrarRenovacao(DateTime agora) => UltimaRenovacaoEm = agora;
}
