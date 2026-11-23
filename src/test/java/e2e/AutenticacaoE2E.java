package e2e;

import DAO.DaoUtil;
import e2e.pages.AreaClientePage;
import e2e.pages.EstoquePage;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de Sistema (E2E) do caso de teste CT-01 (Autenticação e encerramento de sessão),
 * mais um requisito não funcional de segurança (controle de acesso por perfil).
 *
 * Executar: mvn -Pe2e verify (cenários verdes) e mvn -Pe2e,defeitos verify (defeito conhecido).
 */
class AutenticacaoE2E {

    private static final String MD5_SENHA_PADRAO = "e10adc3949ba59abbe56e057f20f883e";
    private static final String SENHA_TEXTO_PADRAO = "123456";
    private static final String NOME_CLIENTE = "Cliente E2E";

    private String baseUrl;
    private String seleniumUrl;
    private String screenshotsDir;

    private Connection jdbcConnection;
    private WebDriver driver;

    private LoginPage loginPage;
    private AreaClientePage areaCliente;
    private EstoquePage estoquePage;

    private String sufixo;
    private String usuarioCliente;
    private int idEndereco;
    private int idCliente;
    private int idIngrediente;

    @BeforeEach
    void prepararAmbienteEMassa() throws Exception {
        baseUrl = System.getProperty("e2e.baseUrl", "http://e2e-app:8080");
        seleniumUrl = System.getProperty("e2e.seleniumUrl", "http://selenium:4444/wd/hub");
        screenshotsDir = System.getProperty("e2e.screenshotsDir", "docs/entrega-2/selenium/yuri-mascarenhas");

        jdbcConnection = new DaoUtil().conecta();

        sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        usuarioCliente = "e2e_auth_" + sufixo;
        idEndereco = inserirEndereco();
        idCliente = inserirCliente(usuarioCliente, idEndereco);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1440,900");

        driver = new RemoteWebDriver(new URL(seleniumUrl), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        loginPage = new LoginPage(driver);
        areaCliente = new AreaClientePage(driver);
        estoquePage = new EstoquePage(driver);
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
                    executarDelete("DELETE FROM tb_tokens WHERE token LIKE ?", idCliente + "-%");
                    executarDelete("DELETE FROM tb_ingredientes WHERE id_ingrediente = ?", idIngrediente);
                    executarDelete("DELETE FROM tb_clientes WHERE id_cliente = ?", idCliente);
                    executarDelete("DELETE FROM tb_enderecos WHERE id_endereco = ?", idEndereco);
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
    @DisplayName("Cenário 1 (CT-01): login válido grava o token no banco, cria o cookie e identifica o cliente")
    void deveAutenticarClienteComCredenciaisValidas() throws Exception {
        loginPage.abrir(baseUrl);
        salvarScreenshot("01-login-tela.png");

        loginPage.realizarLogin(usuarioCliente, SENHA_TEXTO_PADRAO);
        areaCliente.aguardarClienteIdentificado(NOME_CLIENTE);
        salvarScreenshot("02-login-valido-area-cliente.png");

        Cookie token = driver.manage().getCookieNamed("token");
        assertNotNull(token, "Cookie token deve existir após o login");
        assertTrue(token.getValue().startsWith(idCliente + "-"), "Token deve começar pelo id do cliente: " + token.getValue());
        assertEquals(1, contarTokens(token.getValue()), "Token do cookie deve estar gravado em tb_tokens");
    }

    @Test
    @DisplayName("Cenário 2 (CT-01): senha incorreta exibe erro, não cria cookie e não grava token")
    void deveRecusarLoginComSenhaIncorreta() throws Exception {
        loginPage.abrir(baseUrl);
        loginPage.tentarLogin(usuarioCliente, "senha-errada");

        String alerta = loginPage.aguardarEAceitarAlerta();
        assertTrue(alerta.startsWith("Erro ao Logar!"), "Alerta inesperado: " + alerta);
        salvarScreenshot("03-login-senha-incorreta.png");

        assertTrue(driver.getCurrentUrl().contains("/view/login/login.html"), "Deveria continuar na tela de login");
        assertNull(driver.manage().getCookieNamed("token"));
        assertEquals(0, contarTokensDoCliente());
    }

    @Test
    @DisplayName("Cenário 3 (CT-01): painel do funcionário aberto sem login redireciona para o login de funcionário")
    void deveBloquearPainelDoFuncionarioSemLogin() throws Exception {
        idIngrediente = inserirIngrediente("InsumoE2E-" + sufixo);

        estoquePage.abrirDireto(baseUrl);
        String alerta = estoquePage.aguardarDesfechoDoAcesso("InsumoE2E-" + sufixo);

        assertNotNull(alerta, "Painel de estoque não deveria abrir sem login");
        assertTrue(alerta.contains("desconectado do Painel"), "Alerta inesperado: " + alerta);
        assertTrue(driver.getCurrentUrl().contains("login_Funcionario.html?Action=TokenError"), driver.getCurrentUrl());
        salvarScreenshot("04-painel-sem-login-bloqueado.png");
    }

    @Test
    @DisplayName("Cenário 4 (CT-01): logout apaga o token do banco e o cookie antigo reapresentado é recusado")
    void deveEncerrarSessaoNoLogoutERecusarCookieAntigo() throws Exception {
        loginPage.abrir(baseUrl);
        loginPage.realizarLogin(usuarioCliente, SENHA_TEXTO_PADRAO);
        areaCliente.aguardarClienteIdentificado(NOME_CLIENTE);
        String tokenAntigo = driver.manage().getCookieNamed("token").getValue();

        String alerta = areaCliente.sairECapturarAlerta();
        assertEquals("Deslogado", alerta.trim());
        salvarScreenshot("05-logout-home.png");

        assertEquals(0, contarTokens(tokenAntigo), "Logout deve apagar o token de tb_tokens");

        // Ataque de repetição: alguém que copiou o cookie tenta reutilizá-lo depois do logout
        driver.manage().addCookie(new Cookie("token", tokenAntigo, "/"));
        areaCliente.abrirDireto(baseUrl);
        String alertaReuso = loginPage.aguardarEAceitarAlerta();
        assertTrue(alertaReuso.contains("Sua sessão expirou"), "Alerta inesperado: " + alertaReuso);
        loginPage.aguardarTelaLoginVisivel();
        salvarScreenshot("06-cookie-antigo-recusado.png");
    }

    @Test
    @Tag("defeito-conhecido")
    @DisplayName("RNF Segurança (DEFEITO): cliente que copia o próprio token para o cookie tokenFuncionario não pode abrir o painel")
    void naoDeveLiberarPainelDoFuncionarioParaCliente() throws Exception {
        String insumo = "InsumoE2E-" + sufixo;
        idIngrediente = inserirIngrediente(insumo);

        loginPage.abrir(baseUrl);
        loginPage.realizarLogin(usuarioCliente, SENHA_TEXTO_PADRAO);
        areaCliente.aguardarClienteIdentificado(NOME_CLIENTE);
        String tokenDoCliente = driver.manage().getCookieNamed("token").getValue();

        driver.manage().addCookie(new Cookie("tokenFuncionario", tokenDoCliente, "/"));
        estoquePage.abrirDireto(baseUrl);
        String alerta = estoquePage.aguardarDesfechoDoAcesso(insumo);
        salvarScreenshot("07-rnf-seguranca-cliente-no-painel.png");

        assertNotNull(alerta, "Cliente acessou o painel de estoque do funcionário usando o próprio token");
    }

    private void salvarScreenshot(String nomeArquivo) throws IOException {
        Path diretorio = Paths.get(screenshotsDir);
        Files.createDirectories(diretorio);
        File origem = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Files.copy(origem.toPath(), diretorio.resolve(nomeArquivo), StandardCopyOption.REPLACE_EXISTING);
    }

    private int contarTokens(String token) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement("SELECT COUNT(*) FROM tb_tokens WHERE token = ?")) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int contarTokensDoCliente() throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement("SELECT COUNT(*) FROM tb_tokens WHERE token LIKE ?")) {
            stmt.setString(1, idCliente + "-%");
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
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
            stmt.setString(5, "Niteroi");
            stmt.setString(6, "RJ");
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
            stmt.setString(3, "21999990000");
            stmt.setString(4, usuario);
            stmt.setString(5, MD5_SENHA_PADRAO);
            stmt.setInt(6, idEnd);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private int inserirIngrediente(String nome) throws SQLException {
        String sql = "INSERT INTO tb_ingredientes(nm_ingrediente, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo) "
                + "VALUES(?,?,?,?,?,?,1) RETURNING id_ingrediente";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, "Insumo teste E2E");
            stmt.setInt(3, 10);
            stmt.setDouble(4, 1.00);
            stmt.setDouble(5, 2.00);
            stmt.setString(6, "Pão");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }

    private void executarDelete(String sql, Object parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setObject(1, parametro);
            stmt.executeUpdate();
        }
    }
}
