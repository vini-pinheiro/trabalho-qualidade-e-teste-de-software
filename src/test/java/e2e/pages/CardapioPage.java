package e2e.pages;

import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object da tela de cardápio (/view/menu/menu.html).
 */
public class CardapioPage {

    private static final By ABA_BEBIDAS = By.cssSelector("a[href='javascript:showBebidas()']");
    private static final By SCREEN_LANCHES = By.id("screen");
    private static final By SCREEN_BEBIDAS = By.id("screenBebidas");
    private static final By LINK_MEU_CARRINHO = By.cssSelector("#carrinho a");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public CardapioPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrir(String baseUrl) {
        driver.get(baseUrl + "/view/menu/menu.html");
        wait.until(ExpectedConditions.visibilityOfElementLocated(SCREEN_LANCHES));
    }

    public void aguardarLancheVisivel(String nomeLanche) {
        wait.until(ExpectedConditions.elementToBeClickable(botaoAdicionarItem("screen", nomeLanche)));
    }

    public String adicionarLancheAoCarrinho(String nomeLanche) {
        return clicarBotaoAdicionarECapturarAlerta(botaoAdicionarItem("screen", nomeLanche));
    }

    public void abrirAbaBebidas() {
        wait.until(ExpectedConditions.elementToBeClickable(ABA_BEBIDAS)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(SCREEN_BEBIDAS));
    }

    public String adicionarBebidaAoCarrinho(String nomeBebida) {
        return clicarBotaoAdicionarECapturarAlerta(botaoAdicionarItem("screenBebidas", nomeBebida));
    }

    public void irParaMeuCarrinho() {
        WebElement linkCarrinho = wait.until(ExpectedConditions.elementToBeClickable(LINK_MEU_CARRINHO));
        linkCarrinho.click();
        wait.until(ExpectedConditions.urlContains("/view/carrinho/carrinho.html"));
    }

    private By botaoAdicionarItem(String containerId, String nomeItem) {
        String xpath = "//div[@id='" + containerId + "']"
                + "//div[contains(@class,'divLanche')]"
                + "[.//h1[contains(@class,'tituloLanche') and normalize-space(.)='" + nomeItem + "']]"
                + "//button[normalize-space(.)='Adicionar ao Carrinho']";
        return By.xpath(xpath);
    }

    private String clicarBotaoAdicionarECapturarAlerta(By locator) {
        WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(locator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", botao);
        botao.click();
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String texto = alert.getText();
        alert.accept();
        return texto;
    }
}
