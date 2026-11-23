package e2e.pages;

import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object da área logada do cliente (/view/carrinho/carrinho.html): identificação e logout.
 */
public class AreaClientePage {

    private static final By NOME_CLIENTE = By.id("nome2");
    private static final By BOTAO_LOGOUT = By.cssSelector("button.buttonLogout");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AreaClientePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrirDireto(String baseUrl) {
        driver.get(baseUrl + "/view/carrinho/carrinho.html");
    }

    public void aguardarClienteIdentificado(String nomeEsperado) {
        wait.until(ExpectedConditions.urlContains("/view/carrinho/carrinho.html"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(NOME_CLIENTE, nomeEsperado));
    }

    /** Clica em Logout, aceita o alerta de confirmação e espera a volta para a home. */
    public String sairECapturarAlerta() {
        wait.until(ExpectedConditions.elementToBeClickable(BOTAO_LOGOUT)).click();
        Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
        String texto = alerta.getText();
        alerta.accept();
        wait.until(ExpectedConditions.urlContains("/view/home/home.html"));
        return texto;
    }
}
