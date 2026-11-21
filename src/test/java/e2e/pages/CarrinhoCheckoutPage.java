package e2e.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object da tela de carrinho e checkout (/view/carrinho/carrinho.html).
 */
public class CarrinhoCheckoutPage {

    private static final By NOME_CLIENTE_ETAPA1 = By.id("nome2");
    private static final By CONTEUDO_ETAPA1 = By.id("conteudo1");
    private static final By ITENS_PEDIDO = By.cssSelector("#containerPedido .ItemPedido");
    private static final By NOMES_ITENS_PEDIDO = By.cssSelector("#containerPedido .ItemPedido .legendaNome");
    private static final By VALOR_TOTAL_ETAPA1 = By.id("valor1");
    private static final By BOTAO_FORMA_PAGAMENTO = By.cssSelector("input[name='Metodopagamento']");

    private static final By CONTEUDO_ETAPA2 = By.id("conteudo2");
    private static final By TEXTO_RESUMO = By.id("textResumo");
    private static final By VALOR_TOTAL_ETAPA2 = By.id("valor2");
    private static final By OPCAO_ENTREGA = By.cssSelector("input[name='Entrega']");
    private static final By BOTAO_FINALIZAR = By.cssSelector("#conteudo2 button.buttonSubmit");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public CarrinhoCheckoutPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrirDireto(String baseUrl) {
        driver.get(baseUrl + "/view/carrinho/carrinho.html");
    }

    public void aguardarCarregamentoComItens(String nomeClienteEsperado, int quantidadeItensEsperada) {
        wait.until(ExpectedConditions.urlContains("/view/carrinho/carrinho.html"));
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(NOME_CLIENTE_ETAPA1, nomeClienteEsperado));
        wait.until(d -> d.findElements(ITENS_PEDIDO).size() >= quantidadeItensEsperada);
    }

    public List<String> obterTextoItens() {
        List<WebElement> elementos = driver.findElements(NOMES_ITENS_PEDIDO);
        List<String> nomes = new ArrayList<>();
        for (WebElement elemento : elementos) {
            nomes.add(elemento.getText().trim());
        }
        return nomes;
    }

    public String obterValorTotalEtapa1() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(VALOR_TOTAL_ETAPA1)).getText().trim();
    }

    public void avancarParaFormaDePagamento() {
        WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(BOTAO_FORMA_PAGAMENTO));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", botao);
        botao.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(CONTEUDO_ETAPA2));
        wait.until(d -> !d.findElement(TEXTO_RESUMO).getText().trim().isEmpty());
    }

    public String obterTextoResumo() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(TEXTO_RESUMO)).getText();
    }

    public String obterValorTotalEtapa2() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(VALOR_TOTAL_ETAPA2)).getText().trim();
    }

    public void selecionarPagamentoEEntrega(String tipoPagamento) {
        By radioPagamento = By.cssSelector("input[name='Pagamento'][value='" + tipoPagamento + "']");
        wait.until(ExpectedConditions.elementToBeClickable(radioPagamento)).click();
        wait.until(ExpectedConditions.elementToBeClickable(OPCAO_ENTREGA)).click();
    }

    public String clicarFinalizarECapturarAlerta() {
        WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(BOTAO_FINALIZAR));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", botao);
        botao.click();
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String texto = alert.getText();
        alert.accept();
        return texto;
    }

    public void aguardarCarrinhoLimpoAposFinalizacao() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(CONTEUDO_ETAPA1));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(VALOR_TOTAL_ETAPA1, "0.00"));
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }
}
