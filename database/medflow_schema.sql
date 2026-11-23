-- =====================================================================
-- MedFlow Solutions - PIM IV - script completo do banco de dados (MySQL 8.x)
-- Parte 1: DDL gerado pelas migrações do EF Core (14 tabelas + histórico de migrações).
-- Parte 2: restrições CHECK, tabela de alertas, visões, procedimentos e gatilhos.
-- Uso:  mysql -u root -p -e "CREATE DATABASE medflow CHARACTER SET utf8mb4"
--       mysql -u root -p medflow < database/medflow_schema.sql
-- Verificação do comportamento: database/testes_banco.sql (saída em database/resultado_testes_banco.txt).
-- =====================================================================

CREATE TABLE IF NOT EXISTS `__EFMigrationsHistory` (
    `MigrationId` varchar(150) CHARACTER SET utf8mb4 NOT NULL,
    `ProductVersion` varchar(32) CHARACTER SET utf8mb4 NOT NULL,
    CONSTRAINT `PK___EFMigrationsHistory` PRIMARY KEY (`MigrationId`)
) CHARACTER SET=utf8mb4;

START TRANSACTION;

ALTER DATABASE CHARACTER SET utf8mb4;

CREATE TABLE `medicamentos` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `Nome` varchar(150) CHARACTER SET utf8mb4 NOT NULL,
    `PrincipioAtivo` varchar(150) CHARACTER SET utf8mb4 NOT NULL,
    `Concentracao` varchar(50) CHARACTER SET utf8mb4 NOT NULL,
    `FormaFarmaceutica` varchar(50) CHARACTER SET utf8mb4 NOT NULL,
    `Tarja` varchar(20) CHARACTER SET utf8mb4 NOT NULL,
    `ControleEspecial` tinyint(1) NOT NULL,
    `Categoria` varchar(30) CHARACTER SET utf8mb4 NOT NULL,
    `Termolabil` tinyint(1) NOT NULL,
    `NivelMinimo` int NOT NULL,
    `Ativo` tinyint(1) NOT NULL,
    CONSTRAINT `PK_medicamentos` PRIMARY KEY (`Id`)
) CHARACTER SET=utf8mb4;

CREATE TABLE `usuarios` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `Nome` varchar(150) CHARACTER SET utf8mb4 NOT NULL,
    `Email` varchar(150) CHARACTER SET utf8mb4 NOT NULL,
    `Cpf` varchar(14) CHARACTER SET utf8mb4 NOT NULL,
    `SenhaHash` varchar(200) CHARACTER SET utf8mb4 NOT NULL,
    `Perfil` varchar(20) CHARACTER SET utf8mb4 NOT NULL,
    `Endereco` varchar(255) CHARACTER SET utf8mb4 NULL,
    `Telefone` varchar(20) CHARACTER SET utf8mb4 NULL,
    `CriadoEm` datetime(6) NOT NULL,
    `Ativo` tinyint(1) NOT NULL,
    `AnonimizadoEm` datetime(6) NULL,
    `FalhasLogin` int NOT NULL,
    `BloqueadoAte` datetime(6) NULL,
    CONSTRAINT `PK_usuarios` PRIMARY KEY (`Id`)
) CHARACTER SET=utf8mb4;

CREATE TABLE `lotes` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `MedicamentoId` int NOT NULL,
    `Numero` varchar(40) CHARACTER SET utf8mb4 NOT NULL,
    `Fabricacao` datetime(6) NOT NULL,
    `Validade` datetime(6) NOT NULL,
    `QuantidadeFisica` int NOT NULL,
    `QuantidadeReservada` int NOT NULL,
    `Versao` int NOT NULL,
    CONSTRAINT `PK_lotes` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_lotes_medicamentos_MedicamentoId` FOREIGN KEY (`MedicamentoId`) REFERENCES `medicamentos` (`Id`) ON DELETE RESTRICT
) CHARACTER SET=utf8mb4;

CREATE TABLE `consentimentos` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `UsuarioId` int NOT NULL,
    `Tipo` varchar(20) CHARACTER SET utf8mb4 NOT NULL,
    `VersaoTermo` varchar(20) CHARACTER SET utf8mb4 NOT NULL,
    `ConcedidoEm` datetime(6) NOT NULL,
    `RevogadoEm` datetime(6) NULL,
    CONSTRAINT `PK_consentimentos` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_consentimentos_usuarios_UsuarioId` FOREIGN KEY (`UsuarioId`) REFERENCES `usuarios` (`Id`) ON DELETE CASCADE
) CHARACTER SET=utf8mb4;

