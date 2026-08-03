package md.daniel_rosca.pdf

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.Margin
import com.microsoft.playwright.options.Media
import com.microsoft.playwright.options.WaitUntilState
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors

/**
 * Launching a fresh headless Chromium process per PDF (the original behavior) cost real, measured
 * time on every call - not just JVM/app startup, but process-launch overhead paid again and again
 * for the life of a long-running server. Keeping one Browser alive for the process's lifetime and
 * only opening a fresh, isolated BrowserContext/Page per render removes that repeated cost.
 *
 * Playwright's Java driver isn't safe to call concurrently from multiple threads against the same
 * Playwright/Browser instance, so instead of sharing it directly across Tomcat's request threads,
 * a single dedicated thread owns the Browser and every render is funneled through it via submit().
 * Concurrent callers simply queue briefly - acceptable for a single-user personal tool (see
 * ../../../../../../CLAUDE.md) and far cheaper than a fresh Chromium launch per call regardless.
 */
private object BrowserPool {
    private val executor =
        Executors.newSingleThreadExecutor { r -> Thread(r, "playwright-browser").apply { isDaemon = true } }
    private var playwright: Playwright? = null
    private var browser: Browser? = null

    init {
        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
    }

    fun <T> withPage(block: (Page) -> T): T =
        executor
            .submit<T> {
                // isConnected() guards against a Chromium process that died mid-uptime (e.g. OOM) -
                // without it, every render after a crash would fail forever instead of relaunching.
                val activeBrowser = browser?.takeIf { it.isConnected() } ?: launchBrowser()
                activeBrowser.newContext().use { context ->
                    context.newPage().use { page -> block(page) }
                }
            }.get()

    private fun launchBrowser(): Browser {
        val pw = Playwright.create()
        playwright = pw
        val newBrowser = pw.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
        browser = newBrowser
        return newBrowser
    }

    private fun shutdown() {
        browser?.close()
        playwright?.close()
        executor.shutdown()
    }
}

fun generatePdf(
    htmlContent: String,
    outputPath: String,
) {
    // Ensure output directory exists
    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()

    // Write HTML to a temporary file so Chromium can load it via file:// URI
    val tempHtml: Path = Files.createTempFile("cvgen-", ".html")
    Files.writeString(tempHtml, htmlContent)
    val fileUri = tempHtml.toUri().toString() // e.g., file:///.../cvgen-XXXX.html

    BrowserPool.withPage { page ->
        // Emulate print media so @media print rules apply
        page.emulateMedia(Page.EmulateMediaOptions().setMedia(Media.PRINT))

        // Navigate and wait until network is idle (covers fonts/images if any)
        page.navigate(
            fileUri,
            Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE),
        )

        // Export to PDF with background graphics and A4 format
        val pdfOptions =
            Page.PdfOptions()
                .setPath(Paths.get(outputPath))
                .setPrintBackground(true)
                .setFormat("A4")
                .setPreferCSSPageSize(false)
                .setMargin(
                    Margin()
                        .setTop("0")
                        .setLeft("0")
                        .setRight("0")
                        .setBottom("0"),
                )

        page.pdf(pdfOptions)
    }

    // Clean up temp file
    try {
        Files.deleteIfExists(tempHtml)
    } catch (_: Exception) {
        // ignore
    }
}
