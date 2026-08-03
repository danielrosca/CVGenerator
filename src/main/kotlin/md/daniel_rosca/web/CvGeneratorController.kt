package md.daniel_rosca.web

import md.daniel_rosca.dto.CvData
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files

@RestController
class CvGeneratorController(private val cvGenerationService: CvGenerationService) {
    private val logger = LoggerFactory.getLogger(CvGeneratorController::class.java)

    /**
     * Malformed JSON in the request body never reaches this method - Spring's default handling
     * of a Jackson [com.fasterxml.jackson.databind.exc.MismatchedInputException] (or any other
     * `HttpMessageNotReadableException`) during `@RequestBody` binding already returns 400 with
     * no code needed here.
     */
    @PostMapping("/generate", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun generate(
        @RequestBody cvData: CvData,
    ): ResponseEntity<ByteArray> {
        val tempPdf = Files.createTempFile("cvgen-", ".pdf")
        try {
            cvGenerationService.generate(cvData, tempPdf.toString())
            val pdfBytes = Files.readAllBytes(tempPdf)
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cv.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes)
        } catch (e: Exception) {
            // Logged in full server-side; the caller only ever sees a generic 500, never
            // exception internals (stack traces, file paths, etc.).
            logger.error("Failed to generate PDF", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF generation failed")
        } finally {
            Files.deleteIfExists(tempPdf)
        }
    }
}
