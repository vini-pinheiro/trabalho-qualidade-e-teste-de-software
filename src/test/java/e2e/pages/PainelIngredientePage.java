package e2e.pages;

import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object do painel administrativo (/view/painel/painel.html), aba "Cadastrar Ingredientes".
 */
public class PainelIngredientePage {

    private static final By BOTAO_CADASTRAR_INGREDIENTES =
            By.cssSelector("button[onclick='showCadIngredienteDiv()']");
    private static final By FORM = By.id("addIngrediente");
    private static final By CAMPO_NOME = By.cssSelector("#addIngrediente input[name='nome']");
    private static final By CAMPO_TIPO = By.cssSelector("#addIngrediente select[name='tipo']");
    private static final By CAMPO_QUANTIDADE = By.cssSelector("#addIngrediente input[name='quantidade']");
    private static final By CAMPO_VALOR_COMPRA = By.cssSelector("#addIngrediente input[name='ValorCompra']");
    private static final By CAMPO_VALOR_VENDA = By.cssSelector("#addIngrediente input[name='ValorVenda']");
    private static final By CAMPO_DESCRICAO = By.cssSelector("#addIngrediente textarea[name='descricao']");
    private static final By BOTAO_SALVAR = By.cssSelector("#addIngrediente input[name='salvar']");
    private static final By LINK_ESTOQUE = By.cssSelector("a[href='../estoque/estoque.html']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public PainelIngredientePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrir(String baseUrl) {
        driver.get(baseUrl + "/view/painel/painel.html");
        aguardarPainelVisivel();
    }

    public void aguardarPainelVisivel() {
        wait.until(ExpectedConditions.urlContains("/view/painel/painel.html"));
        wait.until(ExpectedConditions.elementToBeClickable(BOTAO_CADASTRAR_INGREDIENTES));
    }

    public void abrirCadastroDeIngredientes() {
        wait.until(ExpectedConditions.elementToBeClickable(BOTAO_CADASTRAR_INGREDIENTES)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(FORM));
    }

    public void preencher(String nome, String tipo, String quantidade, String valorCompra, String valorVenda,
            String descricao) {
        digitar(CAMPO_NOME, nome);
        new Select(wait.until(ExpectedConditions.visibilityOfElementLocated(CAMPO_TIPO))).selectByValue(tipo);
        digitar(CAMPO_QUANTIDADE, quantidade);
        digitar(CAMPO_VALOR_COMPRA, valorCompra);
        digitar(CAMPO_VALOR_VENDA, valorVenda);
        digitar(CAMPO_DESCRICAO, descricao);
    }

    public String salvarECapturarAlerta() {
        WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(BOTAO_SALVAR));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", botao);
        botao.click();
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String texto = alert.getText();
        alert.accept();
        // Depois do alerta a tela recarrega e volta ao início do painel
        aguardarPainelVisivel();
        return texto;
    }

    public void irParaEstoque() {
        wait.until(ExpectedConditions.elementToBeClickable(LINK_ESTOQUE)).click();
        wait.until(ExpectedConditions.urlContains("/view/estoque/estoque.html"));
    }

    private void digitar(By campo, String valor) {
        WebElement elemento = wait.until(ExpectedConditions.visibilityOfElementLocated(campo));
        // Os campos de valor vêm com "00.00": seleciona tudo antes de digitar
        elemento.clear();
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", elemento);
        elemento.sendKeys(valor);
    }
}
