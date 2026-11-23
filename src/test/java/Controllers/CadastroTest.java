package Controllers;


import DAO.DaoCliente;
import Model.Cliente;
import apoio.CorpoRequisicao;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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



    // Trabalho parte 2 - Novos casos de teste


    @Test
    @DisplayName("Mapeia todos os campos do endereço e o telefone do usuário")
    void deveMapearTodosOsCamposDoEndereco() throws Exception {
        String json = "{\"usuario\": {\"nome\": \"Bruna\", \"sobrenome\": \"Lima\", \"telefone\": \"41988887777\","
                + " \"usuario\": \"bruna.lima\", \"senha\": \"123456\"},"
                + " \"endereco\": {\"rua\": \"Rua das Flores\", \"numero\": \"45\", \"bairro\": \"Batel\","
                + " \"complemento\": \"Apto 12\", \"cidade\": \"Curitiba\", \"estado\": \"PR\"}}";

        enviar(json);

        Cliente cliente = clienteEnviadoAoDao();
        assertEquals("41988887777", cliente.getTelefone());
        assertEquals("Batel", cliente.getEndereco().getBairro());
        assertEquals("Apto 12", cliente.getEndereco().getComplemento());
        assertEquals("Curitiba", cliente.getEndereco().getCidade());
        assertEquals(45, cliente.getEndereco().getNumero());
        assertEquals(0, cliente.getId_cliente(), "o id é gerado pelo banco, não pelo servlet");
    }

    @Test
    @DisplayName("Número do endereço enviado como número JSON (sem aspas) também é aceito")
    void deveAceitarNumeroComoNumeroJson() throws Exception {
        enviar(cadastroJson("Bruna", "bruna.lima", "123456", "0").replace("\"numero\": \"0\"", "\"numero\": 77"));

        assertEquals(77, clienteEnviadoAoDao().getEndereco().getNumero());
    }

    @Test
    @DisplayName("Campos extras no JSON são ignorados e o fg_ativo enviado pelo cliente não é usado")
    void deveIgnorarCamposExtras() throws Exception {
        String json = cadastroJson("Bruna", "bruna.lima", "123456", "1")
                .replace("\"usuario\": {", "\"usuario\": {\"fg_ativo\": 0, \"admin\": true, ");

        enviar(json);

        assertEquals(1, clienteEnviadoAoDao().getFg_ativo());
    }

    @Test
    @DisplayName("Define o tipo de conteúdo JSON e a codificação UTF-8 na resposta")
    void deveDefinirCabecalhosDaResposta() throws Exception {
        enviar(cadastroJson("Bruna", "bruna.lima", "123456", "1"));

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    @ParameterizedTest(name = "sem o campo \"{0}\" a requisição é recusada")
    @ValueSource(strings = {"nome", "sobrenome", "telefone", "usuario", "senha"})
    @DisplayName("Campo obrigatório ausente no bloco usuario gera JSONException e nada é gravado")
    void deveRecusarCampoObrigatorioAusente(String campo) {
        String json = cadastroJson("Bruna", "bruna.lima", "123456", "1")
                .replace("\"" + campo + "\": \"", "\"_ignorado_" + campo + "\": \"");

        assertThrows(JSONException.class, () -> enviar(json));
        verify(clienteDao, never()).salvar(any());
    }

    @ParameterizedTest(name = "sem o campo \"{0}\" do endereço a requisição é recusada")
    @ValueSource(strings = {"rua", "bairro", "complemento", "cidade", "estado"})
    @DisplayName("Campo ausente no bloco endereco gera JSONException e nada é gravado")
    void deveRecusarCampoDeEnderecoAusente(String campo) {
        String json = cadastroJson("Bruna", "bruna.lima", "123456", "1")
                .replace("\"" + campo + "\": \"", "\"_ignorado_" + campo + "\": \"");

        assertThrows(JSONException.class, () -> enviar(json));
        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("JSON malformado gera JSONException e nada é gravado")
    void deveRecusarJsonMalformado() {
        assertThrows(JSONException.class, () -> enviar("{\"usuario\": {\"nome\": "));
        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Caracterização: corpo vazio lança NullPointerException (readLine devolve null)")
    void caracterizacaoCorpoVazio() throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.vazio());

        assertThrows(NullPointerException.class, () -> controller.processRequest(request, response));
        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("doPost delega para processRequest e grava o cliente")
    void doPostDeveCadastrar() throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(cadastroJson("Bruna", "bruna.lima", "123456", "1")));

        controller.doPost(request, response);

        verify(clienteDao).salvar(any());
        assertEquals("Usuário Cadastrado!", resposta.toString().trim());
    }

    @Test
    @DisplayName("Caracterização: doGet também cadastra (operação com efeito colateral aceita via GET)")
    void caracterizacaoDoGetTambemCadastra() throws Exception {
        when(request.getInputStream()).thenReturn(CorpoRequisicao.de(cadastroJson("Bruna", "bruna.lima", "123456", "1")));

        controller.doGet(request, response);

        verify(clienteDao).salvar(any());
        assertEquals("Short description", controller.getServletInfo());
    }

    @Test
    @DisplayName("Entrega 2 (Sonar): falha de leitura no doPost vira HTTP 500, sem escapar do servlet")
    void doPostDeveResponder500QuandoLeituraFalha() throws Exception {
        when(request.getInputStream()).thenThrow(new java.io.IOException("conexão interrompida"));

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Caracterização: o servlet não verifica se o usuário já existe antes de gravar (achado do CT-03)")
    void caracterizacaoNaoVerificaUsuarioDuplicado() throws Exception {
        enviar(cadastroJson("Bruna", "bruna.lima", "123456", "1"));

        verify(clienteDao, never()).pesquisaPorUsuario(any());
        verify(clienteDao).salvar(any());
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("Usuário já cadastrado não pode ser gravado de novo (achado do CT-03, ainda sem issue)")
    void naoDeveGravarUsuarioDuplicado() throws Exception {
        Cliente existente = new Cliente();
        existente.setId_cliente(5);
        existente.setUsuario("bruna.lima");
        when(clienteDao.pesquisaPorUsuario(any())).thenReturn(existente);

        try {
            enviar(cadastroJson("Bruna", "bruna.lima", "123456", "1"));
        } catch (RuntimeException recusaAceitavel) {
            // recusar com erro também atende ao requisito
        }

        verify(clienteDao, never()).salvar(any());
    }

    @Test
    @DisplayName("Caracterização: com charset padrão UTF-8, acentos do nome são corrompidos (achado do CT-03)")
    void caracterizacaoAcentosCorrompidos() throws Exception {
        // A conversão ISO-8859-1 -> UTF-8 só "funciona" quando a JVM lê o corpo em ISO-8859-1/Cp1252.
        // Com o charset padrão UTF-8 (Docker, Java 18+), cada letra acentuada vira o caractere U+FFFD.
        assumeTrue(Charset.defaultCharset().equals(StandardCharsets.UTF_8), "requer charset padrão UTF-8");

        enviar(cadastroJson("José Conceição", "jose", "123456", "1"));

        String nome = clienteEnviadoAoDao().getNome();
        assertNotEquals("José Conceição", nome);
        assertEquals("Jos� Concei��o", nome);
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("Nome com acento deve chegar intacto ao DAO (achado do CT-03, ainda sem issue)")
    void devePreservarAcentosDoNome() throws Exception {
        enviar(cadastroJson("José Conceição", "jose", "123456", "1"));

        assertEquals("José Conceição", clienteEnviadoAoDao().getNome());
    }
}
