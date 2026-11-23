package e2e;

import DAO.DaoUtil;
import e2e.pages.CadastroPage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de sistema (E2E) do caso de teste manual CT-03 (cadastro de cliente, história U4).
 *
 * Automatiza no navegador os mesmos passos executados à mão na Entrega 1:
 * - cenário 1 (passos 1 e 2): cadastro válido e login com o cliente novo, que PASSARAM no CT-03;
 * - cenários 2 a 4 (passos 3 a 7): campos vazios, usuário duplicado e acentos, que FALHARAM no CT-03.
 *   Esses ficam marcados como defeito-conhecido: só rodam com -Pdefeitos e devem falhar até a correção.
 *
 * Usa a mesma estrutura do CheckoutE2E: aplicação em http://e2e-app:8080, Selenium remoto em
 * http://selenium:4444/wd/hub e banco pelo DaoUtil (host "db"), todos na rede do docker compose.
 */
class CadastroE2E {

    private static final String SENHA = "123456";
    private static final String HASH_SENHA = "e10adc3949ba59abbe56e057f20f883e";

    private String baseUrl;
    private String screenshotsDir;

    private Connection jdbcConnection;
    private WebDriver driver;
    private CadastroPage cadastroPage;
    private LoginPage loginPage;

    private String usuario;
    private String rua;

