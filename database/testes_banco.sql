-- Roteiro de verificação do banco (MySQL 8.4): procedimentos, gatilhos, restrições e visões.
-- Executar em um banco criado pelas migrações:  mysql -u root -p --force medflow < database/testes_banco.sql
-- Linhas "ESPERADO: ERRO" devem falhar; o restante deve passar. A saída é conferida no capítulo 13.
SET @hoje = CURDATE();

SELECT '== preparo ==' AS passo;
INSERT INTO usuarios (Nome, Email, Cpf, SenhaHash, Perfil, Endereco, CriadoEm, Ativo, FalhasLogin)
VALUES ('Paciente Teste', 'p@teste.med', '39053344705', 'hash-de-teste', 'Paciente', 'Rua Teste 123, Sao Paulo', UTC_TIMESTAMP(6), 1, 0),
       ('Farmaceutica Teste', 'f@teste.med', '52998224725', 'hash-de-teste', 'Administrador', NULL, UTC_TIMESTAMP(6), 1, 0);
SET @pac = (SELECT Id FROM usuarios WHERE Email = 'p@teste.med');
SET @far = (SELECT Id FROM usuarios WHERE Email = 'f@teste.med');
INSERT INTO medicamentos (Nome, PrincipioAtivo, Concentracao, FormaFarmaceutica, Tarja, ControleEspecial, Categoria, Termolabil, NivelMinimo, Ativo)
VALUES ('Losartana', 'losartana', '50 mg', 'comprimido', 'Vermelha', 0, 'Comum', 0, 100, 1);
SET @med = (SELECT Id FROM medicamentos WHERE Nome = 'Losartana');

SELECT '== sp_registrar_entrada_lote: dois lotes, validades diferentes ==' AS passo;
CALL sp_registrar_entrada_lote(@med, 'CEDO', DATE_SUB(@hoje, INTERVAL 60 DAY), DATE_ADD(@hoje, INTERVAL 120 DAY), 50, @far);
CALL sp_registrar_entrada_lote(@med, 'TARDE', DATE_SUB(@hoje, INTERVAL 30 DAY), DATE_ADD(@hoje, INTERVAL 400 DAY), 400, @far);
SELECT 'ESPERADO: ERRO (lote vencido)' AS passo;
CALL sp_registrar_entrada_lote(@med, 'VENCIDO', DATE_SUB(@hoje, INTERVAL 90 DAY), DATE_SUB(@hoje, INTERVAL 1 DAY), 10, @far);
SELECT 'ESPERADO: ERRO (quantidade nao positiva)' AS passo;
CALL sp_registrar_entrada_lote(@med, 'ZERO', DATE_SUB(@hoje, INTERVAL 30 DAY), DATE_ADD(@hoje, INTERVAL 400 DAY), 0, @far);

