package e2e;

import DAO.DaoUtil;
import e2e.pages.EstoquePage;
import e2e.pages.LoginFuncionarioPage;
import e2e.pages.PainelIngredientePage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de Sistema (E2E) do caso de teste CT-05 (Gestão de Insumos e Estoque).
 * Rodam só no perfil "e2e": mvn -Pe2e verify (Tomcat 8.5 + PostgreSQL 13 + Selenium Chrome).
 */
class EstoqueE2E {

    private static final String MD5_SENHA_PADRAO = "e10adc3949ba59abbe56e057f20f883e";
    private static final String SENHA_TEXTO_PADRAO = "123456";

    private String baseUrl;
    private String screenshotsDir;

    private Connection jdbcConnection;
    private WebDriver driver;

    private LoginFuncionarioPage loginPage;
    private PainelIngredientePage painelPage;
    private EstoquePage estoquePage;

    private String prefixo;
    private String usuarioFuncionario;
    private int idFuncionario;

    @BeforeEach
    void prepararAmbienteEMassa() throws Exception {
        baseUrl = System.getProperty("e2e.baseUrl", "http://e2e-app:8080");
        String seleniumUrl = System.getProperty("e2e.seleniumUrl", "http://selenium:4444/wd/hub");
        screenshotsDir = System.getProperty("e2e.screenshotsDir", "docs/entrega-2/selenium/felipe-paixao");

        jdbcConnection = new DaoUtil().conecta();

        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // Todo insumo criado pelo teste começa com este prefixo, para a limpeza não tocar em mais nada
        prefixo = "E2E-" + sufixo + " ";
        usuarioFuncionario = "e2ef_" + sufixo;
        idFuncionario = inserirFuncionario(usuarioFuncionario);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1440,900");

        driver = new RemoteWebDriver(new URL(seleniumUrl), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        loginPage = new LoginFuncionarioPage(driver);
        painelPage = new PainelIngredientePage(driver);
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
                    executar("DELETE FROM tb_ingredientes WHERE nm_ingrediente LIKE ?", prefixo + "%");
                    if (idFuncionario > 0) {
                        executar("DELETE FROM tb_tokens WHERE token LIKE ?", idFuncionario + "-%");
                        executar("DELETE FROM tb_funcionarios WHERE id_funcionario = ?", idFuncionario);
                    }
                }
            } finally {
                if (jdbcConnection != null) {
                    try {
                        if (!jdbcConnection.isClosed()) {
                            jdbcConnection.close();
                        }
                    } catch (SQLException ignored) {
                        // conexão de teste: nada a fazer se já caiu
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Cenário 1 (CT-05): funcionário cadastra um insumo no painel e ele aparece no estoque e no PostgreSQL")
    void deveCadastrarInsumoPeloPainelEExibirNoEstoque() throws Exception {
        String nome = prefixo + "Cheddar";

        loginPage.abrir(baseUrl);
        salvarScreenshot("01-cenario1-login-funcionario.png");
        loginPage.realizarLogin(usuarioFuncionario, SENHA_TEXTO_PADRAO);
        assertNotNull(driver.manage().getCookieNamed("tokenFuncionario"),
                "Cookie tokenFuncionario deve existir após o login");

        painelPage.abrirCadastroDeIngredientes();
        painelPage.preencher(nome, "queijo", "100", "0.80", "2.00", "Fatia de cheddar");
        salvarScreenshot("02-cenario1-formulario-cadastro.png");
        assertEquals("Ingrediente Salvo!", painelPage.salvarECapturarAlerta().trim());

        int id = validarInsumoNoBanco(nome, 100, 0.80, 2.00, "queijo");

        painelPage.irParaEstoque();
        estoquePage.aguardarIngredienteNaTabela(nome);
        List<String> colunas = estoquePage.colunasDoIngrediente(nome);
        assertEquals(String.valueOf(id), colunas.get(0));
        assertEquals("100", colunas.get(3));
        assertEquals("queijo", colunas.get(6));
        salvarScreenshot("03-cenario1-insumo-no-estoque.png");
    }

    @Test
    @DisplayName("Cenário 2 (CT-05): funcionário dá baixa no estoque e depois remove o insumo")
    void deveAtualizarQuantidadeERemoverInsumoPeloEstoque() throws Exception {
        String nome = prefixo + "Bacon";
        String sentinela = prefixo + "Sentinela";
        int id = inserirIngrediente(nome, 50);
        inserirIngrediente(sentinela, 10);

        loginPage.abrir(baseUrl);
        loginPage.realizarLogin(usuarioFuncionario, SENHA_TEXTO_PADRAO);
        painelPage.irParaEstoque();

        estoquePage.aguardarIngredienteNaTabela(nome);
        estoquePage.selecionarIngrediente(nome);
        assertEquals(String.valueOf(id), estoquePage.valorDoCampoId());
        assertEquals("50", estoquePage.valorDoCampoQuantidade());
        assertEquals("carne", estoquePage.valorDoCampoTipo());

        estoquePage.informarQuantidadeEValores("35", "1.20", "3.50");
        salvarScreenshot("04-cenario2-baixa-no-estoque.png");
        assertEquals("Ingrediente Alterado!", estoquePage.alterarECapturarAlerta().trim());
        validarInsumoNoBanco(nome, 35, 1.20, 3.50, "carne");

        estoquePage.aguardarIngredienteNaTabela(nome);
        assertEquals("35", estoquePage.colunasDoIngrediente(nome).get(3));
        salvarScreenshot("05-cenario2-estoque-atualizado.png");

        estoquePage.selecionarIngrediente(nome);
        estoquePage.removerECapturarAlerta();
        assertEquals(0, contarInsumosPorNome(nome), "Insumo removido não deve existir em tb_ingredientes");

        estoquePage.aguardarIngredienteNaTabela(sentinela);
        assertFalse(estoquePage.ingredienteEstaNaTabela(nome), "Insumo removido não deve aparecer no estoque");
        salvarScreenshot("06-cenario2-insumo-removido.png");
    }

    @Test
    @DisplayName("Cenário 3 (Negativo): acesso direto ao estoque sem login redireciona para o login administrativo")
    void deveBloquearAcessoDiretoAoEstoqueSemLogin() throws Exception {
        inserirIngrediente(prefixo + "Invisivel", 5);

        estoquePage.abrirSemAguardar(baseUrl);
        String alerta = loginPage.aguardarEAceitarAlertas();

        assertTrue(alerta.contains("Sua sessão expirou"), "Alerta inesperado sem login: " + alerta);
        loginPage.aguardarTelaLoginVisivel();
        assertTrue(driver.getCurrentUrl().contains("login_Funcionario.html?Action=TokenError"),
                "Deveria redirecionar para o login administrativo, mas foi: " + driver.getCurrentUrl());
        assertNull(driver.manage().getCookieNamed("tokenFuncionario"));
        salvarScreenshot("07-cenario3-bloqueio-sem-login.png");
    }

    @Test
    @DisplayName("Cenário 4 (caracterização do defeito #25): o painel aceita insumo com quantidade e valores negativos")
    void caracterizacaoPainelAceitaInsumoComValoresNegativos() throws Exception {
        String nome = prefixo + "Negativo";

        loginPage.abrir(baseUrl);
        loginPage.realizarLogin(usuarioFuncionario, SENHA_TEXTO_PADRAO);
        painelPage.abrirCadastroDeIngredientes();
        painelPage.preencher(nome, "molho", "-5", "-1.00", "-2.00", "Insumo com valores negativos");
        salvarScreenshot("08-cenario4-valores-negativos.png");

        // Comportamento atual (defeito #25): o sistema grava em vez de recusar
        assertEquals("Ingrediente Salvo!", painelPage.salvarECapturarAlerta().trim());
        validarInsumoNoBanco(nome, -5, -1.00, -2.00, "molho");
    }

    private int validarInsumoNoBanco(String nome, int quantidade, double valorCompra, double valorVenda,
            String tipo) throws SQLException {
        String sql = "SELECT id_ingrediente, quantidade, valor_compra, valor_venda, tipo, fg_ativo "
                + "FROM tb_ingredientes WHERE nm_ingrediente = ?";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Insumo deve existir em tb_ingredientes: " + nome);
                int id = rs.getInt("id_ingrediente");
                assertEquals(quantidade, rs.getInt("quantidade"));
                assertEquals(valorCompra, rs.getDouble("valor_compra"), 0.001);
                assertEquals(valorVenda, rs.getDouble("valor_venda"), 0.001);
                assertEquals(tipo, rs.getString("tipo"));
                assertEquals(1, rs.getInt("fg_ativo"));
                assertFalse(rs.next(), "Deve existir exatamente um insumo com esse nome");
                return id;
            }
        }
    }

    private int contarInsumosPorNome(String nome) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(
                "SELECT COUNT(*) FROM tb_ingredientes WHERE nm_ingrediente = ?")) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int inserirFuncionario(String usuario) throws SQLException {
        String sql = "INSERT INTO tb_funcionarios(nome, sobrenome, usuario, senha, cargo, salario, cad_por, fg_ativo) "
                + "VALUES(?,?,?,?,?,?,NULL,1) RETURNING id_funcionario";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, "Estoquista");
            stmt.setString(2, "E2E");
            stmt.setString(3, usuario);
            stmt.setString(4, MD5_SENHA_PADRAO);
            stmt.setString(5, "estoquista");
            stmt.setDouble(6, 2500.00);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Funcionário de teste deve gerar id_funcionario");
                return rs.getInt(1);
            }
        }
    }

    private int inserirIngrediente(String nome, int quantidade) throws SQLException {
        String sql = "INSERT INTO tb_ingredientes(nm_ingrediente, descricao, quantidade, valor_compra, valor_venda, "
                + "tipo, fg_ativo) VALUES(?,?,?,?,?,?,1) RETURNING id_ingrediente";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, "Insumo do teste E2E");
            stmt.setInt(3, quantidade);
            stmt.setDouble(4, 1.00);
            stmt.setDouble(5, 3.00);
            stmt.setString(6, "carne");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Insumo de teste deve gerar id_ingrediente");
                return rs.getInt(1);
            }
        }
    }

    private void executar(String sql, Object parametro) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setObject(1, parametro);
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
