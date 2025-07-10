import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

fun main() {
    println("CV Generator Started...")

    // --- 1. Configure Paths ---
    val inputYamlPath = "src/main/resources/cv.yaml"
    val outputHtmlPath = "build/cv.html"
    val outputPdfPath = "build/cv.pdf"

    // --- 2. Read and Parse YAML ---
    val cvData = try {
        val mapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val yamlFile = File(inputYamlPath)
        mapper.readValue(yamlFile, CvData::class.java)
    } catch (e: Exception) {
        println("Error reading or parsing YAML file: ${e.message}")
        return
    }
    println("✅ Successfully parsed cv.yaml.")

    // --- 3. Generate HTML ---
    val htmlContent = generateHtml(cvData)
    File(outputHtmlPath).apply {
        parentFile.mkdirs()
        writeText(htmlContent)
    }
    println("✅ Successfully generated HTML: $outputHtmlPath")

    // --- 4. Generate PDF from HTML ---
    try {
        generatePdf(htmlContent, outputPdfPath)
        println("✅ Successfully generated PDF: $outputPdfPath")
    } catch (e: Exception) {
        println("Error generating PDF: ${e.message}")
        e.printStackTrace()
    }

    println("\nCV Generation Complete!")
}