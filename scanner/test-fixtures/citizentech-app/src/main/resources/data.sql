-- Fictitious seed data (test input only). No real person.
-- Valid check digits: must be reported (literal CPF).
INSERT INTO cidadao (id, nome, cpf, email, telefone, senha)
VALUES (1, 'Maria Exemplo', '529.982.247-25', 'maria.exemplo@bairrovivo.test', '(11) 98765-4321', 'trocar123');

-- Invalid check digits: the runner's validator must drop this match.
INSERT INTO cidadao (id, nome, cpf, email, telefone, senha)
VALUES (2, 'Registro Teste', '123.456.789-00', 'teste@example.com', '(11) 90000-0000', 'trocar123');

-- Cartão Nacional de Saúde (CNS) with a valid check digit: must be reported.
INSERT INTO triagem (id, cns, cid10, prioridade) VALUES (1, '700 0012 3456 7894', 'J11', 2);
