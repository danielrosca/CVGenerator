package md.daniel_rosca.pdf

import com.itextpdf.html2pdf.HtmlConverter
import java.io.File
import java.io.FileOutputStream

fun generatePdf(htmlContent: String, outputPath: String) {
    val outputFile = File(outputPath)
    outputFile.parentFile.mkdirs()

    FileOutputStream(outputFile).use { outputStream ->
        HtmlConverter.convertToPdf(htmlContent, outputStream)
    }
}