CREATE TABLE `receitas` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `PacienteId` int NOT NULL,
    `CaminhoArquivo` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
    `NomeOriginal` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
    `TipoConteudo` varchar(50) CHARACTER SET utf8mb4 NOT NULL,
    `TamanhoBytes` bigint NOT NULL,
    `EnviadaEm` datetime(6) NOT NULL,
    `Status` varchar(20) CHARACTER SET utf8mb4 NOT NULL,
    `AnalisadaPorId` int NULL,
    `AnalisadaEm` datetime(6) NULL,
    `ValidaAte` datetime(6) NULL,
    `MotivoRecusa` varchar(500) CHARACTER SET utf8mb4 NULL,
    `PrescritorNome` varchar(150) CHARACTER SET utf8mb4 NULL,
    `PrescritorCrm` varchar(30) CHARACTER SET utf8mb4 NULL,
    `DataEmissao` datetime(6) NULL,
    CONSTRAINT `PK_receitas` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_receitas_usuarios_PacienteId` FOREIGN KEY (`PacienteId`) REFERENCES `usuarios` (`Id`) ON DELETE RESTRICT
) CHARACTER SET=utf8mb4;

CREATE TABLE `tratamentos` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `PacienteId` int NOT NULL,
    `ReceitaId` int NOT NULL,
    `CriadoEm` datetime(6) NOT NULL,
    `Ativo` tinyint(1) NOT NULL,
    CONSTRAINT `PK_tratamentos` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_tratamentos_receitas_ReceitaId` FOREIGN KEY (`ReceitaId`) REFERENCES `receitas` (`Id`) ON DELETE RESTRICT,
    CONSTRAINT `FK_tratamentos_usuarios_PacienteId` FOREIGN KEY (`PacienteId`) REFERENCES `usuarios` (`Id`) ON DELETE RESTRICT
) CHARACTER SET=utf8mb4;

CREATE TABLE `pedidos` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `PacienteId` int NOT NULL,
    `TratamentoId` int NOT NULL,
    `ReceitaId` int NOT NULL,
    `Status` varchar(30) CHARACTER SET utf8mb4 NOT NULL,
    `EnderecoEntrega` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
    `DiasCiclo` int NOT NULL,
    `CriadoEm` datetime(6) NOT NULL,
    `AtualizadoEm` datetime(6) NOT NULL,
    CONSTRAINT `PK_pedidos` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_pedidos_receitas_ReceitaId` FOREIGN KEY (`ReceitaId`) REFERENCES `receitas` (`Id`) ON DELETE RESTRICT,
    CONSTRAINT `FK_pedidos_tratamentos_TratamentoId` FOREIGN KEY (`TratamentoId`) REFERENCES `tratamentos` (`Id`) ON DELETE RESTRICT,
    CONSTRAINT `FK_pedidos_usuarios_PacienteId` FOREIGN KEY (`PacienteId`) REFERENCES `usuarios` (`Id`) ON DELETE RESTRICT
) CHARACTER SET=utf8mb4;

CREATE TABLE `saches` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `TratamentoId` int NOT NULL,
    `MedicamentoId` int NOT NULL,
    `Horario` varchar(5) CHARACTER SET utf8mb4 NOT NULL,
    `Quantidade` int NOT NULL,
    CONSTRAINT `PK_saches` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_saches_medicamentos_MedicamentoId` FOREIGN KEY (`MedicamentoId`) REFERENCES `medicamentos` (`Id`) ON DELETE RESTRICT,
    CONSTRAINT `FK_saches_tratamentos_TratamentoId` FOREIGN KEY (`TratamentoId`) REFERENCES `tratamentos` (`Id`) ON DELETE CASCADE
) CHARACTER SET=utf8mb4;

