package Controllers;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Servlets salvarIngrediente e alterarIngrediente (painel do funcionário).
 * Contrato de entrada (web/view/estoque/estoque.js):
 * {"id"?, "nome", "descricao", "quantidade", "ValorCompra", "ValorVenda", "tipo"}
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngredienteControllersTest {

    @Mock
    private ValidadorCookie validadorCookie;
    @Mock
    private DaoIngrediente ingredienteDao;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private salvarIngrediente cadastroDeInsumo;
    private alterarIngrediente alteracaoDeInsumo;
    private StringWriter resposta;

    @BeforeEach
    void preparar() throws Exception {
        cadastroDeInsumo = new salvarIngrediente(validadorCookie, ingredienteDao);
        alteracaoDeInsumo = new alterarIngrediente(validadorCookie, ingredienteDao);
        resposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(resposta));
        when(validadorCookie.validarFuncionario(any())).thenReturn(true);
    }

    private static String insumoJson(String id, String quantidade, String valorCompra, String valorVenda) {
        String campoId = (id == null) ? "" : "\"id\": " + id + ", ";
        return "{" + campoId + "\"nome\": \"Queijo Cheddar\", \"descricao\": \"Fatia\", \"quantidade\": \"" + quantidade
                + "\", \"ValorCompra\": \"" + valorCompra + "\", \"ValorVenda\": \"" + valorVenda + "\", \"tipo\": \"queijo\"}";
    }

    private void corpo(String json) throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(json));
    }

    @Test
    @DisplayName("Funcionário autenticado cadastra insumo ativo com os valores informados")
    void deveCadastrarInsumoComoFuncionario() throws Exception {
        corpo(insumoJson(null, "150", "0.80", "2.00"));

        cadastroDeInsumo.processRequest(request, response);

        ArgumentCaptor<Ingrediente> captor = ArgumentCaptor.forClass(Ingrediente.class);
        verify(ingredienteDao).salvar(captor.capture());
        Ingrediente insumo = captor.getValue();
        assertEquals("Queijo Cheddar", insumo.getNome());
        assertEquals(150, insumo.getQuantidade());
        assertEquals(0.80, insumo.getValor_compra(), 0.001);
        assertEquals(2.00, insumo.getValor_venda(), 0.001);
        assertEquals("queijo", insumo.getTipo());
        assertEquals(1, insumo.getFg_ativo());
        assertEquals("Ingrediente Salvo!", resposta.toString().trim());
    }

    @Test
    @DisplayName("Atualização de estoque leva o id e a nova quantidade ao DAO")
    void deveAtualizarEstoquePeloId() throws Exception {
        corpo(insumoJson("3", "0", "0.80", "2.00"));

        alteracaoDeInsumo.processRequest(request, response);

        ArgumentCaptor<Ingrediente> captor = ArgumentCaptor.forClass(Ingrediente.class);
        verify(ingredienteDao).alterar(captor.capture());
        assertEquals(3, captor.getValue().getId_ingrediente());
        assertEquals(0, captor.getValue().getQuantidade());
        assertEquals("Ingrediente Alterado!", resposta.toString().trim());
    }

    @Test
    @DisplayName("Sem token de funcionário o painel responde erro e não toca no estoque")
    void deveBloquearQuemNaoEhFuncionario() throws Exception {
        when(validadorCookie.validarFuncionario(any())).thenReturn(false);
        corpo(insumoJson("3", "10", "0.80", "2.00"));

        cadastroDeInsumo.processRequest(request, response);
        assertEquals("erro", resposta.toString().trim());

        // cada servlet fecha o writer ao responder, por isso a segunda chamada usa outro
        StringWriter respostaDaAlteracao = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(respostaDaAlteracao));
        alteracaoDeInsumo.processRequest(request, response);
        assertEquals("erro", respostaDaAlteracao.toString().trim());

        verifyNoInteractions(ingredienteDao);
        // token de cliente não é consultado para o painel
        verify(validadorCookie, never()).validar(any());
    }

    @Test
    @DisplayName("Requisição sem cookies (NullPointerException no validador) é bloqueada")
    void deveBloquearRequisicaoSemCookies() throws Exception {
        when(validadorCookie.validarFuncionario(any())).thenThrow(new NullPointerException());
        corpo(insumoJson(null, "10", "0.80", "2.00"));

        cadastroDeInsumo.processRequest(request, response);

        assertEquals("erro", resposta.toString().trim());
        verifyNoInteractions(ingredienteDao);
    }

    @Test
    @DisplayName("Quantidade não numérica ou decimal gera JSONException e nada é gravado")
    void deveRecusarQuantidadeQueNaoEhInteira() throws Exception {
        corpo(insumoJson(null, "dez", "0.80", "2.00"));
        assertThrows(JSONException.class, () -> cadastroDeInsumo.processRequest(request, response));

        corpo(insumoJson(null, "", "0.80", "2.00"));
        assertThrows(JSONException.class, () -> cadastroDeInsumo.processRequest(request, response));

        verifyNoInteractions(ingredienteDao);
    }

    @Test
    @DisplayName("Alteração sem o id do insumo gera JSONException")
    void deveRecusarAlteracaoSemId() throws Exception {
        corpo(insumoJson(null, "10", "0.80", "2.00"));

        assertThrows(JSONException.class, () -> alteracaoDeInsumo.processRequest(request, response));

        verifyNoInteractions(ingredienteDao);
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("DEF-07: estoque e valores negativos não podem ser gravados")
    void naoDeveGravarEstoqueOuValoresNegativos() throws Exception {
        corpo(insumoJson(null, "-5", "-0.80", "-2.00"));

        try {
            cadastroDeInsumo.processRequest(request, response);
        } catch (RuntimeException recusaAceitavel) {
            // recusar com erro também atende ao requisito
        }

        verify(ingredienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Caracterização do DEF-07: quantidade e valores negativos chegam ao DAO")
    void caracterizacaoValoresNegativosSaoAceitos() throws Exception {
        corpo(insumoJson(null, "-5", "-0.80", "-2.00"));

        cadastroDeInsumo.processRequest(request, response);

        ArgumentCaptor<Ingrediente> captor = ArgumentCaptor.forClass(Ingrediente.class);
        verify(ingredienteDao).salvar(captor.capture());
        assertEquals(-5, captor.getValue().getQuantidade());
        assertEquals(-0.80, captor.getValue().getValor_compra(), 0.001);
        assertEquals("Ingrediente Salvo!", resposta.toString().trim());
    }
}
