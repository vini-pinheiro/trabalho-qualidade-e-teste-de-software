package Helpers;

import DAO.DaoToken;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ValidadorCookieTest {

    private static final String TOKEN_CLIENTE = "7-2026-11-22T10:15:30.123Z";
    private static final String TOKEN_FUNCIONARIO = "1-2026-11-22T11:00:00.456Z";

    private DaoToken tokenDao;
    private ValidadorCookie validador;

    @BeforeEach
    void preparar() {
        tokenDao = mock(DaoToken.class);
        validador = new ValidadorCookie(tokenDao);
    }

    private static Cookie[] cookies(Cookie... cookies) {
        return cookies;
    }

    private static Cookie sessao() {
        return new Cookie("JSESSIONID", "A1B2C3D4E5");
    }

    @Nested
    @DisplayName("validar (cliente)")
    class Validar {

        @Test
        @DisplayName("P1: token aceito pelo banco libera o acesso")
        void deveRetornarTrueQuandoTokenDoClienteForValido() {
            when(tokenDao.validar(TOKEN_CLIENTE)).thenReturn(true);

            assertTrue(validador.validar(cookies(new Cookie("token", TOKEN_CLIENTE))));

            verify(tokenDao).validar(TOKEN_CLIENTE);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P1: token recusado pelo banco nega o acesso")
        void deveRetornarFalseQuandoTokenDoClienteForInvalido() {
            when(tokenDao.validar(TOKEN_CLIENTE)).thenReturn(false);

            assertFalse(validador.validar(cookies(new Cookie("token", TOKEN_CLIENTE))));

            verify(tokenDao).validar(TOKEN_CLIENTE);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P2: encontra o token entre outros cookies e consulta só o valor dele")
        void deveConsultarApenasOTokenQuandoHaOutrosCookies() {
            when(tokenDao.validar(TOKEN_CLIENTE)).thenReturn(true);

            assertTrue(validador.validar(cookies(sessao(), new Cookie("token", TOKEN_CLIENTE), new Cookie("tema", "escuro"))));

            verify(tokenDao).validar(TOKEN_CLIENTE);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P2: com os dois perfis logados, valida só o token de cliente")
        void deveIgnorarTokenDeFuncionarioQuandoOsDoisEstaoPresentes() {
            when(tokenDao.validar(TOKEN_CLIENTE)).thenReturn(false);
            when(tokenDao.validar(TOKEN_FUNCIONARIO)).thenReturn(true);

            assertFalse(validador.validar(cookies(new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO), new Cookie("token", TOKEN_CLIENTE))));

            verify(tokenDao, never()).validar(TOKEN_FUNCIONARIO);
        }

        @Test
        @DisplayName("P4: só cookies alheios nega o acesso sem consultar o banco")
        void deveNegarAcessoSemConsultarBancoQuandoNaoHaToken() {
            assertFalse(validador.validar(cookies(sessao(), new Cookie("tema", "escuro"))));

            verifyNoInteractions(tokenDao);
        }
    }

    @Nested
    @DisplayName("validarFuncionario")
    class ValidarFuncionario {

        @Test
        @DisplayName("P1: token aceito pelo banco libera o acesso")
        void deveRetornarTrueQuandoTokenDoFuncionarioForValido() {
            when(tokenDao.validar(TOKEN_FUNCIONARIO)).thenReturn(true);

            assertTrue(validador.validarFuncionario(cookies(new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO))));

            verify(tokenDao).validar(TOKEN_FUNCIONARIO);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P1: token recusado pelo banco nega o acesso")
        void deveRetornarFalseQuandoTokenDoFuncionarioForInvalido() {
            when(tokenDao.validar(TOKEN_FUNCIONARIO)).thenReturn(false);

            assertFalse(validador.validarFuncionario(cookies(new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO))));

            verify(tokenDao).validar(TOKEN_FUNCIONARIO);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P2: encontra o token entre outros cookies e consulta só o valor dele")
        void deveConsultarApenasOTokenQuandoHaOutrosCookies() {
            when(tokenDao.validar(TOKEN_FUNCIONARIO)).thenReturn(true);

            assertTrue(validador.validarFuncionario(cookies(sessao(), new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO), new Cookie("tema", "escuro"))));

            verify(tokenDao).validar(TOKEN_FUNCIONARIO);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P2: com os dois perfis logados, valida só o token de funcionário")
        void deveIgnorarTokenDeClienteQuandoOsDoisEstaoPresentes() {
            when(tokenDao.validar(TOKEN_CLIENTE)).thenReturn(true);
            when(tokenDao.validar(TOKEN_FUNCIONARIO)).thenReturn(false);

            assertFalse(validador.validarFuncionario(cookies(new Cookie("token", TOKEN_CLIENTE), new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO))));

            verify(tokenDao, never()).validar(TOKEN_CLIENTE);
        }

        @Test
        @DisplayName("P4: só cookies alheios nega o acesso sem consultar o banco")
        void deveNegarAcessoSemConsultarBancoQuandoNaoHaToken() {
            assertFalse(validador.validarFuncionario(cookies(sessao(), new Cookie("tema", "escuro"))));

            verifyNoInteractions(tokenDao);
        }
    }

    @Nested
    @DisplayName("deletar (logout)")
    class Deletar {

        @Test
        @DisplayName("P1: remove o token de cliente")
        void deveRemoverTokenDoCliente() {
            validador.deletar(cookies(new Cookie("token", TOKEN_CLIENTE)));

            verify(tokenDao).remover(TOKEN_CLIENTE);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P1: remove o token de funcionário")
        void deveRemoverTokenDoFuncionario() {
            validador.deletar(cookies(new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO)));

            verify(tokenDao).remover(TOKEN_FUNCIONARIO);
            verifyNoMoreInteractions(tokenDao);
        }

        @Test
        @DisplayName("P4: cookies alheios não são removidos do banco")
        void naoDeveRemoverCookieQueNaoSejaToken() {
            validador.deletar(cookies(sessao(), new Cookie("outroCookie", "qualquer-valor")));

            verifyNoInteractions(tokenDao);
        }

        @Test
        @DisplayName("P5: sem cookies o logout não toca no banco")
        void naoDeveAcessarBancoQuandoNaoHaCookies() {
            validador.deletar(cookies());

            verifyNoInteractions(tokenDao);
        }

        @Test
        @DisplayName("Falha ao remover o primeiro token interrompe o logout: o segundo continua no banco")
        void deveInterromperLogoutQuandoRemocaoDoPrimeiroTokenFalha() {
            IllegalStateException falhaBanco = new IllegalStateException("conexão perdida");
            doThrow(falhaBanco).when(tokenDao).remover(TOKEN_CLIENTE);

            RuntimeException erro = assertThrows(RuntimeException.class,
                () -> validador.deletar(cookies(new Cookie("token", TOKEN_CLIENTE), new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO))));

            assertSame(falhaBanco, erro.getCause());
            verify(tokenDao, never()).remover(TOKEN_FUNCIONARIO);
        }
    }

    @Nested
    @DisplayName("getCookieIdCliente")
    class GetCookieIdCliente {

        @Test
        @DisplayName("P1: devolve o ID antes do primeiro hífen, sem consultar o banco")
        void deveRetornarIdDoClienteQuandoCookieTokenExistir() {
            assertEquals("7", validador.getCookieIdCliente(cookies(new Cookie("token", TOKEN_CLIENTE))));

            verifyNoInteractions(tokenDao);
        }

        @Test
        @DisplayName("P2: encontra o token depois de cookies alheios")
        void deveRetornarIdQuandoTokenNaoEOPrimeiroCookie() {
            assertEquals("7", validador.getCookieIdCliente(cookies(sessao(), new Cookie("token", TOKEN_CLIENTE))));
        }

        @Test
        @DisplayName("P3: token de funcionário não identifica cliente")
        void deveRetornarErroQuandoSoHaTokenDeFuncionario() {
            assertEquals("erro", validador.getCookieIdCliente(cookies(new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO))));
        }

        @Test
        @DisplayName("P4: só cookies alheios devolve \"erro\"")
        void deveRetornarErroQuandoCookieTokenNaoExistir() {
            assertEquals("erro", validador.getCookieIdCliente(cookies(new Cookie("outroCookie", "123-abc-def"))));
        }

        @Test
        @DisplayName("P5: sem cookies devolve \"erro\"")
        void deveRetornarErroQuandoNaoHaCookies() {
            assertEquals("erro", validador.getCookieIdCliente(cookies()));
        }

        @Test
        @DisplayName("Extrai o ID mesmo de token que o banco recusaria: quem chama precisa validar antes")
        void deveExtrairIdSemValidarOToken() {
            assertEquals("999", validador.getCookieIdCliente(cookies(new Cookie("token", "999-forjado"))));

            verifyNoInteractions(tokenDao);
        }
    }

    @Nested
    @DisplayName("getCookieIdFuncionario")
    class GetCookieIdFuncionario {

        @Test
        @DisplayName("P1: devolve o ID antes do primeiro hífen, sem consultar o banco")
        void deveRetornarIdDoFuncionarioQuandoCookieExistir() {
            assertEquals("1", validador.getCookieIdFuncionario(cookies(new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO))));

            verifyNoInteractions(tokenDao);
        }

        @Test
        @DisplayName("P2: encontra o token depois de cookies alheios")
        void deveRetornarIdQuandoTokenNaoEOPrimeiroCookie() {
            assertEquals("1", validador.getCookieIdFuncionario(cookies(sessao(), new Cookie("tokenFuncionario", TOKEN_FUNCIONARIO))));
        }

        @Test
        @DisplayName("P3: token de cliente não identifica funcionário")
        void deveRetornarErroQuandoSoHaTokenDeCliente() {
            assertEquals("erro", validador.getCookieIdFuncionario(cookies(new Cookie("token", TOKEN_CLIENTE))));
        }

        @Test
        @DisplayName("P4: só cookies alheios devolve \"erro\"")
        void deveRetornarErroQuandoCookieFuncionarioNaoExistir() {
            assertEquals("erro", validador.getCookieIdFuncionario(cookies(new Cookie("outroCookie", "456-xyz-def"))));
        }

        @Test
        @DisplayName("P5: sem cookies devolve \"erro\"")
        void deveRetornarErroQuandoNaoHaCookies() {
            assertEquals("erro", validador.getCookieIdFuncionario(cookies()));
        }

        @Test
        @DisplayName("Com dois cookies tokenFuncionario, vale o primeiro")
        void deveExtrairIdDoPrimeiroCookieQuandoTokenVemDuplicado() {
            Cookie[] duplicados = cookies(new Cookie("tokenFuncionario", "1-abc"), new Cookie("tokenFuncionario", "2-def"));

            assertEquals("1", validador.getCookieIdFuncionario(duplicados));
        }

        @Test
        @DisplayName("Navegador sem cookies entrega null: lança NullPointerException")
        void deveLancarNullPointerQuandoCookiesForNulo() {
            assertThrows(NullPointerException.class, () -> validador.getCookieIdFuncionario(null));
        }
    }
}