CREATE TABLE `entregas` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `PedidoId` int NOT NULL,
    `EntregadorId` int NOT NULL,
    `Tentativa` int NOT NULL,
    `IniciadaEm` datetime(6) NOT NULL,
    `ConcluidaEm` datetime(6) NULL,
    `Resultado` varchar(20) CHARACTER SET utf8mb4 NOT NULL,
    `Recebedor` varchar(150) CHARACTER SET utf8mb4 NULL,
    `Ocorrencia` varchar(500) CHARACTER SET utf8mb4 NULL,
    CONSTRAINT `PK_entregas` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_entregas_pedidos_PedidoId` FOREIGN KEY (`PedidoId`) REFERENCES `pedidos` (`Id`) ON DELETE CASCADE,
    CONSTRAINT `FK_entregas_usuarios_EntregadorId` FOREIGN KEY (`EntregadorId`) REFERENCES `usuarios` (`Id`) ON DELETE RESTRICT
) CHARACTER SET=utf8mb4;

CREATE TABLE `historico_pedido` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `PedidoId` int NOT NULL,
    `De` varchar(30) CHARACTER SET utf8mb4 NULL,
    `Para` varchar(30) CHARACTER SET utf8mb4 NOT NULL,
    `UsuarioId` int NULL,
    `Em` datetime(6) NOT NULL,
    `Observacao` varchar(500) CHARACTER SET utf8mb4 NULL,
    CONSTRAINT `PK_historico_pedido` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_historico_pedido_pedidos_PedidoId` FOREIGN KEY (`PedidoId`) REFERENCES `pedidos` (`Id`) ON DELETE CASCADE
) CHARACTER SET=utf8mb4;

CREATE TABLE `itens_pedido` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `PedidoId` int NOT NULL,
    `MedicamentoId` int NOT NULL,
    `Quantidade` int NOT NULL,
    CONSTRAINT `PK_itens_pedido` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_itens_pedido_medicamentos_MedicamentoId` FOREIGN KEY (`MedicamentoId`) REFERENCES `medicamentos` (`Id`) ON DELETE RESTRICT,
    CONSTRAINT `FK_itens_pedido_pedidos_PedidoId` FOREIGN KEY (`PedidoId`) REFERENCES `pedidos` (`Id`) ON DELETE CASCADE
) CHARACTER SET=utf8mb4;

CREATE TABLE `movimentacoes_estoque` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `LoteId` int NOT NULL,
    `Tipo` varchar(20) CHARACTER SET utf8mb4 NOT NULL,
    `Quantidade` int NOT NULL,
    `PedidoId` int NULL,
    `UsuarioId` int NULL,
    `Em` datetime(6) NOT NULL,
    `Observacao` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
    CONSTRAINT `PK_movimentacoes_estoque` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_movimentacoes_estoque_lotes_LoteId` FOREIGN KEY (`LoteId`) REFERENCES `lotes` (`Id`) ON DELETE RESTRICT,
    CONSTRAINT `FK_movimentacoes_estoque_pedidos_PedidoId` FOREIGN KEY (`PedidoId`) REFERENCES `pedidos` (`Id`) ON DELETE RESTRICT
) CHARACTER SET=utf8mb4;

CREATE TABLE `alocacoes_lote` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `PedidoItemId` int NOT NULL,
    `LoteId` int NOT NULL,
    `Quantidade` int NOT NULL,
    CONSTRAINT `PK_alocacoes_lote` PRIMARY KEY (`Id`),
    CONSTRAINT `FK_alocacoes_lote_itens_pedido_PedidoItemId` FOREIGN KEY (`PedidoItemId`) REFERENCES `itens_pedido` (`Id`) ON DELETE CASCADE,
    CONSTRAINT `FK_alocacoes_lote_lotes_LoteId` FOREIGN KEY (`LoteId`) REFERENCES `lotes` (`Id`) ON DELETE RESTRICT
) CHARACTER SET=utf8mb4;

CREATE INDEX `IX_alocacoes_lote_LoteId` ON `alocacoes_lote` (`LoteId`);

CREATE INDEX `IX_alocacoes_lote_PedidoItemId` ON `alocacoes_lote` (`PedidoItemId`);

CREATE INDEX `IX_consentimentos_UsuarioId` ON `consentimentos` (`UsuarioId`);

CREATE INDEX `IX_entregas_EntregadorId` ON `entregas` (`EntregadorId`);

CREATE INDEX `IX_entregas_PedidoId` ON `entregas` (`PedidoId`);

CREATE INDEX `IX_historico_pedido_PedidoId` ON `historico_pedido` (`PedidoId`);

CREATE INDEX `IX_itens_pedido_MedicamentoId` ON `itens_pedido` (`MedicamentoId`);

