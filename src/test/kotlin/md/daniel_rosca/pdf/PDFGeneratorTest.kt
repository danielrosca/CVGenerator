package md.daniel_rosca.pdf

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Requires Playwright's Chromium browser to be installed locally (`playwright install chromium`
 * via the Java driver, or it's downloaded automatically on first run - see the download log the
 * first time this runs in a fresh environment). Full visual/text-content PDF assertions are out
 * of scope here (see TASK-008 in ../../../../../../TASKS.md) - this only confirms the pipeline
 * produces a real, non-empty PDF file.
 */
class PDFGeneratorTest {
    @Test
    fun `generates a non-empty PDF file starting with the PDF magic bytes`() {
        val html =
            """
            <html><body><h1>Minimal Test Document</h1><p>Some content.</p></body></html>
            """.trimIndent()
        val outputPath = Files.createTempFile("pdfgen-test-", ".pdf")
        Files.deleteIfExists(outputPath) // generatePdf must create the file itself

        generatePdf(html, outputPath.toString())

        assertTrue(Files.exists(outputPath), "expected a PDF file to be created at $outputPath")
        val bytes = Files.readAllBytes(outputPath)
        assertTrue(bytes.isNotEmpty(), "expected the generated PDF to be non-empty")
        val magicBytes = String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)
        assertTrue(magicBytes == "%PDF-", "expected PDF magic bytes, got: $magicBytes")

        Files.deleteIfExists(outputPath)
    }
}
