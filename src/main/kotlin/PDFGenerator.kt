import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.File
import java.io.FileOutputStream

fun generatePdf(htmlContent: String, outputPath: String) {
    val outputFile = File(outputPath)
    outputFile.parentFile.mkdirs() // Ensure parent directory exists

    FileOutputStream(outputFile).use { os ->
        val renderer = ITextRenderer()
        renderer.setDocumentFromString(htmlContent)
        renderer.layout()
        renderer.createPDF(os)
    }
}