using System;
using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedFlow.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class Assinatura : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "assinaturas",
                columns: table => new
                {
                    Id = table.Column<int>(type: "int", nullable: false)
                        .Annotation("MySql:ValueGenerationStrategy", MySqlValueGenerationStrategy.IdentityColumn),
                    PacienteId = table.Column<int>(type: "int", nullable: false),
                    Ativa = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    DiasAntecedencia = table.Column<int>(type: "int", nullable: false),
                    AtivadaEm = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    CanceladaEm = table.Column<DateTime>(type: "datetime(6)", nullable: true),
                    UltimaRenovacaoEm = table.Column<DateTime>(type: "datetime(6)", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_assinaturas", x => x.Id);
                    table.ForeignKey(
                        name: "FK_assinaturas_usuarios_PacienteId",
                        column: x => x.PacienteId,
                        principalTable: "usuarios",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateIndex(
                name: "IX_assinaturas_PacienteId",
                table: "assinaturas",
                column: "PacienteId",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "assinaturas");
        }
    }
}
