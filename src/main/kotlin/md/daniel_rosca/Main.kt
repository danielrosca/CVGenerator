package md.daniel_rosca

import md.daniel_rosca.dto.CvData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import md.daniel_rosca.html.generateHtml
import md.daniel_rosca.pdf.generatePdf
import java.io.File
import kotlin.system.exitProcess

fun main() {
    println("CV Generator Started...")

    // --- 1. Configure Paths ---
    val inputYamlPath = "src/main/resources/ExampleCV.yaml"
    val outputHtmlPath = "build/ExampleCV.html"
    val outputPdfPath = "build/ExampleCV.pdf"

    // --- 2. Read and Parse YAML ---
    val cvData = try {
        val mapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val yamlFile = File(inputYamlPath)
        mapper.readValue(yamlFile, CvData::class.java)
    } catch (e: Exception) {
        System.err.println("Error reading or parsing YAML file: ${e.message}")
        exitProcess(1)
    }
    println("✅ Successfully parsed $inputYamlPath")

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
        System.err.println("Error generating PDF: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }

    println("\nCV Generation Complete!")
}