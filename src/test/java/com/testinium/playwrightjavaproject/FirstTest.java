package com.testinium.playwrightjavaproject;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class FirstTest {

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private Path tracePath;
    private Page newTabPage;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();

        browser = playwright.webkit().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
        );

        // 1. COMMAND_PARAMETER
        String demoParam = System.getProperty("commandParameter");
        // 2. ENVIRONMENT_PARAMETER
        String denemeParam = System.getenv("environmentParameter");

        // 1. COMMAND_PARAMETER
        String demoParam2 = System.getProperty("commandParameter2");
        // 2. ENVIRONMENT_PARAMETER
        String denemeParam2 = System.getenv("environmentParameter2");

        System.out.println(">>> [COMMAND_PARAMETER] demo: " + demoParam);
        System.out.println(">>> [ENVIRONMENT_PARAMETER] deneme: " + denemeParam);

        System.out.println(">>> [COMMAND_PARAMETER] demo2: " + demoParam2);
        System.out.println(">>> [ENVIRONMENT_PARAMETER] deneme2: " + denemeParam2);

        String scenarioIDValue = System.getenv("SCENARIO_ID");
        String executionIDValue = System.getenv("EXECUTION_ID");
        String testResultIDValue = System.getenv("TEST_RESULT_ID");

        System.out.println(">>> [SCENARIO_ID] : " + scenarioIDValue);
        System.out.println(">>> [EXECUTION_ID] : " + executionIDValue);
        System.out.println(">>> [TEST_RESULT_ID] : " + testResultIDValue);

    }

    @BeforeEach
    void createContext(TestInfo testInfo) throws IOException {
        String testName = sanitize(testInfo.getDisplayName());

        Files.createDirectories(Path.of("trace"));
        tracePath = Path.of("trace", testName + ".zip");

        context = browser.newContext();

        // Page oluşturulmadan önce tracing başlatılmalı.
        context.tracing().start(
                new Tracing.StartOptions()
                        .setSnapshots(true)
                        .setSources(true)
        );

        page = context.newPage();
    }

    @Test
    public void shouldOpenGoogle() {
        step("Google sayfasını aç", () ->
                page.navigate("https://www.google.com"));

        step("Sayfa başlığını doğrula", () ->
                Assertions.assertTrue(page.title().contains("Google")));
    }

    @Test
    public void shouldCompleteLoginForm() {
        step("Login sayfasını hazırla", () -> page.setContent("""
                <html>
                  <head><title>Login</title></head>
                  <body>
                    <label>Kullanıcı adı <input id="username"></label>
                    <label>Şifre <input id="password" type="password"></label>
                    <button id="login">Giriş yap</button>
                    <p id="result"></p>
                    <script>
                      document.querySelector('#login').onclick = () => {
                        document.querySelector('#result').textContent = 'Giriş başarılı';
                      };
                    </script>
                  </body>
                </html>
                """));

        step("Kullanıcı bilgilerini gir", () -> {
            page.locator("#username").fill("testinium-user");
            page.locator("#password").fill("secret-password");
        });

        step("Login formunu gönder", () ->
                page.locator("#login").click());

        step("Login sonucunu doğrula", () ->
                Assertions.assertEquals("Giriş başarılı", page.locator("#result").textContent()));
    }

    @Test
    public void shouldAddTodoItem() {
        step("Todo sayfasını hazırla", () -> page.setContent("""
                <html>
                  <head><title>Todo List</title></head>
                  <body>
                    <input id="todo" placeholder="Yeni görev">
                    <button id="add">Ekle</button>
                    <ul id="list"></ul>
                    <script>
                      document.querySelector('#add').onclick = () => {
                        const input = document.querySelector('#todo');
                        const item = document.createElement('li');
                        item.textContent = input.value;
                        document.querySelector('#list').appendChild(item);
                        input.value = '';
                      };
                    </script>
                  </body>
                </html>
                """));

        step("Yeni görev bilgisini gir", () ->
                page.locator("#todo").fill("Playwright trace raporunu kontrol et"));

        step("Görevi listeye ekle", () ->
                page.locator("#add").click());

        step("Görevin eklendiğini doğrula", () ->
                Assertions.assertEquals(
                        "Playwright trace raporunu kontrol et",
                        page.locator("#list li").textContent()));
    }

    @Test
    public void shouldSelectTestPreferences() {
        step("Tercih formunu hazırla", () -> page.setContent("""
                <html>
                  <head><title>Test Preferences</title></head>
                  <body>
                    <label>Browser
                      <select id="browser">
                        <option value="chromium">Chromium</option>
                        <option value="firefox">Firefox</option>
                        <option value="webkit">WebKit</option>
                      </select>
                    </label>
                    <label><input id="headless" type="checkbox"> Headless çalıştır</label>
                  </body>
                </html>
                """));

        step("Firefox browser seç", () ->
                page.locator("#browser").selectOption("firefox"));

        step("Headless seçeneğini işaretle", () ->
                page.locator("#headless").check());

        step("Tercihleri doğrula", () -> {
            Assertions.assertEquals("firefox", page.locator("#browser").inputValue());
            Assertions.assertTrue(page.locator("#headless").isChecked());
        });
    }

    @Test
    public void shouldHandleJsConfirmDialog() {
        step("JS Alerts sayfasını aç", () ->
                page.navigate("https://the-internet.herokuapp.com/javascript_alerts"));

        step("Confirm dialogunu tetikle ve kabul et", () -> {
            page.onDialog(dialog -> dialog.accept());
            page.locator("text=Click for JS Confirm").click();
        });

        step("Confirm sonucunu doğrula", () ->
                Assertions.assertEquals("You clicked: Ok", page.locator("#result").textContent()));
    }

    @Test
    public void shouldOpenContentInNewTab() {
        step("Multiple Windows sayfasını aç", () ->
                page.navigate("https://the-internet.herokuapp.com/windows"));

        step("Yeni sekme açan linke tıkla", () -> {
            newTabPage = context.waitForPage(() ->
                    page.locator("text=Click Here").click());
            newTabPage.waitForLoadState();
        });

        step("Yeni sekmenin içeriğini doğrula", () ->
                Assertions.assertEquals("New Window", newTabPage.locator("h3").textContent()));
    }

    @Test
    public void shouldTypeInsideEditorIframe() {
        step("iFrame sayfasını aç", () ->
                page.navigate("https://the-internet.herokuapp.com/iframe"));

        step("Editör iframe içine yazı yaz", () ->
                page.frameLocator("#mce_0_ifr").locator("#tinymce")
                        .fill("Testinium Playwright suite"));

        step("Yazının iframe içinde göründüğünü doğrula", () ->
                Assertions.assertEquals(
                        "Testinium Playwright suite",
                        page.frameLocator("#mce_0_ifr").locator("#tinymce").textContent()));
    }

    @Test
    public void shouldWaitForDynamicallyLoadedContent() {
        step("Dynamic Loading sayfasını aç", () ->
                page.navigate("https://the-internet.herokuapp.com/dynamic_loading/2"));

        step("Yükleme sürecini başlat", () ->
                page.locator("#start button").click());

        step("Dinamik yüklenen metni doğrula", () -> {
            page.locator("#finish h4").waitFor();
            Assertions.assertEquals("Hello World!", page.locator("#finish h4").textContent());
        });
    }

    @Test
    public void shouldRevealProfileLinkOnHover() {
        step("Hovers sayfasını aç", () ->
                page.navigate("https://the-internet.herokuapp.com/hovers"));

        step("İlk kullanıcı avatarının üzerine gel", () ->
                page.locator(".figure").first().hover());

        step("Profil linkinin göründüğünü doğrula", () ->
                Assertions.assertEquals(
                        "View profile",
                        page.locator(".figure").first().locator("a").textContent()));
    }

    @Test
    public void shouldFailWithUnexpectedError() {
        // Bilerek başarısız olan bir senaryo: var olmayan bir elemente kısa
        // timeout ile tıklamayı deneyip Playwright'ın fırlattığı
        // TimeoutError'ı yakalamadan yukarı taşıyoruz. Bu, hata raporlama,
        // trace/screenshot alma ve CI pipeline'ının başarısız test akışını
        // doğrulamak için kullanılabilir.
        step("Hata senaryosu için sayfa aç", () ->
                page.navigate("https://the-internet.herokuapp.com/"));

        step("Var olmayan bir elemente tıklamayı dene", () ->
                page.locator("#this-element-does-not-exist")
                        .click(new Locator.ClickOptions().setTimeout(3000)));
    }

    @Test
    // @Disabled("Sadece uzun süreli koşum davranışını test etmek istendiğinde açın")
    public void shouldWaitTwentyMinutesWithoutAnyAction() {
        step("20 dakika boyunca herhangi bir işlem yapmadan bekle", () ->
                page.waitForTimeout(20 * 60 * 1000));
    }

    @Test
    @Disabled("Geçici olarak kapalı / refactor bekliyor")
    public void shouldOpenGoogleDisabled() {
        step("Google sayfasını aç", () ->
                page.navigate("https://www.google.com"));

        step("Sayfa başlığını doğrula", () ->
                Assertions.assertTrue(page.title().contains("Google")));
    }

    @Test
    public void RuntimeExcTest() {
        throw new RuntimeException();
    }

    private void step(String stepName, Runnable action) {
        context.tracing().group(stepName);

        try {
            action.run();
        } finally {
            context.tracing().groupEnd();
        }
    }

    @AfterEach
    void closeContext() {
        if (context == null) {
            return;
        }

        try {
            // Context kapanmadan önce trace kaydedilmeli.
            context.tracing().stop(
                    new Tracing.StopOptions()
                            .setPath(tracePath)
            );
        } finally {
            context.close();
        }
    }

    @AfterAll
    static void closeBrowser() {
        try {
            if (browser != null) {
                browser.close();
            }
        } finally {
            if (playwright != null) {
                playwright.close();
            }
        }
    }

    private static String sanitize(String value) {
        return value.toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}