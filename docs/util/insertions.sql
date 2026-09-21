-- ===================================================
-- 1. Endereços
-- ===================================================
INSERT INTO tb_enderecos (rua, bairro, numero, complemento, cidade, estado) VALUES
                                                                                ('Rua das Flores', 'Centro', 123, 'Apto 101', 'Curitiba', 'PR'),
                                                                                ('Av. Sete de Setembro', 'Batel', 4560, 'Bloco B', 'Curitiba', 'PR'),
                                                                                ('Rua Marechal Deodoro', 'Alto da XV', 789, NULL, 'Curitiba', 'PR');

-- ===================================================
-- 2. Clientes (Senha padrão: "123456" -> MD5: e10adc3949ba59abbe56e057f20f883e)
-- ===================================================
INSERT INTO tb_clientes (nome, sobrenome, telefone, usuario, senha, fg_ativo, id_endereco) VALUES
                                                                                               ('Carlos', 'Silva', '41991234567', 'carlos.silva', 'e10adc3949ba59abbe56e057f20f883e', 1, 1),
                                                                                               ('Mariana', 'Costa', '41998765432', 'mariana.costa', 'e10adc3949ba59abbe56e057f20f883e', 1, 2),
                                                                                               ('Lucas', 'Pereira', '41987651234', 'lucas.pereira', 'e10adc3949ba59abbe56e057f20f883e', 1, 3);

-- ===================================================
-- 3. Ingredientes
-- ===================================================
INSERT INTO tb_ingredientes (nm_ingrediente, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo) VALUES
                                                                                                                   ('Pão com Gergelim', 'Pão brioche com gergelim', 100, 1.20, 2.50, 'Pão', 1),
                                                                                                                   ('Hambúrguer de Carne 160g', 'Blend bovino artesanal', 80, 4.50, 9.00, 'Carne', 1),
                                                                                                                   ('Queijo Cheddar', 'Fatia de queijo cheddar inglês', 150, 0.80, 2.00, 'Queijo', 1),
                                                                                                                   ('Bacon Crocante', 'Fatias de bacon defumado crocante', 60, 1.50, 4.00, 'Carne', 1),
                                                                                                                   ('Alface Americana', 'Folhas frescas de alface', 50, 0.30, 1.00, 'Salada', 1),
                                                                                                                   ('Tomate', 'Fatias de tomate selecionado', 50, 0.40, 1.00, 'Salada', 1),
                                                                                                                   ('Maionese Especial', 'Molho artesanal verde', 70, 0.50, 2.00, 'Molho', 1);

-- ===================================================
-- 4. Lanches
-- ===================================================
INSERT INTO tb_lanches (nm_lanche, descricao, valor_venda, fg_ativo) VALUES
                                                                         ('X-Burguer', 'Pão, hambúrguer de carne, queijo cheddar e maionese especial', 18.00, 1),
                                                                         ('X-Bacon', 'Pão, hambúrguer de carne, queijo cheddar, bacon crocante e maionese especial', 24.00, 1),
                                                                         ('X-Salada Especial', 'Pão, hambúrguer de carne, queijo cheddar, alface, tomate e maionese especial', 22.00, 1);

-- ===================================================
-- 5. Ingredientes do Lanche (tb_ingredientes_lanche)
-- ===================================================
-- X-Burguer (id_lanche: 1)
INSERT INTO tb_ingredientes_lanche (id_lanche, id_ingrediente, quantidade) VALUES
                                                                               (1, 1, 1), -- Pão
                                                                               (1, 2, 1), -- Carne
                                                                               (1, 3, 1), -- Cheddar
                                                                               (1, 7, 1); -- Maionese

-- X-Bacon (id_lanche: 2)
INSERT INTO tb_ingredientes_lanche (id_lanche, id_ingrediente, quantidade) VALUES
                                                                               (2, 1, 1), -- Pão
                                                                               (2, 2, 1), -- Carne
                                                                               (2, 3, 1), -- Cheddar
                                                                               (2, 4, 1), -- Bacon
                                                                               (2, 7, 1); -- Maionese

-- X-Salada Especial (id_lanche: 3)
INSERT INTO tb_ingredientes_lanche (id_lanche, id_ingrediente, quantidade) VALUES
                                                                               (3, 1, 1), -- Pão
                                                                               (3, 2, 1), -- Carne
                                                                               (3, 3, 1), -- Cheddar
                                                                               (3, 5, 1), -- Alface
                                                                               (3, 6, 1), -- Tomate
                                                                               (3, 7, 1); -- Maionese

-- ===================================================
-- 6. Bebidas
-- ===================================================
INSERT INTO tb_bebidas (nm_bebida, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo) VALUES
                                                                                                         ('Coca-Cola Lata 350ml', 'Refrigerante de cola tradicional', 100, 2.80, 6.50, 'Refrigerante', 1),
                                                                                                         ('Guaraná Antarctica 350ml', 'Refrigerante de guaraná tradicional', 80, 2.50, 6.00, 'Refrigerante', 1),
                                                                                                         ('Suco de Laranja 400ml', 'Suco natural de laranja sem açúcar', 40, 3.50, 8.00, 'Suco', 1),
                                                                                                         ('Água Mineral 500ml', 'Água mineral sem gás', 100, 1.00, 4.00, 'Água', 1);

-- ===================================================
-- 7. Pedidos Exemplo
-- ===================================================
-- Pedido 1: Cliente 1 pede 1 X-Bacon e 1 Coca-Cola (24.00 + 6.50 = 30.50)
INSERT INTO tb_pedidos (id_cliente, data_pedido, valor_total) VALUES
    (1, '2026-09-20 20:15:00', 30.50);

INSERT INTO tb_lanches_pedido (id_pedido, id_lanche, quantidade) VALUES (1, 2, 1);
INSERT INTO tb_bebidas_pedido (id_pedido, id_bebida, quantidade) VALUES (1, 1, 1);

-- Pedido 2: Cliente 2 pede 2 X-Burguer e 1 Guaraná (36.00 + 6.00 = 42.00)
INSERT INTO tb_pedidos (id_cliente, data_pedido, valor_total) VALUES
    (2, '2026-09-20 21:00:00', 42.00);

INSERT INTO tb_lanches_pedido (id_pedido, id_lanche, quantidade) VALUES (2, 1, 2);
INSERT INTO tb_bebidas_pedido (id_pedido, id_bebida, quantidade) VALUES (2, 2, 1);