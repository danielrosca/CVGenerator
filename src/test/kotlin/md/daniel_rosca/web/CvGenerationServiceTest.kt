package md.daniel_rosca.web

import md.daniel_rosca.dto.CvData
import md.daniel_rosca.dto.EducationEntry
import md.daniel_rosca.dto.EmploymentType
import md.daniel_rosca.dto.JobExperience
import md.daniel_rosca.dto.PersonalInfo
import md.daniel_rosca.dto.TechnicalSkills
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Locale

class CvGenerationServiceTest {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    private fun minimalCvData() =
        CvData(
            personalInfo =
                PersonalInfo(
                    name = "Jane Doe",
                    location = "Remote",
                    phone = "+1 555 0100",
                    email = "jane@example.com",
                    links = null,
                ),
            professionalSummary = "A concise professional summary.",
            technicalSkills =
                TechnicalSkills(
                    backend = "Java",
                    frontend = "React",
                    databases = "PostgreSQL",
                    devopsAndCloud = "Docker",
                    toolsAndMethodologies = "Agile",
                ),
            experience =
                listOf(
                    JobExperience(
                        title = "Senior Engineer",
                        company = "Acme Corp",
                        location = "Remote",
                        employmentType = EmploymentType.FULL_TIME,
                        startDate = dateFormat.parse("2020-01-01"),
                        endDate = null,
                        bullets = listOf("Did impactful things"),
                    ),
                ),
            education =
                listOf(
                    EducationEntry(
                        degree = "BSc Computer Science",
                        institution = "State University",
                        location = "Remote",
                        startDate = dateFormat.parse("2016-01-01"),
                        graduationYear = dateFormat.parse("2020-01-01"),
                    ),
                ),
        )

    @Test
    fun `generate produces a real non-empty PDF and returns the HTML it rendered`() {
        val service = CvGenerationService()
        val outputPath = Files.createTempFile("cvgen-service-test-", ".pdf")
        Files.deleteIfExists(outputPath)

        val html = service.generate(minimalCvData(), outputPath.toString())

        assertTrue(html.contains("Jane Doe"), "expected the returned HTML to contain the candidate's name")
        assertTrue(Files.exists(outputPath), "expected a PDF file to be created at $outputPath")
        val bytes = Files.readAllBytes(outputPath)
        assertTrue(bytes.isNotEmpty(), "expected the generated PDF to be non-empty")
        val magicBytes = String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)
        assertTrue(magicBytes == "%PDF-", "expected PDF magic bytes, got: $magicBytes")

        Files.deleteIfExists(outputPath)
    }
}
