using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedFlow.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class Programabilidade : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            MedFlow.Infrastructure.Persistencia.ProgramabilidadeSql.Aplicar(migrationBuilder);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            MedFlow.Infrastructure.Persistencia.ProgramabilidadeSql.Remover(migrationBuilder);
        }
    }
}
