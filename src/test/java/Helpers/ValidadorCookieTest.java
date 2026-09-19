package Helpers;

import javax.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidadorCookieTest {
    @Test
    void deveRetornarIdDoClienteQuandoCookieTokenExistir() {
        Cookie[] cookies = {
            new Cookie("token", "123-abc-def")
        };

        ValidadorCookie validador = new ValidadorCookie();

        String resultado = validador.getCookieIdCliente(cookies);

        assertEquals("123", resultado);
    }

    @Test
    void deveRetornarErroQuandoCookieTokenNaoExistir() {
        Cookie[] cookies = {
            new Cookie("outroCookie", "123-abc-def")
        };

        ValidadorCookie validador = new ValidadorCookie();

        String resultado = validador.getCookieIdCliente(cookies);

        assertEquals("erro", resultado);
    }

    @Test
    void deveRetornarIdDoFuncionarioQuandoCookieExistir() {
        Cookie[] cookies = {
            new Cookie("tokenFuncionario", "456-xyz-def")
        };

        ValidadorCookie validador = new ValidadorCookie();

        String resultado = validador.getCookieIdFuncionario(cookies);

        assertEquals("456", resultado);
    }

    @Test
    void deveRetornarErroQuandoCookieFuncionarioNaoExistir() {
        Cookie[] cookies = {
            new Cookie("outroCookie", "456-xyz-def")
        };

        ValidadorCookie validador = new ValidadorCookie();

        String resultado = validador.getCookieIdFuncionario(cookies);

        assertEquals("erro", resultado);
    }
}
