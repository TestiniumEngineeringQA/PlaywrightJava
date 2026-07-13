package com.testinium.playwrightjavaproject;

import com.microsoft.playwright.*;
import com.testinium.playwright.screenshot.ScreenshotConfig;
import com.testinium.playwright.screenshot.ScreenshotSession;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class FirstTest {

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private ScreenshotSession screenshots;
    private Path tracePath;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();

        browser = playwright.webkit().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
        );
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
                        .setScreenshots(true)
                        .setSnapshots(true)
                        .setSources(true)
        );

        page = context.newPage();

        screenshots = ScreenshotSession.builder(page, testName)
                .config(
                        ScreenshotConfig.builder()
                                .outputDirectory(Path.of("screenshot"))
                                .fullPage(true)
                                .build()
                )
                .build();
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

    private void step(String stepName, Runnable action) {
        context.tracing().group(stepName);

        try {
            screenshots.step(stepName, action);
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
