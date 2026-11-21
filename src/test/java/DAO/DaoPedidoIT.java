package DAO;

import Model.Bebida;
import Model.Cliente;
import Model.Lanche;
import Model.Pedido;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaoPedidoIT {

    private Connection jdbcConnection;
    private Connection daoConnection;
    private DaoPedido daoPedido;

    private String sufixo;
    private int idCliente;
    private int idLanche;
    private int idBebida;

    @BeforeEach
    void preparar() throws SQLException {
        jdbcConnection = new DaoUtil().conecta();
        daoConnection = new DaoUtil().conecta();
        daoPedido = new DaoPedido(daoConnection);

        sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        idCliente = inserirCliente("it_ped_" + sufixo);
        idLanche = inserirLanche("LancheIT-" + sufixo, 22.50);
        idBebida = inserirBebida("BebidaIT-" + sufixo, 7.50);
    }

    @AfterEach
    void limpar() throws SQLException {
        try {
            if (jdbcConnection != null && !jdbcConnection.isClosed()) {
                executarDelete(
                        "DELETE FROM tb_lanches_pedido "
                                + "WHERE id_pedido IN (SELECT id_pedido FROM tb_pedidos WHERE id_cliente = ?) "
                                + "OR id_lanche = ?",
                        idCliente, idLanche);
                executarDelete(
                        "DELETE FROM tb_bebidas_pedido "
                                + "WHERE id_pedido IN (SELECT id_pedido FROM tb_pedidos WHERE id_cliente = ?) "
                                + "OR id_bebida = ?",
                        idCliente, idBebida);
                executarDelete("DELETE FROM tb_pedidos WHERE id_cliente = ?", idCliente);
                executarDelete("DELETE FROM tb_lanches WHERE id_lanche = ?", idLanche);
                executarDelete("DELETE FROM tb_bebidas WHERE id_bebida = ?", idBebida);
                executarDelete("DELETE FROM tb_clientes WHERE id_cliente = ?", idCliente);
            }
        } finally {
            fecharSilenciosamente(daoConnection);
            fecharSilenciosamente(jdbcConnection);
        }
    }

    private int inserirCliente(String usuario) throws SQLException {
        String sql = "INSERT INTO tb_clientes(nome, sobrenome, telefone, usuario, senha, fg_ativo, id_endereco) "
                + "VALUES(?,?,?,?,?,1,NULL) RETURNING id_cliente";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, "Cliente");
            stmt.setString(2, "Integracao");
            stmt.setString(3, "41999990000");
            stmt.setString(4, usuario);
            stmt.setString(5, "e10adc3949ba59abbe56e057f20f883e");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Cliente de teste deve gerar id_cliente");
                return rs.getInt(1);
            }
        }
    }

    private int inserirLanche(String nome, double valorVenda) throws SQLException {
        String sql = "INSERT INTO tb_lanches(nm_lanche, descricao, valor_venda, fg_ativo) "
                + "VALUES(?,?,?,1) RETURNING id_lanche";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, "Lanche para teste de integracao");
            stmt.setDouble(3, valorVenda);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Lanche de teste deve gerar id_lanche");
                return rs.getInt(1);
            }
        }
    }

    private int inserirBebida(String nome, double valorVenda) throws SQLException {
        String sql = "INSERT INTO tb_bebidas(nm_bebida, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo) "
                + "VALUES(?,?,?,?,?,?,1) RETURNING id_bebida";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, "Bebida para teste de integracao");
            stmt.setInt(3, 50);
            stmt.setDouble(4, 3.00);
            stmt.setDouble(5, valorVenda);
            stmt.setString(6, "Refrigerante");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Bebida de teste deve gerar id_bebida");
                return rs.getInt(1);
            }
        }
    }

    private void executarDelete(String sql, int... parametros) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setInt(i + 1, parametros[i]);
            }
            stmt.executeUpdate();
        }
    }

    private void fecharSilenciosamente(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    private Pedido criarPedido(String dataPedido, double valorTotal) {
        Cliente cliente = new Cliente();
        cliente.setId_cliente(idCliente);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setData_pedido(dataPedido);
        pedido.setValor_total(valorTotal);
        return pedido;
    }

    @Test
    @DisplayName("Deve salvar pedido no PostgreSQL e recuperá-lo via pesquisaPorData")
    void deveSalvarEPesquisarPedidoPorDataNoPostgres() throws SQLException {
        String dataPedido = "2026-11-21T12:00:00Z-" + sufixo;
        Pedido pedido = criarPedido(dataPedido, 30.00);

        daoPedido.salvar(pedido);
        Pedido encontrado = daoPedido.pesquisaPorData(pedido);

        assertTrue(encontrado.getId_pedido() > 0, "id_pedido gerado deve ser positivo");
        assertEquals(dataPedido, encontrado.getData_pedido());
        assertEquals(30.00, encontrado.getValor_total(), 0.001);

        String sql = "SELECT id_pedido, id_cliente, data_pedido, valor_total "
                + "FROM tb_pedidos WHERE id_pedido = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setInt(1, encontrado.getId_pedido());
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Pedido salvo deve existir na tabela tb_pedidos");
                assertEquals(idCliente, rs.getInt("id_cliente"));
                assertEquals(dataPedido, rs.getString("data_pedido"));
                assertEquals(30.00, rs.getDouble("valor_total"), 0.001);
                assertFalse(rs.next(), "Deve existir apenas um registro para o id_pedido");
            }
        }
    }

    @Test
    @DisplayName("Deve retornar Pedido vazio quando data_pedido não existir no banco")
    void deveRetornarPedidoVazioQuandoDataNaoExistir() {
        Pedido filtro = new Pedido();
        filtro.setData_pedido("nao-existe-" + sufixo);

        Pedido encontrado = daoPedido.pesquisaPorData(filtro);

        assertEquals(0, encontrado.getId_pedido());
        assertNull(encontrado.getData_pedido());
        assertNull(encontrado.getValor_total());
    }

    @Test
    @DisplayName("Deve vincular lanche ao pedido na tabela tb_lanches_pedido")
    void deveVincularLancheAoPedidoNaTabelaDeJuncao() throws SQLException {
        String dataPedido = "2026-11-21T12:05:00Z-" + sufixo;
        Pedido pedido = criarPedido(dataPedido, 22.50);
        daoPedido.salvar(pedido);

        Pedido pedidoSalvo = daoPedido.pesquisaPorData(pedido);
        assertTrue(pedidoSalvo.getId_pedido() > 0);

        Lanche lanche = new Lanche();
        lanche.setId_lanche(idLanche);
        lanche.setQuantidade(2);

        daoPedido.vincularLanche(pedidoSalvo, lanche);

        String sql = "SELECT quantidade FROM tb_lanches_pedido WHERE id_pedido = ? AND id_lanche = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setInt(1, pedidoSalvo.getId_pedido());
            stmt.setInt(2, idLanche);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Vínculo do lanche deve existir em tb_lanches_pedido");
                assertEquals(2, rs.getInt("quantidade"));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    @DisplayName("Deve vincular bebida ao pedido na tabela tb_bebidas_pedido")
    void deveVincularBebidaAoPedidoNaTabelaDeJuncao() throws SQLException {
        String dataPedido = "2026-11-21T12:10:00Z-" + sufixo;
        Pedido pedido = criarPedido(dataPedido, 7.50);
        daoPedido.salvar(pedido);

        Pedido pedidoSalvo = daoPedido.pesquisaPorData(pedido);
        assertTrue(pedidoSalvo.getId_pedido() > 0);

        Bebida bebida = new Bebida();
        bebida.setId_bebida(idBebida);
        bebida.setQuantidade(3);

        daoPedido.vincularBebida(pedidoSalvo, bebida);

        String sql = "SELECT quantidade FROM tb_bebidas_pedido WHERE id_pedido = ? AND id_bebida = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setInt(1, pedidoSalvo.getId_pedido());
            stmt.setInt(2, idBebida);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Vínculo da bebida deve existir em tb_bebidas_pedido");
                assertEquals(3, rs.getInt("quantidade"));
                assertFalse(rs.next());
            }
        }
    }
}
