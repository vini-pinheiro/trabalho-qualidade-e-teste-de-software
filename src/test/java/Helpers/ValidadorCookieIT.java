package Helpers;

import DAO.DaoToken;
import DAO.DaoUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integração de ValidadorCookie com DaoToken e a tabela tb_tokens do PostgreSQL 13.
 * Os tokens são gravados como nos servlets login/loginFuncionario (id + "-" + Instant.now()) e
 * conferidos direto no banco por uma conexão JDBC separada.
 *
 * Executar com o banco do docker-compose no ar: mvn -Pintegracao verify
 * Os testes marcados com "defeito-conhecido" só rodam com: mvn -Pintegracao,defeitos verify
 */
class ValidadorCookieIT {

    private Connection jdbcConnection;
    private Connection daoConnection;
    private DaoToken tokenDao;
    private ValidadorCookie validador;

    private String sufixo;
    private final List<String> tokensCriados = new ArrayList<>();

    @BeforeEach
    void preparar() {
        jdbcConnection = new DaoUtil().conecta();
        daoConnection = new DaoUtil().conecta();
        tokenDao = new DaoToken(daoConnection);
        validador = new ValidadorCookie(tokenDao);
        sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @AfterEach
    void limpar() throws SQLException {
        try {
            for (String token : tokensCriados) {
                try (PreparedStatement stmt = jdbcConnection.prepareStatement("DELETE FROM tb_tokens WHERE token = ?")) {
                    stmt.setString(1, token);
                    stmt.executeUpdate();
                }
            }
        } finally {
            fecharSilenciosamente(daoConnection);
            fecharSilenciosamente(jdbcConnection);
        }
    }

    private String loginGravandoToken(int id, Instant instante) {
        String token = id + "-" + instante + "-" + sufixo;
        tokensCriados.add(token);
        tokenDao.salvar(token);
        return token;
    }

    private int contarNoBanco(String token) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement("SELECT COUNT(*) FROM tb_tokens WHERE token = ?")) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static Cookie[] cookies(Cookie... cookies) {
        return cookies;
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

    @Test
    @DisplayName("Construtor padrão (o usado pelos servlets) conecta ao PostgreSQL e aceita token gravado no login")
    void construtorPadraoDeveValidarTokenGravadoNoBanco() {
        String token = loginGravandoToken(7, Instant.now());

        ValidadorCookie validadorDosServlets = new ValidadorCookie();

        assertTrue(validadorDosServlets.validar(cookies(new Cookie("token", token))));
    }

    @Test
    @DisplayName("Ciclo do cliente: login grava, validar aceita, logout apaga e o cookie antigo passa a ser recusado")
    void deveCumprirCicloDeVidaDoTokenDeCliente() throws SQLException {
        String token = loginGravandoToken(7, Instant.now());
        Cookie[] navegador = cookies(new Cookie("token", token));

        assertEquals(1, contarNoBanco(token));
        assertTrue(validador.validar(navegador));
        assertEquals("7", validador.getCookieIdCliente(navegador));

        validador.deletar(navegador);

        assertEquals(0, contarNoBanco(token));
        assertFalse(validador.validar(navegador), "Cookie reapresentado depois do logout não pode valer");
    }

    @Test
    @DisplayName("Ciclo do funcionário: login grava, validarFuncionario aceita, logout apaga e o cookie antigo é recusado")
    void deveCumprirCicloDeVidaDoTokenDeFuncionario() throws SQLException {
        String token = loginGravandoToken(1, Instant.now());
        Cookie[] navegador = cookies(new Cookie("tokenFuncionario", token));

        assertTrue(validador.validarFuncionario(navegador));
        assertEquals("1", validador.getCookieIdFuncionario(navegador));

        validador.deletar(navegador);

        assertEquals(0, contarNoBanco(token));
        assertFalse(validador.validarFuncionario(navegador));
    }

    @Test
    @DisplayName("Token com formato válido mas ausente da tb_tokens é recusado")
    void deveRecusarTokenQueNuncaFoiGravado() {
        String forjado = "7-" + Instant.now() + "-" + sufixo;

        assertFalse(validador.validar(cookies(new Cookie("token", forjado))));
        assertFalse(validador.validarFuncionario(cookies(new Cookie("tokenFuncionario", forjado))));
    }

    @Test
    @DisplayName("A comparação no banco é exata: o token com um espaço a mais é recusado")
    void deveExigirCorrespondenciaExataDoToken() {
        String token = loginGravandoToken(7, Instant.now());

        assertFalse(validador.validar(cookies(new Cookie("token", token + " "))));
        assertFalse(validador.validar(cookies(new Cookie("token", token.toUpperCase()))));
    }

    @Test
    @DisplayName("Logout com os dois perfis apaga os dois tokens")
    void deveApagarOsDoisTokensNoLogout() throws SQLException {
        String tokenCliente = loginGravandoToken(7, Instant.now());
        String tokenFuncionario = loginGravandoToken(1, Instant.now());

        validador.deletar(cookies(new Cookie("JSESSIONID", "A1B2C3"), new Cookie("token", tokenCliente),
            new Cookie("tokenFuncionario", tokenFuncionario)));

        assertEquals(0, contarNoBanco(tokenCliente));
        assertEquals(0, contarNoBanco(tokenFuncionario));
    }

    @Test
    @DisplayName("Logout de um cliente não derruba a sessão de outro")
    void logoutNaoDeveAfetarSessaoDeOutroCliente() throws SQLException {
        String tokenAna = loginGravandoToken(7, Instant.now());
        String tokenBruno = loginGravandoToken(8, Instant.now());

        validador.deletar(cookies(new Cookie("token", tokenAna)));

        assertEquals(0, contarNoBanco(tokenAna));
        assertEquals(1, contarNoBanco(tokenBruno));
        assertTrue(validador.validar(cookies(new Cookie("token", tokenBruno))));
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("DEFEITO: token de cliente copiado para o cookie tokenFuncionario não pode dar acesso de funcionário")
    void naoDeveAceitarTokenDeClienteComoFuncionario() {
        // tb_tokens guarda os dois perfis sem distinção, então validarFuncionario aceita qualquer token gravado
        String tokenDoCliente = loginGravandoToken(7, Instant.now());

        assertFalse(validador.validarFuncionario(cookies(new Cookie("tokenFuncionario", tokenDoCliente))),
            "Cliente obteve acesso às rotas de funcionário (relatórios, cadastro de funcionário, estoque)");
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("DEFEITO: token emitido há mais de 30 minutos (MaxAge do cookie) deve ser recusado pelo servidor")
    void deveRecusarTokenExpirado() {
        // O único prazo é o MaxAge do cookie no navegador; o servidor não guarda nem confere a validade
        String tokenAntigo = loginGravandoToken(7, Instant.now().minus(31, ChronoUnit.MINUTES));

        assertFalse(validador.validar(cookies(new Cookie("token", tokenAntigo))),
            "Token de 31 minutos atrás continua aceito pelo servidor");
    }
}
