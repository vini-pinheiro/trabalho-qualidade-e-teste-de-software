package e2e;

import DAO.DaoUtil;
import e2e.pages.CardapioPage;
import e2e.pages.CarrinhoCheckoutPage;
import e2e.pages.LoginPage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de Sistema (E2E) do caso de teste CT-02 (Carrinho, pedidos e checkout).
 */
class CheckoutE2E {

    private static final String MD5_SENHA_PADRAO = "e10adc3949ba59abbe56e057f20f883e";
    private static final String SENHA_TEXTO_PADRAO = "123456";

    private String baseUrl;
    private String seleniumUrl;
    private String screenshotsDir;

    private Connection jdbcConnection;
    private WebDriver driver;

    private LoginPage loginPage;
    private CardapioPage cardapioPage;
    private CarrinhoCheckoutPage carrinhoPage;

    private String sufixo;
    private String usuarioCliente;
    private String nomeLanche;
    private String nomeBebida;

    private int idStatusInserido;
    private int idEndereco;
    private int idCliente;
    private int idLanche;
    private int idBebida;

    @BeforeEach
    void prepararAmbienteEMassa() throws Exception {
        baseUrl = System.getProperty("e2e.baseUrl", "http://e2e-app:8080");
        seleniumUrl = System.getProperty("e2e.seleniumUrl", "http://selenium:4444/wd/hub");
        screenshotsDir = System.getProperty("e2e.screenshotsDir", "docs/entrega-2/selenium");

        jdbcConnection = new DaoUtil().conecta();

        sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        usuarioCliente = "e2e_" + sufixo;
        nomeLanche = "BurgerE2E-" + sufixo;
        nomeBebida = "RefriE2E-" + sufixo;

        idStatusInserido = inserirStatusLanchonete("ABERTO");
        idEndereco = inserirEndereco();
        idCliente = inserirCliente(usuarioCliente, idEndereco);
        idLanche = inserirLanche(nomeLanche, 20.00);
        idBebida = inserirBebida(nomeBebida, 6.50);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1440,900");

        driver = new RemoteWebDriver(new URL(seleniumUrl), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        loginPage = new LoginPage(driver);
        cardapioPage = new CardapioPage(driver);
        carrinhoPage = new CarrinhoCheckoutPage(driver);
    }

    @AfterEach
    void limparRecursosEMassa() throws SQLException {
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            try {
                if (jdbcConnection != null && !jdbcConnection.isClosed()) {
                    if (idCliente > 0 || idLanche > 0) {
                        executarDelete(
                                "DELETE FROM tb_lanches_pedido "
                                        + "WHERE id_pedido IN (SELECT id_pedido FROM tb_pedidos WHERE id_cliente = ?) "
                                        + "OR id_lanche = ?",
                                idCliente, idLanche);
                    }
                    if (idCliente > 0 || idBebida > 0) {
                        executarDelete(
                                "DELETE FROM tb_bebidas_pedido "
                                        + "WHERE id_pedido IN (SELECT id_pedido FROM tb_pedidos WHERE id_cliente = ?) "
                                        + "OR id_bebida = ?",
                                idCliente, idBebida);
                    }
                    if (idCliente > 0) {
                        executarDelete("DELETE FROM tb_pedidos WHERE id_cliente = ?", idCliente);
                        executarDeleteString("DELETE FROM tb_tokens WHERE token LIKE ?", idCliente + "-%");
                    }
                    if (idLanche > 0) {
                        executarDelete("DELETE FROM tb_lanches WHERE id_lanche = ?", idLanche);
                    }
                    if (idBebida > 0) {
                        executarDelete("DELETE FROM tb_bebidas WHERE id_bebida = ?", idBebida);
                    }
                    if (idCliente > 0) {
                        executarDelete("DELETE FROM tb_clientes WHERE id_cliente = ?", idCliente);
                    }
                    if (idEndereco > 0) {
                        executarDelete("DELETE FROM tb_enderecos WHERE id_endereco = ?", idEndereco);
                    }
                    if (idStatusInserido > 0) {
                        executarDelete("DELETE FROM tb_status_lanchonete WHERE id_status = ?", idStatusInserido);
                    }
                }
            } finally {
                if (jdbcConnection != null) {
                    try {
                        if (!jdbcConnection.isClosed()) {
                            jdbcConnection.close();
                        }
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Cenário 1 (CT-02): login, adição de lanche/bebida, checkout e persistência no PostgreSQL")
    void deveRealizarLoginAdicionarItensEFinalizarCompraPersistindoNoPostgres() throws Exception {
        loginPage.abrir(baseUrl);
        salvarScreenshot("01-cenario1-login.png");

        loginPage.realizarLogin(usuarioCliente, SENHA_TEXTO_PADRAO);
        assertNotNull(driver.manage().getCookieNamed("token"), "Cookie token deve existir após o login");

        cardapioPage.abrir(baseUrl);
        cardapioPage.aguardarLancheVisivel(nomeLanche);
        salvarScreenshot("02-cenario1-cardapio.png");

        String alertaLanche = cardapioPage.adicionarLancheAoCarrinho(nomeLanche);
        assertTrue(alertaLanche.contains("Lanche salvo!"), "Alerta inesperado ao adicionar lanche: " + alertaLanche);

        cardapioPage.abrirAbaBebidas();
        String alertaBebida = cardapioPage.adicionarBebidaAoCarrinho(nomeBebida);
        assertTrue(alertaBebida.contains("Bebida salva!"), "Alerta inesperado ao adicionar bebida: " + alertaBebida);

        cardapioPage.irParaMeuCarrinho();
        carrinhoPage.aguardarCarregamentoComItens("Cliente E2E", 2);

        List<String> itensCarrinho = carrinhoPage.obterTextoItens();
        assertTrue(itensCarrinho.contains(nomeLanche), "Carrinho deve listar o lanche adicionado");
        assertTrue(itensCarrinho.contains(nomeBebida), "Carrinho deve listar a bebida adicionada");
        assertEquals("R$ 26.50", carrinhoPage.obterValorTotalEtapa1());
        salvarScreenshot("03-cenario1-carrinho.png");

        carrinhoPage.avancarParaFormaDePagamento();
        assertEquals("R$ 26.50", carrinhoPage.obterValorTotalEtapa2());
        String resumo = carrinhoPage.obterTextoResumo();
        assertTrue(resumo.contains(nomeLanche), "Resumo deve conter o lanche");
        assertTrue(resumo.contains(nomeBebida), "Resumo deve conter a bebida");

        carrinhoPage.selecionarPagamentoEEntrega("Crédito");
        salvarScreenshot("04-cenario1-resumo-checkout.png");

        String alertaFinalizacao = carrinhoPage.clicarFinalizarECapturarAlerta();
        assertEquals("Pedido Salvo com Sucesso!", alertaFinalizacao.trim());

        carrinhoPage.aguardarCarrinhoLimpoAposFinalizacao();
        salvarScreenshot("05-cenario1-pos-finalizacao.png");

        validarPedidoPersistidoNoBanco();
    }

    @Test
    @DisplayName("Cenário 2a (Negativo): acesso direto ao carrinho sem login redireciona com alerta e não cria pedido")
    void deveBloquearAcessoDiretoAoCarrinhoSemAutenticacaoENaoCriarPedido() throws Exception {
        carrinhoPage.abrirDireto(baseUrl);

        String alertaSessao = loginPage.aguardarEAceitarAlerta();
        assertTrue(alertaSessao.contains("Sua sessão expirou"),
                "Alerta inesperado ao acessar carrinho sem autenticação: " + alertaSessao);

        loginPage.aguardarTelaLoginVisivel();
        assertTrue(driver.getCurrentUrl().contains("login.html?Action=TokenError"),
                "URL deveria redirecionar para login.html?Action=TokenError, mas foi: " + driver.getCurrentUrl());

        salvarScreenshot("06-cenario2a-bloqueio-sem-login.png");
        validarNenhumPedidoPersistido();
    }

    @Test
    @DisplayName("Cenário 2b (Negativo): finalizar compra sem cookie token válido exibe erro e não cria pedido")
    void deveRecusarFinalizacaoDeCompraSemCookieTokenValidoENaoPersistirPedido() throws Exception {
        loginPage.abrir(baseUrl);
        loginPage.realizarLogin(usuarioCliente, SENHA_TEXTO_PADRAO);
        assertNotNull(driver.manage().getCookieNamed("token"));

        cardapioPage.abrir(baseUrl);
        cardapioPage.aguardarLancheVisivel(nomeLanche);
        cardapioPage.adicionarLancheAoCarrinho(nomeLanche);
        cardapioPage.abrirAbaBebidas();
        cardapioPage.adicionarBebidaAoCarrinho(nomeBebida);

        cardapioPage.irParaMeuCarrinho();
        carrinhoPage.aguardarCarregamentoComItens("Cliente E2E", 2);
        carrinhoPage.avancarParaFormaDePagamento();
        carrinhoPage.selecionarPagamentoEEntrega("Crédito");

        driver.manage().deleteCookieNamed("token");
        assertNull(driver.manage().getCookieNamed("token"), "Cookie token deve estar removido antes de finalizar");

        String alertaErro = carrinhoPage.clicarFinalizarECapturarAlerta();
        assertEquals("Ocorreu um Erro com seu Pedido! tente logar Novamente.", alertaErro.trim());

        loginPage.aguardarTelaLoginVisivel();
        salvarScreenshot("07-cenario2b-erro-sem-cookie.png");
        validarNenhumPedidoPersistido();
    }

    private void validarPedidoPersistidoNoBanco() throws SQLException {
        int idPedidoGerado;
        String sqlPedido = "SELECT id_pedido, data_pedido, valor_total FROM tb_pedidos WHERE id_cliente = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sqlPedido)) {
            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Pedido deve existir em tb_pedidos");
                idPedidoGerado = rs.getInt("id_pedido");
                String dataPedido = rs.getString("data_pedido");
                double valorTotal = rs.getDouble("valor_total");
                assertFalse(rs.next(), "Deve existir exatamente um pedido para o cliente E2E");

                assertTrue(idPedidoGerado > 0);
                assertNotNull(dataPedido);
                assertDoesNotThrow(() -> Instant.parse(dataPedido));
                assertEquals(26.50, valorTotal, 0.001);
            }
        }

        String sqlLanche = "SELECT quantidade FROM tb_lanches_pedido WHERE id_pedido = ? AND id_lanche = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sqlLanche)) {
            stmt.setInt(1, idPedidoGerado);
            stmt.setInt(2, idLanche);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Lanche deve estar vinculado em tb_lanches_pedido");
                assertEquals(1, rs.getInt("quantidade"));
                assertFalse(rs.next());
            }
        }

        String sqlBebida = "SELECT quantidade FROM tb_bebidas_pedido WHERE id_pedido = ? AND id_bebida = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sqlBebida)) {
            stmt.setInt(1, idPedidoGerado);
            stmt.setInt(2, idBebida);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Bebida deve estar vinculada em tb_bebidas_pedido");
                assertEquals(1, rs.getInt("quantidade"));
                assertFalse(rs.next());
            }
        }
    }

    private void validarNenhumPedidoPersistido() throws SQLException {
        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_pedidos WHERE id_cliente = ?", idCliente));
        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_lanches_pedido WHERE id_lanche = ?", idLanche));
        assertEquals(0, contarRegistros("SELECT COUNT(*) FROM tb_bebidas_pedido WHERE id_bebida = ?", idBebida));
    }

    private void salvarScreenshot(String nomeArquivo) throws IOException {
        Path diretorio = Paths.get(screenshotsDir);
        Files.createDirectories(diretorio);
        File origem = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Path destino = diretorio.resolve(nomeArquivo);
        Files.copy(origem.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
    }

    private int inserirStatusLanchonete(String status) throws SQLException {
        String sql = "INSERT INTO tb_status_lanchonete(status) VALUES(?) RETURNING id_status";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private int inserirEndereco() throws SQLException {
        String sql = "INSERT INTO tb_enderecos(rua, bairro, numero, complemento, cidade, estado) "
                + "VALUES(?,?,?,?,?,?) RETURNING id_endereco";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, "Rua E2E");
            stmt.setString(2, "Centro");
            stmt.setInt(3, 100);
            stmt.setString(4, "Sala 1");
            stmt.setString(5, "Curitiba");
            stmt.setString(6, "PR");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private int inserirCliente(String usuario, int idEnd) throws SQLException {
        String sql = "INSERT INTO tb_clientes(nome, sobrenome, telefone, usuario, senha, fg_ativo, id_endereco) "
                + "VALUES(?,?,?,?,?,1,?) RETURNING id_cliente";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, "Cliente");
            stmt.setString(2, "E2E");
            stmt.setString(3, "41999992222");
            stmt.setString(4, usuario);
            stmt.setString(5, MD5_SENHA_PADRAO);
            stmt.setInt(6, idEnd);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private int inserirLanche(String nome, double valorVenda) throws SQLException {
        String sql = "INSERT INTO tb_lanches(nm_lanche, descricao, valor_venda, fg_ativo) "
                + "VALUES(?,?,?,1) RETURNING id_lanche";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, "Lanche teste E2E");
            stmt.setDouble(3, valorVenda);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private int inserirBebida(String nome, double valorVenda) throws SQLException {
        String sql = "INSERT INTO tb_bebidas(nm_bebida, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo) "
                + "VALUES(?,?,?,?,?,?,1) RETURNING id_bebida";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, "Bebida teste E2E");
            stmt.setInt(3, 30);
            stmt.setDouble(4, 2.50);
            stmt.setDouble(5, valorVenda);
            stmt.setString(6, "Refrigerante");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private int contarRegistros(String sql, int parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setInt(1, parametro);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private void executarDelete(String sql, int... parametros) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setInt(i + 1, parametros[i]);
            }
            stmt.executeUpdate();
        }
    }

    private void executarDeleteString(String sql, String parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, parametro);
            stmt.executeUpdate();
        }
    }
}