CREATE INDEX `IX_itens_pedido_PedidoId` ON `itens_pedido` (`PedidoId`);

CREATE UNIQUE INDEX `IX_lotes_MedicamentoId_Numero` ON `lotes` (`MedicamentoId`, `Numero`);

CREATE INDEX `IX_lotes_Validade` ON `lotes` (`Validade`);

CREATE INDEX `IX_movimentacoes_estoque_Em` ON `movimentacoes_estoque` (`Em`);

CREATE INDEX `IX_movimentacoes_estoque_LoteId` ON `movimentacoes_estoque` (`LoteId`);

CREATE INDEX `IX_movimentacoes_estoque_PedidoId` ON `movimentacoes_estoque` (`PedidoId`);

CREATE INDEX `IX_pedidos_PacienteId` ON `pedidos` (`PacienteId`);

CREATE INDEX `IX_pedidos_ReceitaId` ON `pedidos` (`ReceitaId`);

CREATE INDEX `IX_pedidos_Status` ON `pedidos` (`Status`);

CREATE INDEX `IX_pedidos_TratamentoId` ON `pedidos` (`TratamentoId`);

CREATE INDEX `IX_receitas_PacienteId` ON `receitas` (`PacienteId`);

CREATE INDEX `IX_receitas_Status` ON `receitas` (`Status`);

CREATE INDEX `IX_saches_MedicamentoId` ON `saches` (`MedicamentoId`);

CREATE INDEX `IX_saches_TratamentoId` ON `saches` (`TratamentoId`);

CREATE INDEX `IX_tratamentos_PacienteId` ON `tratamentos` (`PacienteId`);

CREATE INDEX `IX_tratamentos_ReceitaId` ON `tratamentos` (`ReceitaId`);

CREATE UNIQUE INDEX `IX_usuarios_Cpf` ON `usuarios` (`Cpf`);

CREATE UNIQUE INDEX `IX_usuarios_Email` ON `usuarios` (`Email`);

INSERT INTO `__EFMigrationsHistory` (`MigrationId`, `ProductVersion`)
VALUES ('20261123015746_Inicial', '8.0.8');

COMMIT;

-- ===================== PARTE 2: programabilidade =====================
DELIMITER $$
-- =====================================================================
-- MedFlow Solutions - PIM IV - Programabilidade do banco (MySQL 8.4)
-- Restrições CHECK, tabela de alertas, visões, procedimentos e gatilhos.
-- Fonte única: aplicado pela migração "Programabilidade" e também publicado em
-- database/medflow_schema.sql (junto com o DDL gerado pelo EF Core).
-- Convenção do arquivo: cada comando termina com um duplo cifrão (o mesmo delimitador usado com DELIMITER no cliente mysql).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. RESTRIÇÕES DE INTEGRIDADE (CHECK)
-- ---------------------------------------------------------------------
ALTER TABLE lotes ADD CONSTRAINT ck_lote_saldo
  CHECK (QuantidadeFisica >= 0 AND QuantidadeReservada >= 0 AND QuantidadeReservada <= QuantidadeFisica)$$
ALTER TABLE lotes ADD CONSTRAINT ck_lote_datas CHECK (Validade > Fabricacao)$$
ALTER TABLE medicamentos ADD CONSTRAINT ck_med_minimo CHECK (NivelMinimo >= 0)$$
ALTER TABLE itens_pedido ADD CONSTRAINT ck_item_qtd CHECK (Quantidade > 0)$$
ALTER TABLE alocacoes_lote ADD CONSTRAINT ck_aloc_qtd CHECK (Quantidade > 0)$$
ALTER TABLE saches ADD CONSTRAINT ck_sache_dose CHECK (Quantidade BETWEEN 1 AND 4)$$
ALTER TABLE movimentacoes_estoque ADD CONSTRAINT ck_mov_qtd CHECK (Quantidade > 0)$$

-- ---------------------------------------------------------------------
-- 2. TABELA DE ALERTAS (alimentada pelos gatilhos)
-- ---------------------------------------------------------------------
CREATE TABLE alertas_estoque (
  Id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  MedicamentoId INT NOT NULL,
  Tipo          VARCHAR(20) NOT NULL,
  Descricao     VARCHAR(200) NOT NULL,
  CriadoEm      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_alerta_med FOREIGN KEY (MedicamentoId) REFERENCES medicamentos(Id) ON DELETE CASCADE,
  INDEX ix_alerta_med (MedicamentoId, CriadoEm)
) CHARACTER SET=utf8mb4$$

