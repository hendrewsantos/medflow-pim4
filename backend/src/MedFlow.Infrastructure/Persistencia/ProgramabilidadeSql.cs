using System.Reflection;
using Microsoft.EntityFrameworkCore.Migrations;

namespace MedFlow.Infrastructure.Persistencia;

/// <summary>Lê o arquivo Sql/programabilidade.sql (recurso embutido) e aplica cada comando, separados por "$$".</summary>
public static class ProgramabilidadeSql
{
    public static string Texto()
    {
        var assembly = typeof(ProgramabilidadeSql).Assembly;
        var nome = assembly.GetManifestResourceNames().Single(n => n.EndsWith("programabilidade.sql", StringComparison.OrdinalIgnoreCase));
        using var leitor = new StreamReader(assembly.GetManifestResourceStream(nome)!);
        return leitor.ReadToEnd();
    }

    public static void Aplicar(MigrationBuilder migrationBuilder)
    {
        foreach (var comando in Texto().Split("$$", StringSplitOptions.RemoveEmptyEntries))
        {
            var limpo = string.Join('\n', comando.Split('\n').Where(l => !l.TrimStart().StartsWith("--"))).Trim();
            if (limpo.Length > 0) migrationBuilder.Sql(limpo);
        }
    }

    public static void Remover(MigrationBuilder migrationBuilder)
    {
        foreach (var gatilho in new[] { "trg_pedidos_valida_transicao", "trg_mov_imutavel_upd", "trg_mov_imutavel_del", "trg_lotes_alerta_minimo", "trg_lotes_alerta_validade" })
            migrationBuilder.Sql($"DROP TRIGGER IF EXISTS {gatilho}");
        foreach (var proc in new[] { "sp_reservar_pedido", "sp_baixar_pedido", "sp_liberar_reserva", "sp_registrar_entrada_lote" })
            migrationBuilder.Sql($"DROP PROCEDURE IF EXISTS {proc}");
        foreach (var visao in new[] { "vw_top_medicamentos", "vw_lotes_a_vencer", "vw_estoque_critico", "vw_estoque_disponivel" })
            migrationBuilder.Sql($"DROP VIEW IF EXISTS {visao}");
        migrationBuilder.Sql("DROP TABLE IF EXISTS alertas_estoque");
        foreach (var (tabela, restricao) in new[] { ("lotes", "ck_lote_saldo"), ("lotes", "ck_lote_datas"), ("medicamentos", "ck_med_minimo"), ("itens_pedido", "ck_item_qtd"),
                                                    ("alocacoes_lote", "ck_aloc_qtd"), ("saches", "ck_sache_dose"), ("movimentacoes_estoque", "ck_mov_qtd") })
            migrationBuilder.Sql($"ALTER TABLE {tabela} DROP CONSTRAINT {restricao}");
    }
}
