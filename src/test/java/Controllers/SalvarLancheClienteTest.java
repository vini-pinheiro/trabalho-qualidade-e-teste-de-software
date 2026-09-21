package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;
import apoio.CorpoRequisicao;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato de entrada (web/view/montarLanche/montarLanche.js):
 * {"nome": ..., "descricao": ..., "ingredientes": {"Nome do ingrediente": quantidade}}
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalvarLancheClienteTest {

    private static final String LANCHE_DA_CASA = "{\"nome\": \"Lanche da Bia\", \"descricao\": \"Sem cebola\","
            + " \"ingredientes\": {\"Queijo Cheddar\": \"2\", \"Bacon Crocante\": \"3\", \"Pao com Gergelim\": 1}}";

    @Mock
    private ValidadorCookie validadorCookie;
    @Mock
    private DaoLanche lancheDao;
    @Mock
    private DaoIngrediente ingredienteDao;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private salvarLancheCliente controller;
    private StringWriter resposta;
    private Lanche lancheGravado;
    private final Map<String, Ingrediente> estoque = new HashMap<String, Ingrediente>();

    @BeforeEach
    void preparar() throws Exception {
        controller = new salvarLancheCliente(validadorCookie, lancheDao, ingredienteDao);
        resposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(resposta));
        when(validadorCookie.validar(any())).thenReturn(true);

        cadastrarIngrediente(1, "Pao com Gergelim", 2.50);
        cadastrarIngrediente(3, "Queijo Cheddar", 2.00);
        cadastrarIngrediente(4, "Bacon Crocante", 4.00);
        // O DAO real devolve um Ingrediente vazio quando o nome não existe
        when(ingredienteDao.pesquisaPorNome(any(Ingrediente.class))).thenAnswer(chamada -> {
            Ingrediente procurado = chamada.getArgument(0);
            Ingrediente encontrado = estoque.get(procurado.getNome());
            return encontrado != null ? encontrado : new Ingrediente();
        });

        lancheGravado = new Lanche();
        lancheGravado.setId_lanche(50);
        lancheGravado.setNome("Lanche da Bia");
        lancheGravado.setValor_venda(18.50);
        when(lancheDao.pesquisaPorNome(any(Lanche.class))).thenReturn(lancheGravado);
    }

    private void cadastrarIngrediente(int id, String nome, double valorVenda) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId_ingrediente(id);
        ingrediente.setNome(nome);
        ingrediente.setValor_venda(valorVenda);
        estoque.put(nome, ingrediente);
    }

    private void enviar(String json) throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(json));
        controller.processRequest(request, response);
    }

    private Lanche lancheEnviadoAoDao() {
        ArgumentCaptor<Lanche> captor = ArgumentCaptor.forClass(Lanche.class);
        verify(lancheDao).salvarCliente(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Preço do lanche montado é a soma de valor de venda x quantidade de cada ingrediente")
    void deveCalcularPrecoPelaSomaDosIngredientes() throws Exception {
        double precoEsperado = 2 * 2.00 + 3 * 4.00 + 1 * 2.50;

        enviar(LANCHE_DA_CASA);

        Lanche lanche = lancheEnviadoAoDao();
        assertEquals(precoEsperado, lanche.getValor_venda(), 0.001);
        assertEquals("Lanche da Bia", lanche.getNome());
        assertEquals("Sem cebola", lanche.getDescricao());
    }

    @Test
    @DisplayName("Resposta aponta para o carrinho com nome e preço lidos de volta do banco")
    void deveResponderComLinkDoCarrinho() throws Exception {
        enviar(LANCHE_DA_CASA);

        assertEquals("../carrinho/carrinho.html?nome=Lanche da Bia&preco=18.5", resposta.toString().trim());
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("DEF-01: cada ingrediente escolhido deve ser vinculado ao lanche com sua quantidade")
    void deveVincularCadaIngredienteAoLanche() throws Exception {
        enviar(LANCHE_DA_CASA);

        ArgumentCaptor<Ingrediente> vinculados = ArgumentCaptor.forClass(Ingrediente.class);
        verify(lancheDao, times(3)).vincularIngrediente(eq(lancheGravado), vinculados.capture());
        Map<String, Integer> quantidadePorNome = new HashMap<String, Integer>();
        for (Ingrediente ingrediente : vinculados.getAllValues()) {
            quantidadePorNome.put(ingrediente.getNome(), ingrediente.getQuantidade());
        }
        assertEquals(Integer.valueOf(2), quantidadePorNome.get("Queijo Cheddar"));
        assertEquals(Integer.valueOf(3), quantidadePorNome.get("Bacon Crocante"));
        assertEquals(Integer.valueOf(1), quantidadePorNome.get("Pao com Gergelim"));
    }

    @Test
    @DisplayName("Caracterização do DEF-01: hoje nenhum ingrediente é vinculado ao lanche")
    void caracterizacaoNenhumIngredienteEhVinculado() throws Exception {
        enviar(LANCHE_DA_CASA);

        // O Iterator das chaves já foi consumido no cálculo do preço; o segundo laço não executa.
        // Remover quando o DEF-01 for corrigido.
        verify(lancheDao).salvarCliente(any());
        verify(lancheDao, never()).vincularIngrediente(any(), any());
    }

    @Test
    @DisplayName("Sem token de cliente válido a montagem é recusada e nada é gravado")
    void deveRecusarMontagemSemTokenValido() throws Exception {
        when(validadorCookie.validar(any())).thenReturn(false);

        enviar(LANCHE_DA_CASA);

        assertEquals("erro", resposta.toString().trim());
        verify(lancheDao, never()).salvarCliente(any());
    }

    @Test
    @DisplayName("Requisição sem cookies (NullPointerException no validador) é recusada")
    void deveRecusarMontagemSemCookies() throws Exception {
        when(validadorCookie.validar(any())).thenThrow(new NullPointerException());

        enviar(LANCHE_DA_CASA);

        assertEquals("erro", resposta.toString().trim());
        verify(lancheDao, never()).salvarCliente(any());
    }

    @Test
    @DisplayName("Ingrediente inexistente interrompe a montagem antes de gravar o lanche")
    void deveInterromperMontagemComIngredienteInexistente() {
        String comIngredienteFantasma = "{\"nome\": \"Teste\", \"descricao\": \"x\","
                + " \"ingredientes\": {\"Queijo Cheddar\": 1, \"Trufa Negra\": 1}}";

        assertThrows(NullPointerException.class, () -> enviar(comIngredienteFantasma));

        verify(lancheDao, never()).salvarCliente(any());
    }

    @Test
    @DisplayName("Sem o bloco de ingredientes a requisição gera JSONException")
    void deveRecusarRequisicaoSemIngredientes() {
        assertThrows(JSONException.class, () -> enviar("{\"nome\": \"Teste\", \"descricao\": \"x\"}"));

        verify(lancheDao, never()).salvarCliente(any());
    }

    @Test
    @DisplayName("Composição vazia: o servidor grava lanche com preço zero (só a tela exige o pão)")
    void caracterizacaoComposicaoVaziaGeraLancheDePrecoZero() throws Exception {
        enviar("{\"nome\": \"Vazio\", \"descricao\": \"x\", \"ingredientes\": {}}");

        assertEquals(0.00, lancheEnviadoAoDao().getValor_venda(), 0.001);
    }

    @Test
    @DisplayName("Quantidade negativa reduz o preço: o servidor não valida o que a tela impede")
    void caracterizacaoQuantidadeNegativaReduzPreco() throws Exception {
        enviar("{\"nome\": \"Desconto\", \"descricao\": \"x\","
                + " \"ingredientes\": {\"Pao com Gergelim\": 1, \"Bacon Crocante\": -1}}");

        assertEquals(2.50 - 4.00, lancheEnviadoAoDao().getValor_venda(), 0.001);
    }
}