-- ---------------------------------------------------------------------
-- 3. VISÕES (alimentam o painel gerencial)
-- ---------------------------------------------------------------------
CREATE VIEW vw_estoque_disponivel AS
SELECT m.Id AS MedicamentoId, m.Nome, m.Concentracao, m.NivelMinimo,
       COALESCE(SUM(CASE WHEN l.Validade >= CURDATE() THEN l.QuantidadeFisica - l.QuantidadeReservada END), 0) AS Disponivel,
       COALESCE(SUM(l.QuantidadeFisica), 0) AS Fisico,
       COALESCE(SUM(l.QuantidadeReservada), 0) AS Reservado
FROM medicamentos m LEFT JOIN lotes l ON l.MedicamentoId = m.Id
GROUP BY m.Id, m.Nome, m.Concentracao, m.NivelMinimo$$

CREATE VIEW vw_estoque_critico AS
SELECT * FROM vw_estoque_disponivel WHERE Disponivel <= NivelMinimo$$

CREATE VIEW vw_lotes_a_vencer AS
SELECT l.Id AS LoteId, m.Nome, l.Numero, l.Validade, DATEDIFF(l.Validade, CURDATE()) AS DiasParaVencer, l.QuantidadeFisica
FROM lotes l JOIN medicamentos m ON m.Id = l.MedicamentoId
WHERE l.QuantidadeFisica > 0 AND l.Validade <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)$$

CREATE VIEW vw_top_medicamentos AS
SELECT m.Id AS MedicamentoId, m.Nome, SUM(i.Quantidade) AS TotalPedido
FROM itens_pedido i
JOIN pedidos p ON p.Id = i.PedidoId AND p.Status NOT IN ('Cancelado', 'Recusado')
JOIN medicamentos m ON m.Id = i.MedicamentoId
GROUP BY m.Id, m.Nome
ORDER BY TotalPedido DESC$$

-- ---------------------------------------------------------------------
-- 4. PROCEDIMENTOS ARMAZENADOS (regra de estoque atômica, em transação)
-- ---------------------------------------------------------------------

-- Reserva, por FEFO, o estoque de todos os itens de um pedido. Tudo ou nada.
CREATE PROCEDURE sp_reservar_pedido(IN p_pedido INT, IN p_usuario INT)
BEGIN
  DECLARE v_item INT; DECLARE v_med INT; DECLARE v_qtd INT; DECLARE v_restante INT;
  DECLARE v_lote INT; DECLARE v_disp INT; DECLARE v_aloc INT;
  DECLARE fim_itens INT DEFAULT 0;
  DECLARE c_item CURSOR FOR
    SELECT Id, MedicamentoId, Quantidade FROM itens_pedido WHERE PedidoId = p_pedido;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET fim_itens = 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  START TRANSACTION;
  OPEN c_item;
  itens: LOOP
    FETCH c_item INTO v_item, v_med, v_qtd;
    IF fim_itens = 1 THEN LEAVE itens; END IF;
    SET v_restante = v_qtd;
    BEGIN
      DECLARE fim_lotes INT DEFAULT 0;
      DECLARE c_lote CURSOR FOR
        SELECT Id, QuantidadeFisica - QuantidadeReservada FROM lotes
        WHERE MedicamentoId = v_med
          AND Validade >= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
          AND QuantidadeFisica - QuantidadeReservada > 0
        ORDER BY Validade, Id FOR UPDATE;
      DECLARE CONTINUE HANDLER FOR NOT FOUND SET fim_lotes = 1;
      OPEN c_lote;
      lotes: LOOP
        FETCH c_lote INTO v_lote, v_disp;
        IF fim_lotes = 1 OR v_restante = 0 THEN LEAVE lotes; END IF;
        SET v_aloc = LEAST(v_restante, v_disp);
        UPDATE lotes SET QuantidadeReservada = QuantidadeReservada + v_aloc, Versao = Versao + 1 WHERE Id = v_lote;
        INSERT INTO alocacoes_lote (PedidoItemId, LoteId, Quantidade) VALUES (v_item, v_lote, v_aloc);
        INSERT INTO movimentacoes_estoque (LoteId, Tipo, Quantidade, PedidoId, UsuarioId, Em, Observacao)
          VALUES (v_lote, 'Reserva', v_aloc, p_pedido, p_usuario, UTC_TIMESTAMP(6), 'Reserva na confirmacao do pedido');
        SET v_restante = v_restante - v_aloc;
      END LOOP lotes;
      CLOSE c_lote;
    END;
    IF v_restante > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estoque insuficiente para um dos itens do pedido';
    END IF;
  END LOOP itens;
  CLOSE c_item;
  COMMIT;
