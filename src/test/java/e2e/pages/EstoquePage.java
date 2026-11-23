package e2e.pages;

import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object do painel de estoque do funcionário (/view/estoque/estoque.html).
 * A página consulta getIngredientes/getBebidas/getLanches ao carregar; sem token de funcionário
 * válido, o JavaScript redireciona para login_Funcionario.html?Action=TokenError, que exibe um alerta.
 */
public class EstoquePage {

    private static final By TABELA_INGREDIENTES = By.id("tabelaIngredientes");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public EstoquePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void abrirDireto(String baseUrl) {
        driver.get(baseUrl + "/view/estoque/estoque.html");
    }

    /**
     * Espera o desfecho do acesso: ou o sistema bloqueia (alerta de sessão), ou a tabela de
     * ingredientes é preenchida com o insumo informado.
     *
     * @return o texto do alerta de bloqueio, já aceito, ou null se o painel foi liberado
     */
    public String aguardarDesfechoDoAcesso(String ingredienteEsperado) {
        String desfecho = wait.until(d -> {
            try {
                return "ALERTA:" + d.switchTo().alert().getText();
            } catch (NoAlertPresentException semAlerta) {
                boolean liberado = !d.findElements(TABELA_INGREDIENTES).isEmpty()
                        && d.findElement(TABELA_INGREDIENTES).getText().contains(ingredienteEsperado);
                return liberado ? "LIBERADO" : null;
            }
        });
        if (desfecho.startsWith("ALERTA:")) {
            Alert alerta = driver.switchTo().alert();
            alerta.accept();
            return desfecho.substring("ALERTA:".length());
        }
        return null;
    }
}
