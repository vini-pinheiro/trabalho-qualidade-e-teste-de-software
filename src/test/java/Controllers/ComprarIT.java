package Controllers;

import DAO.DaoUtil;
import apoio.CorpoRequisicao;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprarIT {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private Connection jdbcConnection;
    private comprar controller;

    private String sufixo;
    private int idCliente;
    private int idLanche;
    private String nomeLanche;
    private int idBebida;
    private String nomeBebida;
    private String tokenValido;

    @BeforeEach
    void preparar() throws SQLException {
        jdbcConnection = new DaoUtil().conecta();

        sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        nomeLanche = "BurgerIT-" + sufixo;
        nomeBebida = "RefriIT-" + sufixo;

        idCliente = inserirCliente("it_cmp_" + sufixo);
        idLanche = inserirLanche(nomeLanche, 20.00);
        idBebida = inserirBebida(nomeBebida, 6.50);
        tokenValido = idCliente + "-it-" + sufixo;
        inserirToken(tokenValido);

        controller = new comprar();
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
                executarDeleteString("DELETE FROM tb_tokens WHERE token = ?", tokenValido);
                executarDelete("DELETE FROM tb_lanches WHERE id_lanche = ?", idLanche);
                executarDelete("DELETE FROM tb_bebidas WHERE id_bebida = ?", idBebida);
                executarDelete("DELETE FROM tb_clientes WHERE id_cliente = ?", idCliente);
            }
        } finally {
            if (jdbcConnection != null) {
                try {
                    if (!jdbcConnection.isClosed()) {
                        jdbcConnection.close();
                    }
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private int inserirCliente(String usuario) throws SQLException {
        String sql = "INSERT INTO tb_clientes(nome, sobrenome, telefone, usuario, senha, fg_ativo, id_endereco) "
                + "VALUES(?,?,?,?,?,1,NULL) RETURNING id_cliente";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, "Cliente");
            stmt.setString(2, "Checkout");
            stmt.setString(3, "41999991111");
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
            stmt.setString(2, "Lanche para checkout real");
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
            stmt.setString(2, "Bebida para checkout real");
            stmt.setInt(3, 40);
            stmt.setDouble(4, 2.50);
            stmt.setDouble(5, valorVenda);
            stmt.setString(6, "Refrigerante");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Bebida de teste deve gerar id_bebida");
                return rs.getInt(1);
            }
        }
    }

    private void inserirToken(String token) throws SQLException {
        String sql = "INSERT INTO tb_tokens(token) VALUES(?)";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
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

    private void executarDeleteString(String sql, String parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, parametro);
            stmt.executeUpdate();
        }
    }

    private String montarPayloadCompra() {
        return "{\"id\": " + idCliente + ", \"" + nomeLanche + "\": [\"20.00\", \"lanche\", 2], \""
                + nomeBebida + "\": [\"6.50\", \"bebida\", 3]}";
    }

    private int contarRegistros(String sql, int parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setInt(1, parametro);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    @Test
    @DisplayName("Cenário 1: compra válida com cookie no banco persiste pedido e itens vinculados no PostgreSQL")
    void deveProcessarCompraValidaEPersistirPedidoEItensNoPostgres() throws Exception {
        StringWriter resposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(resposta));
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(montarPayloadCompra()));
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("token", tokenValido) });

        controller.processRequest(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertEquals("Pedido Salvo com Sucesso!", resposta.toString().trim());

        int idPedidoGerado;
        String sqlPedido = "SELECT id_pedido, data_pedido, valor_total FROM tb_pedidos WHERE id_cliente = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sqlPedido)) {
            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Pedido deve estar persistido em tb_pedidos");
                idPedidoGerado = rs.getInt("id_pedido");
                String dataPedido = rs.getString("data_pedido");
                double valorTotal = rs.getDouble("valor_total");
                assertFalse(rs.next(), "Deve existir exatamente um pedido para o cliente");

                assertTrue(idPedidoGerado > 0);
                assertNotNull(dataPedido);
                assertDoesNotThrow(() -> Instant.parse(dataPedido));
                // Preserva o comportamento atual do DEF-02: soma os preços unitários (20.00 + 6.50)
                assertEquals(26.50, valorTotal, 0.001);
            }
        }

        String sqlLanche = "SELECT quantidade FROM tb_lanches_pedido WHERE id_pedido = ? AND id_lanche = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sqlLanche)) {
            stmt.setInt(1, idPedidoGerado);
            stmt.setInt(2, idLanche);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Lanche deve estar vinculado em tb_lanches_pedido");
                assertEquals(2, rs.getInt("quantidade"));
                assertFalse(rs.next());
            }
        }

        String sqlBebida = "SELECT quantidade FROM tb_bebidas_pedido WHERE id_pedido = ? AND id_bebida = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sqlBebida)) {
            stmt.setInt(1, idPedidoGerado);
            stmt.setInt(2, idBebida);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Bebida deve estar vinculada em tb_bebidas_pedido");
                assertEquals(3, rs.getInt("quantidade"));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    @DisplayName("Cenário 2a: cookie com token inexistente retorna erro e não persiste pedido nem vínculos")
    void deveRetornarErroENaoPersistirPedidoQuandoTokenForInvalido() throws Exception {
        StringWriter resposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(resposta));
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(montarPayloadCompra()));
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("token", "token-invalido-" + sufixo) });

        controller.processRequest(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertEquals("erro", resposta.toString().trim());

        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_pedidos WHERE id_cliente = ?", idCliente));
        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_lanches_pedido WHERE id_lanche = ?", idLanche));
        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_bebidas_pedido WHERE id_bebida = ?", idBebida));
    }

    @Test
    @DisplayName("Cenário 2b: cookies ausentes retornam erro e não persistem pedido")
    void deveRetornarErroENaoPersistirPedidoQuandoCookiesForemNulos() throws Exception {
        StringWriter resposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(resposta));
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(montarPayloadCompra()));
        when(request.getCookies()).thenReturn(null);

        controller.processRequest(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertEquals("erro", resposta.toString().trim());

        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_pedidos WHERE id_cliente = ?", idCliente));
        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_lanches_pedido WHERE id_lanche = ?", idLanche));
        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_bebidas_pedido WHERE id_bebida = ?", idBebida));
    }
}
