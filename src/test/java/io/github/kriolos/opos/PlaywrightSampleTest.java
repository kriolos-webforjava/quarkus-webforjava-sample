package io.github.kriolos.opos;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Sample Playwright Java test demonstrating browser automation with webforJ + Quarkus.
 *
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Launching Firefox (or Chromium) in headless/headed mode</li>
 *   <li>Handling webforJ DWC (Dynamic Web Client) asynchronous WebSocket/RMI initialization</li>
 *   <li>Validating page title, rendered components, and login modal elements</li>
 *   <li>Capturing full-page screenshots</li>
 *   <li>Proper lifecycle teardown to ensure webforJ CDI sessions clean up cleanly</li>
 * </ul>
 * </p>
 */
public class PlaywrightSampleTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    private static final String BASE_URL = System.getProperty("test.base.url", "http://localhost:8080");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("test.headless", "true"));
    private static final String BROWSER_TYPE = System.getProperty("test.browser", "firefox");

    @BeforeAll
    static void initPlaywright() {
        playwright = Playwright.create();

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(HEADLESS);

        if ("chromium".equalsIgnoreCase(BROWSER_TYPE)) {
            browser = playwright.chromium().launch(options);
        } else {
            // Defaults to Firefox
            browser = playwright.firefox().launch(options);
        }
    }

    @BeforeEach
    void setUpContext() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 800));
        page = context.newPage();
    }

    @AfterEach
    void tearDownContext() {
        if (context != null) {
            context.close();
        }
    }

    @AfterAll
    static void closePlaywright() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @DisplayName("Verify that webforJ root redirects to /login and renders the Authentication dialog")
    void testLoginPageRendersSuccessfully() {
        // 1. Navigate to the root endpoint
        page.navigate(BASE_URL + "/");

        // 2. Wait for webforJ DWC client and redirect to settle
        page.waitForURL("**/login", new Page.WaitForURLOptions().setTimeout(15000));

        // 3. Verify page title
        String title = page.title();
        assertNotNull(title, "Page title should not be null");
        assertTrue(title.contains("Login") || title.contains("KriolOS"),
                "Title should contain 'Login' or 'KriolOS', but was: " + title);

        // 4. Wait for Authentication dialog to appear in DOM
        Locator authHeading = page.locator("text=Authentication").first();
        authHeading.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertTrue(authHeading.isVisible(), "Authentication heading should be visible");

        // 5. Verify sign-in button exists
        Locator signInBtn = page.locator("text=Sign in").first();
        assertTrue(signInBtn.isVisible(), "Sign in button should be visible");

        // 6. Capture screenshot for verification
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/playwright-login-rendered.png")));
    }
}
