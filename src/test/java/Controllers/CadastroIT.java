package Controllers;

import DAO.DaoCliente;
import DAO.DaoUtil;
import Model.Cliente;
import apoio.CorpoRequisicao;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Teste de integração do cadastro de cliente (CT-03 / história U4).
 *
 * Exercita juntos, sem mocks nos DAOs: servlet cadastro -> DaoCliente -> DaoEndereco -> PostgreSQL 13.
 * Só a requisição e a resposta HTTP são simuladas, porque o Tomcat não sobe neste teste.
 * Usa os construtores padrão, que conectam pelo DaoUtil (host "db"); por isso precisa rodar na rede
 * do docker compose: mvn -Pintegracao verify
 *
 * Cada teste cria dados com um sufixo aleatório e apaga tudo no @AfterEach.
 */
@ExtendWith(MockitoExtension.class)
class CadastroIT {

    private static final String SENHA = "123456";
    // MD5("123456"), o mesmo valor que o banco.sql usa para os clientes de exemplo
    private static final String HASH_SENHA = "e10adc3949ba59abbe56e057f20f883e";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private Connection jdbcConnection;
    private String sufixo;
    private String usuario;
    private String rua;

    @BeforeEach
    void preparar() {
        jdbcConnection = new DaoUtil().conecta();
        sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        usuario = "it_cad_" + sufixo;
        rua = "Rua Integracao " + sufixo;
    }

    @AfterEach
    void limpar() throws SQLException {
        try {
            if (jdbcConnection != null && !jdbcConnection.isClosed()) {
                // clientes primeiro, por causa da chave estrangeira para tb_enderecos
                executarDelete("DELETE FROM tb_clientes WHERE usuario = ?", usuario);
                executarDelete("DELETE FROM tb_enderecos WHERE rua = ?", rua);
            }
        } finally {
            if (jdbcConnection != null) {
                try {
                    jdbcConnection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Test
    @DisplayName("Cadastro com endereço novo grava o endereço e o cliente ligado a ele, com a senha em MD5")
    void deveGravarClienteEEnderecoNovoNoPostgres() throws Exception {
        String resposta = cadastrar(usuario, "Bia", rua, 50);

        assertTrue(resposta.contains("Cadastrado"), "o servlet deve confirmar o cadastro");

        int idEndereco = idDoEndereco(rua);
        assertTrue(idEndereco > 0, "o endereço novo deve ter sido gravado em tb_enderecos");

        String sql = "SELECT nome, sobrenome, telefone, senha, fg_ativo, id_endereco FROM tb_clientes WHERE usuario = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "o cliente deve ter sido gravado em tb_clientes");
                assertEquals("Bia", rs.getString("nome"));
                assertEquals("Souza", rs.getString("sobrenome"));
                assertEquals("21999990000", rs.getString("telefone"));
                assertEquals(HASH_SENHA, rs.getString("senha"), "a senha deve ser gravada em MD5, nunca em texto puro");
                assertEquals(1, rs.getInt("fg_ativo"), "o cadastro sempre nasce ativo");
                assertEquals(idEndereco, rs.getInt("id_endereco"), "o cliente deve apontar para o endereço gravado");
                assertFalse(rs.next(), "deve existir um único cliente com esse usuário");
            }
        }
    }

    @Test
    @DisplayName("Cadastro com endereço já existente reaproveita o endereço em vez de duplicar")
    void deveReaproveitarEnderecoExistente() throws Exception {
        int idExistente = inserirEndereco(rua, 77);

        cadastrar(usuario, "Bia", rua, 77);

        assertEquals(1, contarEnderecos(rua), "não deve criar um segundo endereço igual");
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(
                "SELECT id_endereco FROM tb_clientes WHERE usuario = ?")) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(idExistente, rs.getInt("id_endereco"));
            }
        }
    }

    @Test
    @DisplayName("Cliente cadastrado pelo servlet consegue fazer login (MD5 do banco = MD5 do Java)")
    void clienteCadastradoDeveConseguirFazerLogin() throws Exception {
        cadastrar(usuario, "Bia", rua, 50);

        // o cadastro aplica MD5() no PostgreSQL; o login aplica o EncryptadorMD5 no Java: os dois precisam bater
        assertTrue(new DaoCliente().login(tentativa(usuario, SENHA)), "a senha cadastrada deve liberar o login");
        assertFalse(new DaoCliente().login(tentativa(usuario, "senha-errada")), "outra senha deve ser recusada");
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("Usuário já cadastrado não pode ser gravado de novo (achado do CT-03, ainda sem issue)")
    void naoDeveGravarUsuarioDuplicadoNoBanco() throws Exception {
        cadastrar(usuario, "Bia", rua, 50);

        try {
            cadastrar(usuario, "Impostora", rua, 50);
        } catch (RuntimeException esperadaQuandoCorrigido) {
            // quando o defeito for corrigido, o cadastro duplicado pode ser recusado com exceção
        }

        assertEquals(1, contarClientes(usuario), "o banco deve ter um único cliente com esse usuário");
    }

    // ---------- apoio ----------

    private String cadastrar(String usuarioCadastro, String nome, String ruaCadastro, int numero) throws Exception {
        StringWriter saida = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(saida));
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(json(usuarioCadastro, nome, ruaCadastro, numero)));

        // construtor padrão: o servlet cria o DaoCliente real, que cria o DaoEndereco real
        new cadastro().doPost(request, response);

        return saida.toString();
    }

    // o servlet lê uma única linha, então o JSON não pode ter quebras de linha
    private String json(String usuarioCadastro, String nome, String ruaCadastro, int numero) {
        return "{\"usuario\": {\"nome\": \"" + nome + "\", \"sobrenome\": \"Souza\", \"telefone\": \"21999990000\", "
                + "\"usuario\": \"" + usuarioCadastro + "\", \"senha\": \"" + SENHA + "\"}, "
                + "\"endereco\": {\"rua\": \"" + ruaCadastro + "\", \"numero\": " + numero + ", \"bairro\": \"Centro\", "
                + "\"complemento\": \"Sala 2\", \"cidade\": \"Rio de Janeiro\", \"estado\": \"RJ\"}}";
    }

    private Cliente tentativa(String usuarioLogin, String senha) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuarioLogin);
        cliente.setSenha(senha);
        return cliente;
    }

    private int inserirEndereco(String ruaEndereco, int numero) throws SQLException {
        String sql = "INSERT INTO tb_enderecos(rua, bairro, numero, complemento, cidade, estado) "
                + "VALUES(?, 'Centro', ?, 'Sala 2', 'Rio de Janeiro', 'RJ') RETURNING id_endereco";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, ruaEndereco);
            stmt.setInt(2, numero);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "o endereço de teste deve gerar id_endereco");
                return rs.getInt(1);
            }
        }
    }

    private int idDoEndereco(String ruaEndereco) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(
                "SELECT id_endereco FROM tb_enderecos WHERE rua = ?")) {
            stmt.setString(1, ruaEndereco);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int contarEnderecos(String ruaEndereco) throws SQLException {
        return contar("SELECT COUNT(*) FROM tb_enderecos WHERE rua = ?", ruaEndereco);
    }

    private int contarClientes(String usuarioCliente) throws SQLException {
        return contar("SELECT COUNT(*) FROM tb_clientes WHERE usuario = ?", usuarioCliente);
    }

    private int contar(String sql, String parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, parametro);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void executarDelete(String sql, String parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, parametro);
            stmt.executeUpdate();
        }
    }
}
