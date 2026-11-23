using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedFlow.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class Pagamento : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "PagamentoPrazoEm",
                table: "pedidos",
                type: "datetime(6)",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "PagoEm",
                table: "pedidos",
                type: "datetime(6)",
                nullable: true);

            migrationBuilder.AddColumn<decimal>(
                name: "ValorEntrega",
                table: "pedidos",
                type: "decimal(10,2)",
                precision: 10,
                scale: 2,
                nullable: false,
                defaultValue: 0m);

            migrationBuilder.AddColumn<decimal>(
                name: "ValorMedicamentos",
                table: "pedidos",
                type: "decimal(10,2)",
                precision: 10,
                scale: 2,
                nullable: false,
                defaultValue: 0m);

            migrationBuilder.AddColumn<decimal>(
                name: "PrecoUnitario",
                table: "medicamentos",
                type: "decimal(10,2)",
                precision: 10,
                scale: 2,
                nullable: false,
                defaultValue: 0m);

            // RN08 no banco: o gatilho passa a aceitar o estado AguardandoPagamento.
            migrationBuilder.Sql("DROP TRIGGER IF EXISTS trg_pedidos_valida_transicao");
            migrationBuilder.Sql(@"CREATE TRIGGER trg_pedidos_valida_transicao BEFORE UPDATE ON pedidos FOR EACH ROW
BEGIN
  IF NEW.Status <> OLD.Status AND NOT (
             (OLD.Status = 'AguardandoPagamento' AND NEW.Status IN ('Pendente', 'EmSeparacao', 'Recusado', 'Cancelado')) OR
             (OLD.Status = 'Pendente' AND NEW.Status IN ('EmSeparacao', 'Recusado', 'Cancelado')) OR
             (OLD.Status = 'EmSeparacao' AND NEW.Status = 'AguardandoEntregador') OR
             (OLD.Status = 'AguardandoEntregador' AND NEW.Status = 'EmRota') OR
             (OLD.Status = 'EmRota' AND NEW.Status IN ('Entregue', 'AguardandoEntregador'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transicao de status nao permitida';
  END IF;
END");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("DROP TRIGGER IF EXISTS trg_pedidos_valida_transicao");
            migrationBuilder.Sql(@"CREATE TRIGGER trg_pedidos_valida_transicao BEFORE UPDATE ON pedidos FOR EACH ROW
BEGIN
  IF NEW.Status <> OLD.Status AND NOT (
             (OLD.Status = 'Pendente' AND NEW.Status IN ('EmSeparacao', 'Recusado', 'Cancelado')) OR
             (OLD.Status = 'EmSeparacao' AND NEW.Status = 'AguardandoEntregador') OR
             (OLD.Status = 'AguardandoEntregador' AND NEW.Status = 'EmRota') OR
             (OLD.Status = 'EmRota' AND NEW.Status IN ('Entregue', 'AguardandoEntregador'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transicao de status nao permitida';
  END IF;
END");

            migrationBuilder.DropColumn(
                name: "PagamentoPrazoEm",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "PagoEm",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "ValorEntrega",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "ValorMedicamentos",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "PrecoUnitario",
                table: "medicamentos");
        }
    }
}
