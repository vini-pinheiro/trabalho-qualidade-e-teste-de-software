package e2e.pages;

import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object da tela de login do cliente (/view/login/login.html).
 */
public class LoginPage {

    private static final By LOGIN_INPUT = By.id("loginInput");
    private static final By SENHA_INPUT = By.id("senhaInput");
    private static final By BOTAO_ENTRAR = By.cssSelector("button.buttonSubmit");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrir(String baseUrl) {
        driver.get(baseUrl + "/view/login/login.html");
        aguardarTelaLoginVisivel();
    }

    public void aguardarTelaLoginVisivel() {
        wait.until(ExpectedConditions.urlContains("/view/login/login.html"));
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
        wait.until(ExpectedConditions.urlContains("/view/carrinho/carrinho.html"));
    }

    public String aguardarEAceitarAlerta() {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String texto = alert.getText();
        alert.accept();
        return texto;
    }
}
