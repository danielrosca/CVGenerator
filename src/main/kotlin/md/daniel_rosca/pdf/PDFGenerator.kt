package md.daniel_rosca.pdf

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.Media
import com.microsoft.playwright.options.WaitUntilState
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun generatePdf(htmlContent: String, outputPath: String) {
    // Ensure output directory exists
    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()

    // Write HTML to a temporary file so Chromium can load it via file:// URI
    val tempHtml: Path = Files.createTempFile("cvgen-", ".html")
    Files.writeString(tempHtml, htmlContent)
    val fileUri = tempHtml.toUri().toString() // e.g., file:///.../cvgen-XXXX.html

    Playwright.create().use { pw ->
        val browser = pw.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
        browser.newContext().use { context ->
            context.newPage().use { page ->
                // Emulate print media so @media print rules apply
                page.emulateMedia(Page.EmulateMediaOptions().setMedia(Media.PRINT))

                // Navigate and wait until network is idle (covers fonts/images if any)
                page.navigate(
                    fileUri,
                    Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE)
                )

                // Export to PDF with background graphics and A4 format
                val pdfOptions = Page.PdfOptions()
                    .setPath(Paths.get(outputPath))
                    .setPrintBackground(true)
                    .setFormat("A4")
                    .setPreferCSSPageSize(true)

                page.pdf(pdfOptions)
            }
        }
    }

    // Clean up temp file
    try {
        Files.deleteIfExists(tempHtml)
    } catch (_: Exception) {
        // ignore
    }
}