END$$

-- Baixa definitiva dos lotes reservados ao finalizar a Box; só aceita pedido Em separação.
CREATE PROCEDURE sp_baixar_pedido(IN p_pedido INT, IN p_usuario INT)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF (SELECT Status FROM pedidos WHERE Id = p_pedido) <> 'EmSeparacao' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Pedido nao esta em separacao';
  END IF;
  START TRANSACTION;
  UPDATE lotes l
    JOIN (SELECT a.LoteId, SUM(a.Quantidade) AS Qtd
          FROM alocacoes_lote a JOIN itens_pedido i ON i.Id = a.PedidoItemId
          WHERE i.PedidoId = p_pedido GROUP BY a.LoteId) x ON x.LoteId = l.Id
    SET l.QuantidadeFisica = l.QuantidadeFisica - x.Qtd,
        l.QuantidadeReservada = l.QuantidadeReservada - x.Qtd,
        l.Versao = l.Versao + 1;
  INSERT INTO movimentacoes_estoque (LoteId, Tipo, Quantidade, PedidoId, UsuarioId, Em, Observacao)
    SELECT a.LoteId, 'Baixa', a.Quantidade, p_pedido, p_usuario, UTC_TIMESTAMP(6), 'Baixa na finalizacao da Box'
    FROM alocacoes_lote a JOIN itens_pedido i ON i.Id = a.PedidoItemId WHERE i.PedidoId = p_pedido;
  INSERT INTO historico_pedido (PedidoId, De, Para, UsuarioId, Em, Observacao)
    VALUES (p_pedido, 'EmSeparacao', 'AguardandoEntregador', p_usuario, UTC_TIMESTAMP(6), 'Box finalizada com baixa de estoque');
  UPDATE pedidos SET Status = 'AguardandoEntregador', AtualizadoEm = UTC_TIMESTAMP(6) WHERE Id = p_pedido;
  COMMIT;
END$$

-- Devolve a reserva ao disponível em recusa da receita ou cancelamento (pedido Pendente).
CREATE PROCEDURE sp_liberar_reserva(IN p_pedido INT, IN p_usuario INT, IN p_novo_status VARCHAR(20))
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF p_novo_status NOT IN ('Recusado', 'Cancelado') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Status de liberacao invalido';
  END IF;
  IF (SELECT Status FROM pedidos WHERE Id = p_pedido) <> 'Pendente' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Somente pedido pendente pode ter a reserva liberada';
  END IF;
  START TRANSACTION;
  UPDATE lotes l
    JOIN (SELECT a.LoteId, SUM(a.Quantidade) AS Qtd
          FROM alocacoes_lote a JOIN itens_pedido i ON i.Id = a.PedidoItemId
          WHERE i.PedidoId = p_pedido GROUP BY a.LoteId) x ON x.LoteId = l.Id
    SET l.QuantidadeReservada = l.QuantidadeReservada - x.Qtd, l.Versao = l.Versao + 1;
  INSERT INTO movimentacoes_estoque (LoteId, Tipo, Quantidade, PedidoId, UsuarioId, Em, Observacao)
    SELECT a.LoteId, 'LiberacaoReserva', a.Quantidade, p_pedido, p_usuario, UTC_TIMESTAMP(6), 'Reserva liberada'
    FROM alocacoes_lote a JOIN itens_pedido i ON i.Id = a.PedidoItemId WHERE i.PedidoId = p_pedido;
  INSERT INTO historico_pedido (PedidoId, De, Para, UsuarioId, Em, Observacao)
    VALUES (p_pedido, 'Pendente', p_novo_status, p_usuario, UTC_TIMESTAMP(6), 'Reserva liberada');
  UPDATE pedidos SET Status = p_novo_status, AtualizadoEm = UTC_TIMESTAMP(6) WHERE Id = p_pedido;
  COMMIT;
