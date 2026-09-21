package Controllers;

import DAO.DaoCliente;
import Model.Cliente;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato de entrada (web/view/cadastro/cadastro.js):
 * {"usuario": {nome, sobrenome, telefone, usuario, senha}, "endereco": {rua, numero, bairro, complemento, cidade, estado}}
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CadastroTest {

    @Mock
    private DaoCliente clienteDao;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private cadastro controller;
    private StringWriter resposta;

    @BeforeEach
    void preparar() throws Exception {
        controller = new cadastro(clienteDao);
        resposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(resposta));
    }

    private static String cadastroJson(String nome, String usuario, String senha, String numero) {
        return "{\"usuario\": {\"nome\": \"" + nome + "\", \"sobrenome\": \"Lima\", \"telefone\": \"41988887777\","
                + " \"usuario\": \"" + usuario + "\", \"senha\": \"" + senha + "\"},"
                + " \"endereco\": {\"rua\": \"Rua das Flores\", \"numero\": \"" + numero + "\", \"bairro\": \"Centro\","
                + " \"complemento\": \"\", \"cidade\": \"Curitiba\", \"estado\": \"PR\"}}";
    }

    private void enviar(String json) throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(json));
        controller.processRequest(request, response);
    }

    private Cliente clienteEnviadoAoDao() {
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteDao).salvar(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Cadastro válido monta cliente ativo com endereço e confirma ao usuário")
    void deveMontarClienteAtivoComEndereco() throws Exception {
        enviar(cadastroJson("Bruna", "bruna.lima", "S3nh@Forte", "123"));

        Cliente cliente = clienteEnviadoAoDao();
        assertEquals("Bruna", cliente.getNome());
        assertEquals("Lima", cliente.getSobrenome());
        assertEquals("bruna.lima", cliente.getUsuario());
        assertEquals("S3nh@Forte", cliente.getSenha());
        assertEquals(1, cliente.getFg_ativo());
        assertEquals("Rua das Flores", cliente.getEndereco().getRua());
        assertEquals(123, cliente.getEndereco().getNumero());
        assertEquals("PR", cliente.getEndereco().getEstado());
        assertEquals("Usuário Cadastrado!", resposta.toString().trim());
    }

    @Test
    @DisplayName("Número do endereço vazio ou não numérico gera JSONException e nada é gravado")
    void deveRecusarNumeroDeEnderecoInvalido() {
        assertThrows(JSONException.class, () -> enviar(cadastroJson("Bruna", "bruna.lima", "123456", "")));
        assertThrows(JSONException.class, () -> enviar(cadastroJson("Bruna", "bruna.lima", "123456", "s/n")));

        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Requisição sem o bloco de endereço gera JSONException e nada é gravado")
    void deveRecusarCadastroSemEndereco() {
        String semEndereco = "{\"usuario\": {\"nome\": \"Bruna\", \"sobrenome\": \"Lima\", \"telefone\": \"1\","
                + " \"usuario\": \"bruna\", \"senha\": \"123456\"}}";

        assertThrows(JSONException.class, () -> enviar(semEndereco));

        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("DEF-05: campos obrigatórios vazios não podem chegar ao banco")
    void naoDeveGravarClienteComCamposObrigatoriosVazios() throws Exception {
        try {
            enviar(cadastroJson("", "", "", "123"));
        } catch (RuntimeException recusaAceitavel) {
            // recusar com erro também atende ao requisito
        }

        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Caracterização do DEF-05: nome, usuário e senha vazios são aceitos pelo servidor")
    void caracterizacaoCamposVaziosSaoAceitos() throws Exception {
        enviar(cadastroJson("", "", "", "123"));

        Cliente cliente = clienteEnviadoAoDao();
        assertEquals("", cliente.getNome());
        assertEquals("", cliente.getUsuario());
        assertEquals("", cliente.getSenha());
        assertEquals("Usuário Cadastrado!", resposta.toString().trim());
    }

    @Test
    @DisplayName("Falha do DAO ao gravar é repassada e a confirmação não é enviada")
    void deveRepassarFalhaDoDao() {
        doThrow(new RuntimeException("falha no INSERT")).when(clienteDao).salvar(any());

        assertThrows(RuntimeException.class, () -> enviar(cadastroJson("Bruna", "bruna.lima", "123456", "123")));

        assertEquals("", resposta.toString());
    }
}
