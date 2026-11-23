package e2e.pages;

import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object da tela de cadastro de cliente (/view/cadastro/cadastro.html), usada no CT-03.
 */
public class CadastroPage {

    private static final By FORM_USUARIO = By.id("usuario");
    private static final By SELECT_ESTADO = By.id("UF");
    private static final By BOTAO_CADASTRAR = By.cssSelector(".containerButtonCad button.buttonSubmit");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public CadastroPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrir(String baseUrl) {
        driver.get(baseUrl + "/view/cadastro/cadastro.html");
        wait.until(ExpectedConditions.visibilityOfElementLocated(FORM_USUARIO));
        wait.until(ExpectedConditions.elementToBeClickable(BOTAO_CADASTRAR));
    }

    public void preencherUsuario(String nome, String sobrenome, String telefone, String usuario, String senha) {
        digitar("usuario", "nome", nome);
        digitar("usuario", "sobrenome", sobrenome);
        digitar("usuario", "telefone", telefone);
        digitar("usuario", "usuario", usuario);
        digitar("usuario", "senha", senha);
    }

    public void preencherEndereco(String rua, String numero, String bairro, String complemento, String cidade, String uf) {
        digitar("endereco", "rua", rua);
        digitar("endereco", "numero", numero);
        digitar("endereco", "bairro", bairro);
        digitar("endereco", "complemento", complemento);
        digitar("endereco", "cidade", cidade);
        new Select(driver.findElement(SELECT_ESTADO)).selectByValue(uf);
    }

    public void clicarCadastrar() {
        wait.until(ExpectedConditions.elementToBeClickable(BOTAO_CADASTRAR)).click();
    }

    /**
     * Espera um alerta do navegador, devolve o texto e fecha o alerta.
     * Devolve null se nenhum alerta aparecer no tempo dado.
     */
    public String aceitarAlertaSeAparecer(Duration limite) {
        try {
            Alert alerta = new WebDriverWait(driver, limite).until(ExpectedConditions.alertIsPresent());
            String texto = alerta.getText();
            alerta.accept();
            return texto;
        } catch (TimeoutException semAlerta) {
            return null;
        }
    }

    public void aguardarIrParaLogin() {
        wait.until(ExpectedConditions.urlContains("/view/login/login.html"));
    }

    private void digitar(String formulario, String campo, String valor) {
        WebElement elemento = driver.findElement(By.cssSelector("#" + formulario + " input[name='" + campo + "']"));
        elemento.clear();
        elemento.sendKeys(valor);
    }
}
