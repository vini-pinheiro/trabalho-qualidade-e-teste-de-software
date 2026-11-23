package e2e.pages;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object da tela de estoque (/view/estoque/estoque.html), tabela e formulário de ingredientes.
 */
public class EstoquePage {

    private static final String CAMINHO = "/view/estoque/estoque.html";
    private static final By FORM_EDICAO = By.id("editarIngredientes");
    private static final By CAMPO_ID = By.id("ingredientesID");
    private static final By CAMPO_NOME = By.id("ingredientesNome");
    private static final By CAMPO_QUANTIDADE = By.id("ingredientesQuantidade");
    private static final By CAMPO_VALOR_COMPRA = By.id("ingredientesPrecoCompra");
    private static final By CAMPO_VALOR_VENDA = By.id("ingredientesPrecoVenda");
    private static final By CAMPO_TIPO = By.id("ingredientesTipo");
    private static final By BOTAO_ALTERAR = By.cssSelector("#editarIngredientes input[value='Alterar']");
    private static final By BOTAO_REMOVER = By.cssSelector("#editarIngredientes input[value='Remover']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public EstoquePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrir(String baseUrl) {
        driver.get(baseUrl + CAMINHO);
        wait.until(ExpectedConditions.urlContains(CAMINHO));
    }

    public void abrirSemAguardar(String baseUrl) {
        driver.get(baseUrl + CAMINHO);
    }

    private By linhaDoIngrediente(String nome) {
        return By.xpath("//table[@id='tabelaIngredientes']//tr[td[2][normalize-space()='" + nome + "']]");
    }

    public void aguardarIngredienteNaTabela(String nome) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(linhaDoIngrediente(nome)));
    }

    /**
     * Use depois de aguardarIngredienteNaTabela de outro insumo, para saber que a tabela já carregou.
     */
    public boolean ingredienteEstaNaTabela(String nome) {
        return !driver.findElements(linhaDoIngrediente(nome)).isEmpty();
    }

    public List<String> colunasDoIngrediente(String nome) {
        WebElement linha = wait.until(ExpectedConditions.visibilityOfElementLocated(linhaDoIngrediente(nome)));
        List<WebElement> celulas = linha.findElements(By.tagName("td"));
        java.util.ArrayList<String> textos = new java.util.ArrayList<>();
        for (WebElement celula : celulas) {
            textos.add(celula.getText().trim());
        }
        return textos;
    }

    public void selecionarIngrediente(String nome) {
        WebElement linha = wait.until(ExpectedConditions.elementToBeClickable(linhaDoIngrediente(nome)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", linha);
        linha.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(FORM_EDICAO));
        wait.until(ExpectedConditions.attributeToBe(CAMPO_NOME, "value", nome));
    }

    public String valorDoCampoId() {
        return driver.findElement(CAMPO_ID).getAttribute("value");
    }

    public String valorDoCampoQuantidade() {
        return driver.findElement(CAMPO_QUANTIDADE).getAttribute("value");
    }

    public String valorDoCampoTipo() {
        return driver.findElement(CAMPO_TIPO).getAttribute("value");
    }

    public void informarQuantidadeEValores(String quantidade, String valorCompra, String valorVenda) {
        substituir(CAMPO_QUANTIDADE, quantidade);
        substituir(CAMPO_VALOR_COMPRA, valorCompra);
        substituir(CAMPO_VALOR_VENDA, valorVenda);
    }

    public String alterarECapturarAlerta() {
        return clicarECapturarAlerta(BOTAO_ALTERAR);
    }

    public String removerECapturarAlerta() {
        return clicarECapturarAlerta(BOTAO_REMOVER);
    }

    private String clicarECapturarAlerta(By botao) {
        WebElement elemento = wait.until(ExpectedConditions.elementToBeClickable(botao));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", elemento);
        elemento.click();
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String texto = alert.getText();
        alert.accept();
        // Depois do alerta a tela recarrega a tabela
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        return texto;
    }

    private void substituir(By campo, String valor) {
        WebElement elemento = wait.until(ExpectedConditions.visibilityOfElementLocated(campo));
        elemento.clear();
        elemento.sendKeys(valor);
    }
}
