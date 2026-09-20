package Helpers;

import DAO.DaoToken;
import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ValidadorCookieTest {
    @Test
    void deveRetornarIdDoClienteQuandoCookieTokenExistir() {
        Cookie[] cookies = {
            new Cookie("token", "123-abc-def")
        };

        DaoToken daoMock = mock(DaoToken.class);
        ValidadorCookie validador = new ValidadorCookie(daoMock);

        String resultado = validador.getCookieIdCliente(cookies);

        assertEquals("123", resultado);
    }

    @Test
    void deveRetornarErroQuandoCookieTokenNaoExistir() {
        Cookie[] cookies = {
            new Cookie("outroCookie", "123-abc-def")
        };

        DaoToken daoMock = mock(DaoToken.class);
        ValidadorCookie validador = new ValidadorCookie(daoMock);

        String resultado = validador.getCookieIdCliente(cookies);

        assertEquals("erro", resultado);
    }

    @Test
    void deveRetornarIdDoFuncionarioQuandoCookieExistir() {
        Cookie[] cookies = {
            new Cookie("tokenFuncionario", "456-xyz-def")
        };

        DaoToken daoMock = mock(DaoToken.class);
        ValidadorCookie validador = new ValidadorCookie(daoMock);

        String resultado = validador.getCookieIdFuncionario(cookies);

        assertEquals("456", resultado);
    }

    @Test
    void deveRetornarErroQuandoCookieFuncionarioNaoExistir() {
        Cookie[] cookies = {
            new Cookie("outroCookie", "456-xyz-def")
        };

        DaoToken daoMock = mock(DaoToken.class);
        ValidadorCookie validador = new ValidadorCookie(daoMock);

        String resultado = validador.getCookieIdFuncionario(cookies);

        assertEquals("erro", resultado);
    }

    @Test
void deveRetornarTrueQuandoTokenDoClienteForValido() {

    Cookie[] cookies = {
        new Cookie("token", "123-abc-def")
    };

    DaoToken daoMock = mock(DaoToken.class);

    when(daoMock.validar("123-abc-def"))
            .thenReturn(true);

    ValidadorCookie validador = new ValidadorCookie(daoMock);

    boolean resultado = validador.validar(cookies);

    assertTrue(resultado);

    verify(daoMock).validar("123-abc-def");
}

@Test
void deveRetornarFalseQuandoTokenDoClienteForInvalido() {

    Cookie[] cookies = {
        new Cookie("token", "123-abc-def")
    };

    DaoToken daoMock = mock(DaoToken.class);

    when(daoMock.validar("123-abc-def"))
            .thenReturn(false);

    ValidadorCookie validador = new ValidadorCookie(daoMock);

    boolean resultado = validador.validar(cookies);

    assertFalse(resultado);

    verify(daoMock).validar("123-abc-def");
}

@Test
void deveRetornarTrueQuandoTokenDoFuncionarioForValido() {

    Cookie[] cookies = {
        new Cookie("tokenFuncionario", "456-xyz-def")
    };

    DaoToken daoMock = mock(DaoToken.class);

    when(daoMock.validar("456-xyz-def"))
            .thenReturn(true);

    ValidadorCookie validador = new ValidadorCookie(daoMock);

    boolean resultado = validador.validarFuncionario(cookies);

    assertTrue(resultado);

    verify(daoMock).validar("456-xyz-def");
}

@Test
void deveRetornarFalseQuandoTokenDoFuncionarioForInvalido() {

    Cookie[] cookies = {
        new Cookie("tokenFuncionario", "456-xyz-def")
    };

    DaoToken daoMock = mock(DaoToken.class);

    when(daoMock.validar("456-xyz-def"))
            .thenReturn(false);

    ValidadorCookie validador = new ValidadorCookie(daoMock);

    boolean resultado = validador.validarFuncionario(cookies);

    assertFalse(resultado);

    verify(daoMock).validar("456-xyz-def");
}

@Test
void deveRemoverTokenDoCliente() {

    Cookie[] cookies = {
        new Cookie("token", "123-abc-def")
    };

    DaoToken daoMock = mock(DaoToken.class);

    ValidadorCookie validador = new ValidadorCookie(daoMock);

    validador.deletar(cookies);

    verify(daoMock).remover("123-abc-def");
}

@Test
void deveRemoverTokenDoFuncionario() {

    Cookie[] cookies = {
        new Cookie("tokenFuncionario", "456-xyz-def")
    };

    DaoToken daoMock = mock(DaoToken.class);

    ValidadorCookie validador = new ValidadorCookie(daoMock);

    validador.deletar(cookies);

    verify(daoMock).remover("456-xyz-def");
}

@Test
void naoDeveRemoverCookieQueNaoSejaToken() {

    Cookie[] cookies = {
        new Cookie("outroCookie", "qualquer-valor")
    };

    DaoToken daoMock = mock(DaoToken.class);

    ValidadorCookie validador = new ValidadorCookie(daoMock);

    validador.deletar(cookies);

    verify(daoMock, never()).remover(anyString());
}

}