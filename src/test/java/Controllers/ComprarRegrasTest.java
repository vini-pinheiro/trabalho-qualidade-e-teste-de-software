package Controllers;

import DAO.DaoBebida;
import DAO.DaoCliente;
import DAO.DaoLanche;
import DAO.DaoPedido;
import Helpers.ValidadorCookie;
import Model.Bebida;
import Model.Cliente;
import Model.Lanche;
import Model.Pedido;
import apoio.CorpoRequisicao;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Complementa ComprarTest conferindo o Pedido que chega ao DAO (total, cliente,
 * quantidades e vínculos), e não só a mensagem de sucesso.
 *
 * Contrato de entrada (web/view/carrinho/carrinho.js):
 * {"id": idCliente, "Nome do item": [precoUnitario, "lanche"|"bebida", quantidade]}
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComprarRegrasTest {

    private static final double PRECO_X_BURGER = 15.00;
    private static final double PRECO_REFRIGERANTE = 5.00;

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
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private comprar controller;
    private StringWriter resposta;
    private Cliente clienteLogado;
    private Lanche xBurger;
    private Bebida refrigerante;
    private Pedido pedidoGravado;

    @BeforeEach
    void preparar() throws Exception {
        controller = new comprar(validadorCookie, clienteDao, lancheDao, bebidaDao, pedidoDao);

        resposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(resposta));
        when(validadorCookie.validar(any())).thenReturn(true);

        clienteLogado = new Cliente();
        clienteLogado.setId_cliente(1);
        when(clienteDao.pesquisaPorID("1")).thenReturn(clienteLogado);

        xBurger = new Lanche();
        xBurger.setId_lanche(3);
        xBurger.setNome("X-Burger");
        xBurger.setValor_venda(PRECO_X_BURGER);
        when(lancheDao.pesquisaPorNome("X-Burger")).thenReturn(xBurger);

        refrigerante = new Bebida();
        refrigerante.setId_bebida(4);
        refrigerante.setNome("Refrigerante");
        refrigerante.setValor_venda(PRECO_REFRIGERANTE);
        when(bebidaDao.pesquisaPorNome("Refrigerante")).thenReturn(refrigerante);

        pedidoGravado = new Pedido();
        pedidoGravado.setId_pedido(10);
        when(pedidoDao.pesquisaPorData(any())).thenReturn(pedidoGravado);
    }

    private void enviar(String json) throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(json));
        controller.processRequest(request, response);
    }

    private Pedido pedidoEnviadoAoDao() {
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoDao).salvar(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Carrinho misto com uma unidade de cada: total é a soma dos preços")
    void deveSomarLancheEBebidaComUmaUnidadeDeCada() throws Exception {
        enviar("{\"id\": 1, \"X-Burger\": [\"15.00\", \"lanche\", \"1\"], \"Refrigerante\": [\"5.00\", \"bebida\", \"1\"]}");

        Pedido pedido = pedidoEnviadoAoDao();
        assertEquals(20.00, pedido.getValor_total(), 0.001);
        assertSame(clienteLogado, pedido.getCliente());
        assertEquals("Pedido Salvo com Sucesso!", resposta.toString().trim());
    }

    @Test
    @DisplayName("As quantidades do carrinho chegam aos vínculos do pedido")
    void deveRepassarQuantidadesParaOsVinculos() throws Exception {
        enviar("{\"id\": 1, \"X-Burger\": [\"15.00\", \"lanche\", 3], \"Refrigerante\": [\"5.00\", \"bebida\", 2]}");

        verify(pedidoDao).vincularLanche(pedidoGravado, xBurger);
        verify(pedidoDao).vincularBebida(pedidoGravado, refrigerante);
        assertEquals(3, xBurger.getQuantidade());
        assertEquals(2, refrigerante.getQuantidade());
        assertSame(clienteLogado, pedidoGravado.getCliente());
    }

    @Test
    @DisplayName("O preço vem do banco, não do valor enviado pelo navegador")
    void deveIgnorarPrecoInformadoPeloNavegador() throws Exception {
        enviar("{\"id\": 1, \"X-Burger\": [\"0.01\", \"lanche\", \"1\"]}");

        assertEquals(PRECO_X_BURGER, pedidoEnviadoAoDao().getValor_total(), 0.001);
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("DEF-02: total deve considerar a quantidade (2 x 15,00 + 1 x 5,00 = 35,00)")
    void deveMultiplicarPrecoPelaQuantidade() throws Exception {
        // Oráculo: a tela do carrinho soma preço x quantidade (plusItem em carrinho.js)
        double totalExibidoNoCarrinho = 2 * 15.00 + 1 * 5.00;

        enviar("{\"id\": 1, \"X-Burger\": [\"15.00\", \"lanche\", 2], \"Refrigerante\": [\"5.00\", \"bebida\", 1]}");

        assertEquals(totalExibidoNoCarrinho, pedidoEnviadoAoDao().getValor_total(), 0.001);
    }

    @Test
    @DisplayName("Caracterização do DEF-02: hoje o total ignora a quantidade")
    void caracterizacaoTotalIgnoraQuantidade() throws Exception {
        enviar("{\"id\": 1, \"X-Burger\": [\"15.00\", \"lanche\", 2], \"Refrigerante\": [\"5.00\", \"bebida\", 1]}");

        // Comportamento atual, não o desejado. Remover quando o DEF-02 for corrigido.
        assertEquals(20.00, pedidoEnviadoAoDao().getValor_total(), 0.001);
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("DEF-03: pedido não pode ser gravado para um cliente diferente do autenticado")
    void naoDeveGravarPedidoParaOutroCliente() throws Exception {
        Cliente outroCliente = new Cliente();
        outroCliente.setId_cliente(2);
        when(clienteDao.pesquisaPorID("2")).thenReturn(outroCliente);
        when(validadorCookie.getCookieIdCliente(any())).thenReturn("1");

        enviar("{\"id\": 2, \"X-Burger\": [\"15.00\", \"lanche\", \"1\"]}");

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoDao, org.mockito.Mockito.atMost(1)).salvar(captor.capture());
        for (Pedido pedido : captor.getAllValues()) {
            assertNotEquals(2, pedido.getCliente().getId_cliente(),
                    "o cookie autenticado é do cliente 1, mas o pedido foi gravado para o cliente 2");
        }
    }

    @Test
    @DisplayName("Caracterização do DEF-03: o cliente do pedido vem do JSON e o cookie não é conferido")
    void caracterizacaoClienteVemDoJson() throws Exception {
        Cliente outroCliente = new Cliente();
        outroCliente.setId_cliente(2);
        when(clienteDao.pesquisaPorID("2")).thenReturn(outroCliente);

        enviar("{\"id\": 2, \"X-Burger\": [\"15.00\", \"lanche\", \"1\"]}");

        assertSame(outroCliente, pedidoEnviadoAoDao().getCliente());
        verify(validadorCookie, never()).getCookieIdCliente(any());
    }

    @Test
    @DisplayName("Carrinho vazio: hoje o pedido é gravado com total zero e sem itens")
    void caracterizacaoCarrinhoVazioGeraPedidoZerado() throws Exception {
        enviar("{\"id\": 1}");

        assertEquals(0.00, pedidoEnviadoAoDao().getValor_total(), 0.001);
        verify(pedidoDao, never()).vincularLanche(any(), any());
        verify(pedidoDao, never()).vincularBebida(any(), any());
    }

    @Test
    @DisplayName("Item de tipo desconhecido é ignorado sem aviso")
    void caracterizacaoTipoDesconhecidoEhIgnorado() throws Exception {
        enviar("{\"id\": 1, \"Pudim\": [\"8.00\", \"sobremesa\", \"1\"]}");

        assertEquals(0.00, pedidoEnviadoAoDao().getValor_total(), 0.001);
        verify(lancheDao, never()).pesquisaPorNome(any(String.class));
        verify(bebidaDao, never()).pesquisaPorNome(any());
    }

    @Test
    @DisplayName("Quantidade zero ou negativa é repassada ao vínculo sem validação")
    void caracterizacaoQuantidadeNegativaEhAceita() throws Exception {
        enviar("{\"id\": 1, \"X-Burger\": [\"15.00\", \"lanche\", -2]}");

        verify(pedidoDao).vincularLanche(pedidoGravado, xBurger);
        assertEquals(-2, xBurger.getQuantidade());
    }

    @Test
    @DisplayName("Lanche inexistente interrompe a compra antes de gravar o pedido")
    void deveInterromperCompraQuandoLancheNaoExiste() {
        // Os DAOs devolvem um objeto vazio (preço nulo) quando o nome não é encontrado
        when(lancheDao.pesquisaPorNome("X-Fantasma")).thenReturn(new Lanche());

        assertThrows(NullPointerException.class,
                () -> enviar("{\"id\": 1, \"X-Fantasma\": [\"9.00\", \"lanche\", \"1\"]}"));

        verify(pedidoDao, never()).salvar(any());
    }

    @Test
    @DisplayName("JSON malformado gera JSONException e nada é gravado")
    void deveRecusarJsonMalformado() {
        assertThrows(JSONException.class, () -> enviar("{\"id\": 1, \"X-Burger\": "));

        verify(pedidoDao, never()).salvar(any());
    }

    @Test
    @DisplayName("JSON sem o campo id gera JSONException e nada é gravado")
    void deveRecusarPedidoSemIdDoCliente() {
        assertThrows(JSONException.class, () -> enviar("{\"X-Burger\": [\"15.00\", \"lanche\", \"1\"]}"));

        verify(pedidoDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Item fora do formato [preco, tipo, quantidade] gera JSONException")
    void deveRecusarItemForaDoFormato() {
        assertThrows(JSONException.class, () -> enviar("{\"id\": 1, \"X-Burger\": \"dois\"}"));

        verify(pedidoDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Corpo vazio com cookie válido gera NullPointerException")
    void caracterizacaoCorpoVazioGeraNullPointer() throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.vazio());

        assertThrows(NullPointerException.class, () -> controller.processRequest(request, response));
    }

    @Test
    @DisplayName("Falha ao vincular a bebida acontece depois de o pedido já ter sido gravado")
    void caracterizacaoFalhaParcialDeixaPedidoGravado() {
        doThrow(new RuntimeException("falha ao inserir em tb_bebidas_pedido"))
                .when(pedidoDao).vincularBebida(any(), any());

        assertThrows(RuntimeException.class,
                () -> enviar("{\"id\": 1, \"X-Burger\": [\"15.00\", \"lanche\", \"1\"], \"Refrigerante\": [\"5.00\", \"bebida\", \"1\"]}"));

        // Não há transação: pedido e lanche ficam gravados sem a bebida
        verify(pedidoDao).salvar(any(Pedido.class));
        verify(pedidoDao).vincularLanche(pedidoGravado, xBurger);
    }
}
