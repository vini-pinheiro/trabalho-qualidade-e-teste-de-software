package Controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import DAO.DaoBebida;
import DAO.DaoCliente;
import DAO.DaoLanche;
import DAO.DaoPedido;
import Helpers.ValidadorCookie;
import Model.Bebida;
import Model.Cliente;
import Model.Lanche;
import Model.Pedido;

@ExtendWith(MockitoExtension.class)
public class ComprarTest {
    @Mock
    private ValidadorCookie validadorCookie;
    @Mock
    private DaoCliente clienteDao;
    @Mock
    private DaoLanche lancheDao;
    @Mock
    private DaoBebida bebidaDao;
    @Mock
    private DaoPedido pedidoDao;

    private comprar controller;

    @BeforeEach
    void setUp() {
        controller = new comprar(validadorCookie, clienteDao, lancheDao, bebidaDao, pedidoDao);
    }

    private ServletInputStream criarServletInputStream(String json) {
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        java.io.ByteArrayInputStream byteArrayInputStream = new java.io.ByteArrayInputStream(bytes);

        return new ServletInputStream() {
            @Override
            public int read() throws java.io.IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(javax.servlet.ReadListener readListener) {
            }
        };
    }

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("Deve retornar erro quando o cookie for inválido")
    void testeCookieInvalido() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        ServletInputStream inputStream = org.mockito.Mockito.mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inputStream);

        when(validadorCookie.validar(any())).thenReturn(false);

        controller.processRequest(request, response);

        assertEquals("erro", stringWriter.toString().trim());
        verify(pedidoDao, never()).salvar(any());
        verify(clienteDao, never()).pesquisaPorID(any());
    }

    @Test
    @DisplayName("Deve realizar a compra com sucesso")
    public void testeComprarComSucesso() throws IOException, ServletException {
        String json = "{\"id\": 1, \"X-Burger\": [1, \"lanche\", 2], \"Refrigerante\": [2, \"bebida\", 1]}";
        // Cookie Válido
        when(validadorCookie.validar(any())).thenReturn(true);

        // Entrada e Saída HTTP
        when(request.getInputStream()).thenReturn(criarServletInputStream(json));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Cliente
        Cliente cliente = new Cliente();
        cliente.setId_cliente(1);
        when(clienteDao.pesquisaPorID("1")).thenReturn(cliente);

        // Lanche
        Lanche lanche = new Lanche();
        lanche.setNome("X-Burger");
        lanche.setValor_venda(15.0);
        when(lancheDao.pesquisaPorNome("X-Burger")).thenReturn(lanche);

        // Bebida
        Bebida bebida = new Bebida();
        bebida.setNome("Refrigerante");
        bebida.setValor_venda(5.0);
        when(bebidaDao.pesquisaPorNome("Refrigerante")).thenReturn(bebida);

        // Pedido retornado da busca
        Pedido pedidoBanco = new Pedido();
        pedidoBanco.setId_pedido(10);
        when(pedidoDao.pesquisaPorData(any())).thenReturn(pedidoBanco);

        controller.processRequest(request, response);

        // A mensagem de sucesso na tela
        assertEquals("Pedido Salvo com Sucesso!", stringWriter.toString().trim());

        // Garante que buscou o cliente pelo ID correto
        verify(clienteDao, times(1)).pesquisaPorID("1");

        // Garante que buscou os itens
        verify(lancheDao, times(1)).pesquisaPorNome("X-Burger");
        verify(bebidaDao, times(1)).pesquisaPorNome("Refrigerante");

        // Garante que os vínculos com o pedido foram executados
        verify(pedidoDao, times(1)).vincularLanche(eq(pedidoBanco), eq(lanche));
        verify(pedidoDao, times(1)).vincularBebida(eq(pedidoBanco), eq(bebida));
        verify(pedidoDao, times(1)).salvar(any(Pedido.class));
    }

    @Test
    @DisplayName("Deve retornar erro quando ocorrer NullPointerException ao validar cookies")
    void testeCookieComNullPointerException() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        ServletInputStream inputStream = org.mockito.Mockito.mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inputStream);

        // Ensinamos o mock a estourar a exceção esperada
        when(validadorCookie.validar(any())).thenThrow(new NullPointerException());

        controller.processRequest(request, response);

        assertEquals("erro", stringWriter.toString().trim());
        verify(pedidoDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve processar pedido contendo apenas lanches")
    public void testeComprarApenasLanches() throws ServletException, IOException {
        String json = "{\"id\": 1, \"X-Burger\": [1, \"lanche\", 2]}";
        // Cookie Válido
        when(validadorCookie.validar(any())).thenReturn(true);

        // Entrada e Saída HTTP
        when(request.getInputStream()).thenReturn(criarServletInputStream(json));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Cliente
        Cliente cliente = new Cliente();
        cliente.setId_cliente(1);
        when(clienteDao.pesquisaPorID("1")).thenReturn(cliente);

        // Lanche
        Lanche lanche = new Lanche();
        lanche.setNome("X-Burger");
        lanche.setValor_venda(15.0);
        when(lancheDao.pesquisaPorNome("X-Burger")).thenReturn(lanche);

        // Pedido retornado da busca
        Pedido pedidoBanco = new Pedido();
        pedidoBanco.setId_pedido(10);
        when(pedidoDao.pesquisaPorData(any())).thenReturn(pedidoBanco);

        controller.processRequest(request, response);

        // A mensagem de sucesso na tela
        assertEquals("Pedido Salvo com Sucesso!", stringWriter.toString().trim());

        // Garante que buscou o cliente pelo ID correto
        verify(clienteDao, times(1)).pesquisaPorID("1");

        // Garante que buscou os itens
        verify(lancheDao, times(1)).pesquisaPorNome("X-Burger");

        // Garante que os vínculos ou suas ausências com o pedido foram executados
        verify(pedidoDao, times(1)).vincularLanche(eq(pedidoBanco), eq(lanche));
        verify(pedidoDao, times(1)).salvar(any(Pedido.class));
        verify(bebidaDao, never()).pesquisaPorNome(any());
        verify(pedidoDao, never()).vincularBebida(any(), any());
    }

    @Test
    @DisplayName("Deve processar pedido contendo apenas bebidas")
    public void testeComprarApenasBebidas() throws ServletException, IOException {
        String json = "{\"id\": 1, \"Refrigerante\": [2, \"bebida\", 1]}";
        // Cookie Válido
        when(validadorCookie.validar(any())).thenReturn(true);

        // Entrada e Saída HTTP
        when(request.getInputStream()).thenReturn(criarServletInputStream(json));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Cliente
        Cliente cliente = new Cliente();
        cliente.setId_cliente(1);
        when(clienteDao.pesquisaPorID("1")).thenReturn(cliente);

        // Bebida
        Bebida bebida = new Bebida();
        bebida.setNome("Refrigerante");
        bebida.setValor_venda(8.0);
        when(bebidaDao.pesquisaPorNome("Refrigerante")).thenReturn(bebida);

        // Pedido retornado da busca
        Pedido pedidoBanco = new Pedido();
        pedidoBanco.setId_pedido(10);
        when(pedidoDao.pesquisaPorData(any())).thenReturn(pedidoBanco);

        controller.processRequest(request, response);

        // A mensagem de sucesso na tela
        assertEquals("Pedido Salvo com Sucesso!", stringWriter.toString().trim());

        // Garante que buscou o cliente pelo ID correto
        verify(clienteDao, times(1)).pesquisaPorID("1");

        // Garante que buscou os itens
        verify(bebidaDao, times(1)).pesquisaPorNome("Refrigerante");

        // Garante que os vínculos ou suas ausências com o pedido foram executados
        verify(pedidoDao, times(1)).vincularBebida(pedidoBanco, bebida);
        verify(pedidoDao, times(1)).salvar(any(Pedido.class));
        verify(lancheDao, never()).pesquisaPorNome(anyString());
        verify(pedidoDao, never()).vincularLanche(any(), any());
    }

    @Test
    @DisplayName("Deve delegar requisições GET para o processRequest")
    void testeDoGet() throws Exception {
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
        ServletInputStream inputStream = org.mockito.Mockito.mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inputStream);
        when(validadorCookie.validar(any())).thenReturn(false);

        controller.doGet(request, response);

        assertEquals("erro", stringWriter.toString().trim());
    }

    @Test
    @DisplayName("Deve delegar requisições POST para o processRequest")
    void testeDoPost() throws Exception {
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
        ServletInputStream inputStream = org.mockito.Mockito.mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inputStream);
        when(validadorCookie.validar(any())).thenReturn(false);

        controller.doPost(request, response);

        assertEquals("erro", stringWriter.toString().trim());
    }

    @Test
    @DisplayName("Deve retornar a descrição do servlet")
    void testeGetServletInfo() {
        assertEquals("Short description", controller.getServletInfo());
    }
}