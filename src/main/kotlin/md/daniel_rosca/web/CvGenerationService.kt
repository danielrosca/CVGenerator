package md.daniel_rosca.web

import md.daniel_rosca.dto.CvData
import md.daniel_rosca.html.generateHtml
import md.daniel_rosca.pdf.generatePdf
import org.springframework.stereotype.Service

/**
 * Shared generateHtml -> generatePdf orchestration, called from both Main.kt (local ad-hoc CLI
 * use) and [CvGeneratorController] (the REST wrapper) - see TASK-011 in
 * ../../../../../../TASKS.md, which explicitly asks for this extraction so the two callers don't
 * duplicate it. Not a `@Service`-only concern: also usable directly with `CvGenerationService()`
 * outside a Spring context, since Main.kt has no Spring container of its own.
 */
@Service
class CvGenerationService {
    /**
     * Renders [cvData] to HTML, then to a PDF at [outputPdfPath]. Returns the generated HTML so
     * callers that also want to persist it (Main.kt) don't need to render it a second time.
     */
    fun generate(
        cvData: CvData,
        outputPdfPath: String,
    ): String {
        val htmlContent = generateHtml(cvData)
        generatePdf(htmlContent, outputPdfPath)
        return htmlContent
    }
}