    @BeforeEach
    void preparar() throws Exception {
        baseUrl = System.getProperty("e2e.baseUrl", "http://e2e-app:8080");
        String seleniumUrl = System.getProperty("e2e.seleniumUrl", "http://selenium:4444/wd/hub");
        screenshotsDir = System.getProperty("e2e.screenshotsDir", "docs/entrega-2/thiago-ferreira/Selenium");

        jdbcConnection = new DaoUtil().conecta();

        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        usuario = "e2e_cad_" + sufixo;
        rua = "Rua do Ouvidor " + sufixo;

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1440,900");

        driver = new RemoteWebDriver(new URL(seleniumUrl), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        cadastroPage = new CadastroPage(driver);
        loginPage = new LoginPage(driver);
    }

    @AfterEach
    void limpar() throws SQLException {
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            try {
                if (jdbcConnection != null && !jdbcConnection.isClosed()) {
                    // tokens do login -> clientes -> endereços (ordem das chaves estrangeiras)
                    for (int idCliente : idsDosClientes(usuario)) {
                        executarDelete("DELETE FROM tb_tokens WHERE token LIKE ?", idCliente + "-%");
                    }
                    executarDelete("DELETE FROM tb_clientes WHERE usuario = ?", usuario);
                    executarDelete("DELETE FROM tb_enderecos WHERE rua = ?", rua);
                }
            } finally {
                if (jdbcConnection != null) {
                    try {
                        jdbcConnection.close();
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Cenário 1 (CT-03, passos 1 e 2): cadastro válido, login e carrinho com o nome do cliente")
    void deveCadastrarClienteEEntrarComOsDadosCadastrados() throws Exception {
        cadastroPage.abrir(baseUrl);
        cadastroPage.preencherUsuario("Beatriz", "Souza", "21999990000", usuario, SENHA);
        cadastroPage.preencherEndereco(rua, "50", "Centro", "Sala 2", "Rio de Janeiro", "RJ");
        salvarScreenshot("01-cenario1-formulario-preenchido.png");

        cadastroPage.clicarCadastrar();
        String alerta = cadastroPage.aceitarAlertaSeAparecer(Duration.ofSeconds(15));
        assertNotNull(alerta, "o cadastro deve mostrar um alerta de confirmação");
        assertTrue(alerta.contains("Cadastrado"), "alerta inesperado: " + alerta);
        cadastroPage.aguardarIrParaLogin();

        loginPage.realizarLogin(usuario, SENHA);
        String nome = aguardarTexto(By.id("nome2"));
        salvarScreenshot("02-cenario1-carrinho-apos-login.png");

        assertEquals("Beatriz Souza", nome, "o carrinho deve mostrar o nome cadastrado");
        // O endereço não é conferido aqui: ver o cenário 5 (defeito no getCliente)

        try (PreparedStatement stmt = jdbcConnection.prepareStatement(
                "SELECT senha, fg_ativo FROM tb_clientes WHERE usuario = ?")) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "o cliente deve estar gravado em tb_clientes");
                assertEquals(HASH_SENHA, rs.getString("senha"), "a senha deve estar em MD5 no banco");
                assertEquals(1, rs.getInt("fg_ativo"));
            }
        }
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("Cenário 2 (CT-03, passo 3 / DEF-05): cadastrar com campos vazios deve avisar e não enviar nada")
    void deveAvisarQuandoCamposObrigatoriosEstaoVazios() throws Exception {
        cadastroPage.abrir(baseUrl);

        cadastroPage.clicarCadastrar();
        String alerta = cadastroPage.aceitarAlertaSeAparecer(Duration.ofSeconds(5));
        salvarScreenshot("03-cenario2-campos-vazios.png");

        assertNotNull(alerta, "deveria aparecer o aviso de campos obrigatórios, mas nenhum alerta apareceu");
        assertTrue(alerta.contains("preencher todos os campos"),
                "deveria aparecer o aviso de campos obrigatórios, mas apareceu: " + alerta);
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("Cenário 3 (CT-03, passos 5 e 6): usuário já cadastrado não pode ser cadastrado de novo")
    void naoDeveAceitarUsuarioDuplicado() throws Exception {
        cadastrar("Beatriz", "Souza");
        cadastrar("Impostora", "Souza");
        salvarScreenshot("04-cenario3-apos-segundo-cadastro.png");

        assertEquals(1, idsDosClientes(usuario).size(), "o banco deve ter um único cliente com esse usuário");
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("Cenário 4 (CT-03, passo 7): nome com acento deve aparecer correto depois do login")
    void deveExibirNomeComAcentoCorretamente() throws Exception {
        cadastrar("José", "Conceição");

        loginPage.realizarLogin(usuario, SENHA);
        String nome = aguardarTexto(By.id("nome2"));
        salvarScreenshot("05-cenario4-acentos-no-carrinho.png");

        assertEquals("José Conceição", nome, "o nome deve ser gravado e exibido sem corromper os acentos");
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("Cenário 5 (achado da Entrega 2): o carrinho deve mostrar o endereço do próprio cliente")
    void deveExibirEnderecoCadastradoNoCarrinho() throws Exception {
        // O getCliente busca o endereço pelo id do cliente (pesquisarEnderecoPorID(cliente.getId_cliente())),
        // então o endereço só aparece quando os dois ids coincidem por acaso.
        cadastrar("Beatriz", "Souza");

        loginPage.realizarLogin(usuario, SENHA);
        aguardarTexto(By.id("nome2"));
        String endereco = driver.findElement(By.id("endereco2")).getText().trim();
        salvarScreenshot("06-cenario5-endereco-no-carrinho.png");

        assertTrue(endereco.contains(rua), "o carrinho deve mostrar o endereço cadastrado, mas mostrou: \"" + endereco + "\"");
    }

    // ---------- apoio ----------

    private void cadastrar(String nome, String sobrenome) {
        cadastroPage.abrir(baseUrl);
        cadastroPage.preencherUsuario(nome, sobrenome, "21999990000", usuario, SENHA);
        cadastroPage.preencherEndereco(rua, "50", "Centro", "Sala 2", "Rio de Janeiro", "RJ");
        cadastroPage.clicarCadastrar();
        cadastroPage.aceitarAlertaSeAparecer(Duration.ofSeconds(15));
        cadastroPage.aguardarIrParaLogin();
    }

    private String aguardarTexto(By local) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> !d.findElement(local).getText().trim().isEmpty());
        return driver.findElement(local).getText().trim();
    }

    private List<Integer> idsDosClientes(String usuarioCliente) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(
                "SELECT id_cliente FROM tb_clientes WHERE usuario = ?")) {
            stmt.setString(1, usuarioCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    private void executarDelete(String sql, String parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, parametro);
            stmt.executeUpdate();
        }
    }

    private void salvarScreenshot(String nomeArquivo) throws IOException {
        Path diretorio = Paths.get(screenshotsDir);
        Files.createDirectories(diretorio);
        File origem = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Files.copy(origem.toPath(), diretorio.resolve(nomeArquivo), StandardCopyOption.REPLACE_EXISTING);
    }
}