END$$

-- Entrada de lote com a movimentação correspondente.
CREATE PROCEDURE sp_registrar_entrada_lote(IN p_med INT, IN p_numero VARCHAR(40), IN p_fab DATE,
                                           IN p_val DATE, IN p_qtd INT, IN p_usuario INT)
BEGIN
  DECLARE v_lote INT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF p_val < CURDATE() THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Lote vencido nao pode dar entrada'; END IF;
  IF p_qtd <= 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Quantidade deve ser positiva'; END IF;
  START TRANSACTION;
  INSERT INTO lotes (MedicamentoId, Numero, Fabricacao, Validade, QuantidadeFisica, QuantidadeReservada, Versao)
    VALUES (p_med, p_numero, p_fab, p_val, p_qtd, 0, 0);
  SET v_lote = LAST_INSERT_ID();
  INSERT INTO movimentacoes_estoque (LoteId, Tipo, Quantidade, PedidoId, UsuarioId, Em, Observacao)
    VALUES (v_lote, 'Entrada', p_qtd, NULL, p_usuario, UTC_TIMESTAMP(6), 'Entrada de lote');
  COMMIT;
END$$

-- ---------------------------------------------------------------------
-- 5. GATILHOS
-- ---------------------------------------------------------------------

-- RN08: o banco também recusa transições de status fora da máquina de estados.
CREATE TRIGGER trg_pedidos_valida_transicao BEFORE UPDATE ON pedidos FOR EACH ROW
BEGIN
  IF NEW.Status <> OLD.Status AND NOT (
       (OLD.Status = 'Pendente'             AND NEW.Status IN ('EmSeparacao', 'Recusado', 'Cancelado')) OR
       (OLD.Status = 'EmSeparacao'          AND NEW.Status = 'AguardandoEntregador') OR
       (OLD.Status = 'AguardandoEntregador' AND NEW.Status = 'EmRota') OR
       (OLD.Status = 'EmRota'               AND NEW.Status IN ('Entregue', 'AguardandoEntregador'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transicao de status nao permitida';
  END IF;
END$$

-- RN20: o histórico de movimentações de estoque é imutável.
CREATE TRIGGER trg_mov_imutavel_upd BEFORE UPDATE ON movimentacoes_estoque FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Movimentacoes de estoque sao imutaveis'$$

CREATE TRIGGER trg_mov_imutavel_del BEFORE DELETE ON movimentacoes_estoque FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Movimentacoes de estoque sao imutaveis'$$

-- RN18: registra alerta quando o disponível (lotes não vencidos) cruza o nível mínimo.
CREATE TRIGGER trg_lotes_alerta_minimo AFTER UPDATE ON lotes FOR EACH ROW
BEGIN
  DECLARE v_disp INT; DECLARE v_min INT; DECLARE v_antes INT;
  SELECT COALESCE(SUM(QuantidadeFisica - QuantidadeReservada), 0) INTO v_disp
    FROM lotes WHERE MedicamentoId = NEW.MedicamentoId AND Validade >= CURDATE();
  SELECT NivelMinimo INTO v_min FROM medicamentos WHERE Id = NEW.MedicamentoId;
  SET v_antes = v_disp - (NEW.QuantidadeFisica - NEW.QuantidadeReservada) + (OLD.QuantidadeFisica - OLD.QuantidadeReservada);
  IF v_disp <= v_min AND v_antes > v_min THEN
    INSERT INTO alertas_estoque (MedicamentoId, Tipo, Descricao)
      VALUES (NEW.MedicamentoId, 'EstoqueMinimo', CONCAT('Disponivel ', v_disp, ' abaixo ou igual ao minimo ', v_min));
  END IF;
END$$

-- RN18: lote que entra já perto do vencimento gera alerta.
CREATE TRIGGER trg_lotes_alerta_validade AFTER INSERT ON lotes FOR EACH ROW
BEGIN
  IF NEW.Validade <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN
    INSERT INTO alertas_estoque (MedicamentoId, Tipo, Descricao)
      VALUES (NEW.MedicamentoId, 'LoteAVencer', CONCAT('Lote ', NEW.Numero, ' vence em ', DATE_FORMAT(NEW.Validade, '%Y-%m-%d')));
  END IF;
END$$
DELIMITER ;
