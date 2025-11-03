package pt.zeromonos.garbagecollection;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// Diz ao Spring para arrancar a aplicação numa porta aleatória para este teste.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FrontendE2ETest {

    @LocalServerPort
    private int port;

    // As variáveis do Playwright. São estáticas para serem partilhadas por todos os testes na classe.
    static Playwright playwright;
    static Browser browser;

    // Contexto e Página do Browser
    BrowserContext context;
    Page page;

    // Este método é executado UMA VEZ antes de todos os testes nesta classe.
    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false)); // Headless a false para veres a magia
    }

    // Este método é executado UMA VEZ depois de todos os testes.
    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    // Este método é executado ANTES de CADA @Test.
    @org.junit.jupiter.api.BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    // Este método é executado DEPOIS de CADA @Test.
    @org.junit.jupiter.api.AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void whenCitizenFillsFormAndSubmits_thenSeesSuccess() {
        // 1. Arrange: Navegar para a página
        page.navigate("http://localhost:" + port + "/");

        // 2. Act: Preencher o formulário
        
        // Espera que a opção "Lisboa" apareça e seleciona-a.
        page.selectOption("#municipality", "Lisboa");

        // Preenche os outros campos
        page.getByLabel("Descrição dos Itens:").fill("Um frigorífico velho");
        page.getByLabel("Morada Completa:").fill("Rua do Teste E2E, 123");
        
        String futureDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE);
        page.getByLabel("Data de Recolha:").fill(futureDate);
        
        page.selectOption("#timeSlot", "MORNING");
        
        // Clica no botão de submissão
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agendar")).click();
        
        // 3. Assert: Verificar o resultado
        Locator resultDiv = page.locator("#result");
        
        // Verifica se a mensagem de sucesso e o token aparecem.
        assertThat(resultDiv).isVisible();
        assertThat(resultDiv).containsText("Agendamento realizado com sucesso!");
        assertThat(resultDiv).containsText("Guarde o seu código de consulta:");
    }
}