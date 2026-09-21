package Helpers;

import DAO.DaoToken;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Complementa ValidadorCookieTest com entradas de fronteira: cookies ausentes,
 * nomes trocados, duplicidade, valores malformados e falha do DAO.
 */
class ValidadorCookieLimitesTest {

    private DaoToken tokenDao;
    private ValidadorCookie validador;

    @BeforeEach
    void preparar() {
        tokenDao = mock(DaoToken.class);
        validador = new ValidadorCookie(tokenDao);
    }

    @Test
    @DisplayName("Sem nenhum cookie o acesso é negado e o banco nem é consultado")
    void deveNegarAcessoQuandoNaoHaCookies() {
        Cookie[] semCookies = {};

        assertFalse(validador.validar(semCookies));
        assertFalse(validador.validarFuncionario(semCookies));
        verifyNoInteractions(tokenDao);
    }

    @Test
    @DisplayName("Navegador sem cookies entrega null: o validador lança NullPointerException")
    void deveLancarNullPointerQuandoCookiesForNulo() {
        // Os controllers dependem desse comportamento: eles capturam a NPE e respondem "erro"
        assertThrows(NullPointerException.class, () -> validador.validar(null));
        assertThrows(NullPointerException.class, () -> validador.validarFuncionario(null));
        assertThrows(NullPointerException.class, () -> validador.deletar(null));
        assertThrows(NullPointerException.class, () -> validador.getCookieIdCliente(null));
    }

    @Test
    @DisplayName("Token de cliente não vale como token de funcionário")
    void naoDeveAceitarTokenDeClienteComoFuncionario() {
        Cookie[] cookies = { new Cookie("token", "7-2026-09-21T10:00:00Z") };

        assertFalse(validador.validarFuncionario(cookies));
        verify(tokenDao, never()).validar(anyString());
    }

    @Test
    @DisplayName("Token de funcionário não vale como token de cliente")
    void naoDeveAceitarTokenDeFuncionarioComoCliente() {
        Cookie[] cookies = { new Cookie("tokenFuncionario", "1-2026-09-21T10:00:00Z") };

        assertFalse(validador.validar(cookies));
        verify(tokenDao, never()).validar(anyString());
    }

    @Test
    @DisplayName("O nome do cookie diferencia maiúsculas: 'Token' é ignorado")
    void deveIgnorarCookieComNomeEmOutraCaixa() {
        Cookie[] cookies = { new Cookie("Token", "7-abc") };

        assertFalse(validador.validar(cookies));
        verify(tokenDao, never()).validar(anyString());
    }

    @Test
    @DisplayName("Com dois cookies 'token', vale o resultado do último")
    void deveConsiderarUltimoCookieQuandoTokenVemDuplicado() {
        Cookie[] validoDepoisInvalido = { new Cookie("token", "7-valido"), new Cookie("token", "7-invalido") };
        Cookie[] invalidoDepoisValido = { new Cookie("token", "7-invalido"), new Cookie("token", "7-valido") };
        when(tokenDao.validar("7-valido")).thenReturn(true);
        when(tokenDao.validar("7-invalido")).thenReturn(false);

        assertFalse(validador.validar(validoDepoisInvalido));
        assertTrue(validador.validar(invalidoDepoisValido));
    }

    @Test
    @DisplayName("Na extração do ID com cookie duplicado, vale o primeiro")
    void deveExtrairIdDoPrimeiroCookieQuandoTokenVemDuplicado() {
        Cookie[] cookies = { new Cookie("token", "7-abc"), new Cookie("token", "9-def") };

        // Diferente de validar(), que fica com o último: as duas leituras podem divergir
        assertEquals("7", validador.getCookieIdCliente(cookies));
    }

    @ParameterizedTest(name = "valor \"{0}\" -> id \"{1}\"")
    @CsvSource({
        "15-2026-09-21T10:15:30.123Z, 15",
        "15,                          15",
        "abc-2026,                    abc",
        "'-2026',                     ''",
        "'',                          ''"
    })
    @DisplayName("Extração do ID usa o trecho antes do primeiro hífen, sem validar se é número")
    void deveExtrairTrechoAntesDoPrimeiroHifen(String valorCookie, String idEsperado) {
        Cookie[] cliente = { new Cookie("token", valorCookie) };
        Cookie[] funcionario = { new Cookie("tokenFuncionario", valorCookie) };

        assertEquals(idEsperado, validador.getCookieIdCliente(cliente));
        assertEquals(idEsperado, validador.getCookieIdFuncionario(funcionario));
    }

    @Test
    @DisplayName("Logout remove os tokens de cliente e de funcionário e ignora os demais cookies")
    void deveRemoverOsDoisTokensNoLogout() {
        Cookie[] cookies = {
            new Cookie("JSESSIONID", "qualquer"),
            new Cookie("token", "7-abc"),
            new Cookie("tokenFuncionario", "1-def")
        };

        validador.deletar(cookies);

        verify(tokenDao).remover("7-abc");
        verify(tokenDao).remover("1-def");
        verify(tokenDao, never()).remover("qualquer");
    }

    @Test
    @DisplayName("Falha do DAO ao remover o token é repassada como RuntimeException")
    void deveRepassarFalhaDoDaoAoRemoverToken() {
        Cookie[] cookies = { new Cookie("token", "7-abc") };
        IllegalStateException falhaBanco = new IllegalStateException("conexão perdida");
        doThrow(falhaBanco).when(tokenDao).remover("7-abc");

        RuntimeException erro = assertThrows(RuntimeException.class, () -> validador.deletar(cookies));

        assertSame(falhaBanco, erro.getCause());
    }

    @Test
    @DisplayName("Falha do DAO ao validar o token não é tratada pelo validador")
    void deveRepassarFalhaDoDaoAoValidarToken() {
        Cookie[] cookies = { new Cookie("token", "7-abc") };
        when(tokenDao.validar("7-abc")).thenThrow(new IllegalStateException("conexão perdida"));

        assertThrows(IllegalStateException.class, () -> validador.validar(cookies));
    }
}
