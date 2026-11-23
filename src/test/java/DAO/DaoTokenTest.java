package DAO;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários de DaoToken com a conexão JDBC isolada por mock.
 * Todo @DisplayName começa pela técnica que originou o caso:
 *   Sucesso   - o banco responde normalmente (partições: token presente / ausente / repetido)
 *   Falha     - o JDBC lança exceção; cada método trata de um jeito diferente, o que os testes fixam
 * A ida ao PostgreSQL real fica em ValidadorCookieIT.
 */
@ExtendWith(MockitoExtension.class)
class DaoTokenTest {

    private static final String TOKEN = "7-2026-11-22T10:15:30.123Z";

    @Mock
    private Connection conexao;
    @Mock
    private PreparedStatement comando;
    @Mock
    private ResultSet resultado;

    private DaoToken dao;

    private final PrintStream consoleOriginal = System.out;
    private final ByteArrayOutputStream console = new ByteArrayOutputStream();

    @BeforeEach
    void preparar() {
        dao = new DaoToken(conexao);
        // Nos métodos que engolem SQLException, o println no console é o único registro da falha
        System.setOut(new PrintStream(console, true));
    }

    @AfterEach
    void restaurarConsole() {
        System.setOut(consoleOriginal);
    }

    private String console() {
        return console.toString().trim();
    }

    @Nested
    @DisplayName("salvar")
    class Salvar {

        @Test
        @DisplayName("Sucesso: insere o token como parâmetro, executa e fecha o comando, nessa ordem")
        void deveInserirTokenEFecharComando() throws SQLException {
            when(conexao.prepareStatement("INSERT INTO tb_tokens(token) VALUES(?)")).thenReturn(comando);

            dao.salvar(TOKEN);

            InOrder ordem = inOrder(comando);
            ordem.verify(comando).setString(1, TOKEN);
            ordem.verify(comando).execute();
            ordem.verify(comando).close();
        }

        @Test
        @DisplayName("Falha: erro do banco (ex.: token repetido viola o UNIQUE) é repassado como RuntimeException")
        void deveRepassarFalhaDoBancoAoSalvar() throws SQLException {
            SQLException violacaoUnique = new SQLException("duplicate key value violates unique constraint");
            when(conexao.prepareStatement("INSERT INTO tb_tokens(token) VALUES(?)")).thenReturn(comando);
            when(comando.execute()).thenThrow(violacaoUnique);

            RuntimeException erro = assertThrows(RuntimeException.class, () -> dao.salvar(TOKEN));

            assertSame(violacaoUnique, erro.getCause());
        }
    }

    @Nested
    @DisplayName("validar")
    class Validar {

        private void bancoResponde(Boolean primeiraLinha, Boolean... proximas) throws SQLException {
            when(conexao.prepareStatement("SELECT token FROM tb_tokens WHERE token = ?")).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(primeiraLinha, proximas);
        }

        @Test
        @DisplayName("Sucesso: token presente na tabela é válido; consulta usa o token como parâmetro e fecha os recursos")
        void deveAceitarTokenPresente() throws SQLException {
            bancoResponde(true, false);

            assertTrue(dao.validar(TOKEN));

            verify(comando).setString(1, TOKEN);
            verify(resultado).close();
            verify(comando).close();
        }

        @Test
        @DisplayName("Sucesso: token ausente da tabela é inválido e os recursos também são fechados")
        void deveRecusarTokenAusente() throws SQLException {
            bancoResponde(false);

            assertFalse(dao.validar(TOKEN));

            verify(resultado).close();
            verify(comando).close();
        }

        @Test
        @DisplayName("Sucesso: mais de uma linha (não ocorre com o UNIQUE, mas o laço percorre todas) continua válido")
        void deveAceitarQuandoHaMaisDeUmaLinha() throws SQLException {
            bancoResponde(true, true, false);

            assertTrue(dao.validar(TOKEN));

            verify(resultado, times(3)).next();
        }

        @Test
        @DisplayName("Falha: SQLException na consulta é engolida, só aparece no console, e o token é tratado como inválido")
        void deveRecusarTokenQuandoConsultaFalha() throws SQLException {
            when(conexao.prepareStatement("SELECT token FROM tb_tokens WHERE token = ?")).thenReturn(comando);
            when(comando.executeQuery()).thenThrow(new SQLException("conexão perdida"));

            assertFalse(dao.validar(TOKEN));
            assertEquals("java.sql.SQLException: conexão perdida", console());

            // O close() fica depois da consulta e fora de finally: com a falha, o comando não é fechado
            verify(comando, never()).close();
        }

        @Test
        @DisplayName("Falha: erro de execução (não SQLException) não é tratado e chega a quem chamou")
        void deveRepassarErroQueNaoSejaSqlException() throws SQLException {
            when(conexao.prepareStatement("SELECT token FROM tb_tokens WHERE token = ?")).thenThrow(new IllegalStateException("pool esgotado"));

            assertThrows(IllegalStateException.class, () -> dao.validar(TOKEN));
        }
    }

    @Nested
    @DisplayName("remover")
    class Remover {

        @Test
        @DisplayName("Sucesso: apaga pelo token informado, executa e fecha o comando, nessa ordem")
        void deveApagarTokenEFecharComando() throws SQLException {
            when(conexao.prepareStatement("DELETE FROM tb_tokens WHERE token = ?")).thenReturn(comando);

            dao.remover(TOKEN);

            InOrder ordem = inOrder(comando);
            ordem.verify(comando).setString(1, TOKEN);
            ordem.verify(comando).execute();
            ordem.verify(comando).close();
        }

        @Test
        @DisplayName("Falha: SQLException é engolida (só aparece no console); o logout segue como se o token tivesse sido apagado")
        void deveEngolirFalhaAoRemover() throws SQLException {
            when(conexao.prepareStatement("DELETE FROM tb_tokens WHERE token = ?")).thenReturn(comando);
            when(comando.execute()).thenThrow(new SQLException("conexão perdida"));

            // ValidadorCookie.deletar só repassa RuntimeException, então essa falha nunca chega ao usuário
            assertDoesNotThrow(() -> dao.remover(TOKEN));
            assertEquals("java.sql.SQLException: conexão perdida", console());
            verify(comando, never()).close();
        }
    }

    @Nested
    @DisplayName("LimparTabela")
    class LimparTabela {

        @Test
        @DisplayName("Sucesso: apaga todos os tokens e fecha o comando")
        void deveApagarTodosOsTokens() throws SQLException {
            when(conexao.prepareStatement("DELETE FROM tb_tokens")).thenReturn(comando);

            dao.LimparTabela();

            InOrder ordem = inOrder(comando);
            ordem.verify(comando).execute();
            ordem.verify(comando).close();
        }

        @Test
        @DisplayName("Falha: SQLException é engolida sem avisar quem chamou; só aparece no console")
        void deveEngolirFalhaAoLimpar() throws SQLException {
            when(conexao.prepareStatement("DELETE FROM tb_tokens")).thenThrow(new SQLException("conexão perdida"));

            assertDoesNotThrow(() -> dao.LimparTabela());
            assertEquals("java.sql.SQLException: conexão perdida", console());
        }
    }
}