-- pedido de 80 unidades: FEFO deve tirar 50 do lote CEDO e 30 do lote TARDE
INSERT INTO receitas (PacienteId, CaminhoArquivo, NomeOriginal, TipoConteudo, TamanhoBytes, EnviadaEm, Status)
VALUES (@pac, 'x.pdf', 'x.pdf', 'application/pdf', 10, UTC_TIMESTAMP(6), 'Pendente');
SET @rec = LAST_INSERT_ID();
INSERT INTO tratamentos (PacienteId, ReceitaId, CriadoEm, Ativo) VALUES (@pac, @rec, UTC_TIMESTAMP(6), 1);
SET @trat = LAST_INSERT_ID();
INSERT INTO pedidos (PacienteId, TratamentoId, ReceitaId, Status, EnderecoEntrega, DiasCiclo, CriadoEm, AtualizadoEm)
VALUES (@pac, @trat, @rec, 'Pendente', 'Rua Teste 123', 30, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @ped = LAST_INSERT_ID();
INSERT INTO itens_pedido (PedidoId, MedicamentoId, Quantidade) VALUES (@ped, @med, 80);

SELECT '== sp_reservar_pedido: FEFO (esperado: reservado 50 no CEDO? NAO: CEDO vence em 120 dias e o ciclo exige validade >= 30 dias, entao usa CEDO=50 e TARDE=30) ==' AS passo;
CALL sp_reservar_pedido(@ped, @pac);
SELECT Numero, QuantidadeFisica, QuantidadeReservada, Versao FROM lotes ORDER BY Validade;
SELECT Tipo, Quantidade FROM movimentacoes_estoque WHERE PedidoId = @ped;

SELECT 'ESPERADO: ERRO (estoque insuficiente) e nenhuma reserva nova' AS passo;
INSERT INTO pedidos (PacienteId, TratamentoId, ReceitaId, Status, EnderecoEntrega, DiasCiclo, CriadoEm, AtualizadoEm)
VALUES (@pac, @trat, @rec, 'Pendente', 'Rua Teste 123', 30, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @ped2 = LAST_INSERT_ID();
INSERT INTO itens_pedido (PedidoId, MedicamentoId, Quantidade) VALUES (@ped2, @med, 1000);
CALL sp_reservar_pedido(@ped2, @pac);
SELECT SUM(QuantidadeReservada) AS reservado_total_apos_falha FROM lotes;

SELECT '== gatilho de transicao: valida e invalida ==' AS passo;
UPDATE pedidos SET Status = 'EmSeparacao' WHERE Id = @ped;
SELECT 'ESPERADO: ERRO (Pendente -> EmRota nao e permitido)' AS passo;
UPDATE pedidos SET Status = 'EmRota' WHERE Id = @ped2;
SELECT 'ESPERADO: ERRO (EmSeparacao -> Entregue nao e permitido)' AS passo;
UPDATE pedidos SET Status = 'Entregue' WHERE Id = @ped;

SELECT '== sp_baixar_pedido: baixa definitiva ==' AS passo;
CALL sp_baixar_pedido(@ped, @far);
SELECT Numero, QuantidadeFisica, QuantidadeReservada FROM lotes ORDER BY Validade;
SELECT Status FROM pedidos WHERE Id = @ped;
SELECT 'ESPERADO: ERRO (pedido nao esta em separacao)' AS passo;
CALL sp_baixar_pedido(@ped, @far);

SELECT '== alerta de estoque minimo (gatilho) e sp_liberar_reserva ==' AS passo;
INSERT INTO pedidos (PacienteId, TratamentoId, ReceitaId, Status, EnderecoEntrega, DiasCiclo, CriadoEm, AtualizadoEm)
VALUES (@pac, @trat, @rec, 'Pendente', 'Rua Teste 123', 30, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @ped3 = LAST_INSERT_ID();
INSERT INTO itens_pedido (PedidoId, MedicamentoId, Quantidade) VALUES (@ped3, @med, 290);
CALL sp_reservar_pedido(@ped3, @pac);
SELECT Numero, QuantidadeFisica, QuantidadeReservada FROM lotes ORDER BY Validade;
SELECT 'alertas apos reservar 290 (disponivel cai para 80, minimo 100):' AS passo;
SELECT Tipo, Descricao FROM alertas_estoque ORDER BY Id;
CALL sp_liberar_reserva(@ped3, @pac, 'Cancelado');
SELECT Numero, QuantidadeFisica, QuantidadeReservada FROM lotes ORDER BY Validade;
SELECT Status FROM pedidos WHERE Id = @ped3;
SELECT De, Para FROM historico_pedido WHERE PedidoId = @ped3;
SELECT 'ESPERADO: ERRO (so pedido pendente pode ter a reserva liberada)' AS passo;
CALL sp_liberar_reserva(@ped3, @pac, 'Cancelado');

SELECT '== gatilho de imutabilidade ==' AS passo;
SELECT 'ESPERADO: ERRO (movimentacoes sao imutaveis) x2' AS passo;
UPDATE movimentacoes_estoque SET Quantidade = 1 WHERE Id = (SELECT MIN(Id) FROM (SELECT Id FROM movimentacoes_estoque) t);
DELETE FROM movimentacoes_estoque WHERE Id = (SELECT MIN(Id) FROM (SELECT Id FROM movimentacoes_estoque) t);

SELECT '== restricoes CHECK ==' AS passo;
SELECT 'ESPERADO: ERRO (reservado maior que fisico)' AS passo;
UPDATE lotes SET QuantidadeReservada = QuantidadeFisica + 1 WHERE Numero = 'TARDE';
SELECT 'ESPERADO: ERRO (dose fora de 1..4)' AS passo;
INSERT INTO saches (TratamentoId, MedicamentoId, Horario, Quantidade) VALUES (@trat, @med, '08:00', 9);

SELECT '== gatilho de alerta de estoque minimo e de validade ==' AS passo;
CALL sp_registrar_entrada_lote(@med, 'PERTO', DATE_SUB(@hoje, INTERVAL 300 DAY), DATE_ADD(@hoje, INTERVAL 10 DAY), 5, @far);
SELECT Tipo, Descricao FROM alertas_estoque ORDER BY Id;

SELECT '== visoes ==' AS passo;
SELECT * FROM vw_estoque_disponivel;
SELECT * FROM vw_estoque_critico;
SELECT Nome, Numero, DiasParaVencer FROM vw_lotes_a_vencer;
SELECT * FROM vw_top_medicamentos;
