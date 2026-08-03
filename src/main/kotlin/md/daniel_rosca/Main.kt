package md.daniel_rosca

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import md.daniel_rosca.dto.CvData
import md.daniel_rosca.web.CvGenerationService
import java.io.File
import kotlin.system.exitProcess

fun main() {
    println("CV Generator Started...")

    // --- 1. Configure Paths ---
    val inputYamlPath = "src/main/resources/ExampleCV.yaml"
    val outputHtmlPath = "build/ExampleCV.html"
    val outputPdfPath = "build/ExampleCV.pdf"

    // --- 2. Read and Parse YAML ---
    val cvData =
        try {
            val mapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
            val yamlFile = File(inputYamlPath)
            mapper.readValue(yamlFile, CvData::class.java)
        } catch (e: Exception) {
            System.err.println("Error reading or parsing YAML file: ${e.message}")
            exitProcess(1)
        }
    println("✅ Successfully parsed $inputYamlPath")

    // --- 3 & 4. Generate HTML then PDF (shared with the REST wrapper, see CvGenerationService) ---
    val htmlContent =
        try {
            CvGenerationService().generate(cvData, outputPdfPath)
        } catch (e: Exception) {
            System.err.println("Error generating CV: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    File(outputHtmlPath).apply {
        parentFile.mkdirs()
        writeText(htmlContent)
    }
    println("✅ Successfully generated HTML: $outputHtmlPath")
    println("✅ Successfully generated PDF: $outputPdfPath")

    println("\nCV Generation Complete!")
}
