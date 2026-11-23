package e2e.pages;

import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object da tela de login administrativo (/view/login/login_Funcionario.html).
 */
public class LoginFuncionarioPage {

    private static final String CAMINHO = "/view/login/login_Funcionario.html";
    private static final By LOGIN_INPUT = By.id("loginInput");
    private static final By SENHA_INPUT = By.id("senhaInput");
    private static final By BOTAO_ENTRAR = By.cssSelector("button.buttonSubmit");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginFuncionarioPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrir(String baseUrl) {
        driver.get(baseUrl + CAMINHO);
        aguardarTelaLoginVisivel();
    }

    public void aguardarTelaLoginVisivel() {
        wait.until(ExpectedConditions.urlContains(CAMINHO));
        wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_INPUT));
        wait.until(ExpectedConditions.visibilityOfElementLocated(BOTAO_ENTRAR));
    }

    public void realizarLogin(String usuario, String senha) {
        WebElement campoUsuario = wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_INPUT));
        campoUsuario.clear();
        campoUsuario.sendKeys(usuario);

        WebElement campoSenha = wait.until(ExpectedConditions.visibilityOfElementLocated(SENHA_INPUT));
        campoSenha.clear();
        campoSenha.sendKeys(senha);

        wait.until(ExpectedConditions.elementToBeClickable(BOTAO_ENTRAR)).click();
        wait.until(ExpectedConditions.urlContains("/view/painel/painel.html"));
    }

    /** Aceita todos os alertas em fila (a tela de estoque dispara um por requisição recusada) e devolve o primeiro. */
    public String aguardarEAceitarAlertas() {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String primeiro = alert.getText();
        alert.accept();
        WebDriverWait curta = new WebDriverWait(driver, Duration.ofSeconds(2));
        while (true) {
            try {
                curta.until(ExpectedConditions.alertIsPresent()).accept();
            } catch (org.openqa.selenium.TimeoutException semMaisAlertas) {
                return primeiro;
            }
        }
    }
}
