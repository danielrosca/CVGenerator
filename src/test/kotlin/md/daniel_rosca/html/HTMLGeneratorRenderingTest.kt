package md.daniel_rosca.html

import md.daniel_rosca.dto.CvData
import md.daniel_rosca.dto.EducationEntry
import md.daniel_rosca.dto.EmploymentType
import md.daniel_rosca.dto.JobExperience
import md.daniel_rosca.dto.Language
import md.daniel_rosca.dto.Link
import md.daniel_rosca.dto.OtherSection
import md.daniel_rosca.dto.OtherSectionEntry
import md.daniel_rosca.dto.PersonalInfo
import md.daniel_rosca.dto.TechnicalSkills
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Light smoke tests: assert expected fragments appear in the generated HTML, not a full
 * HTML-structure assertion (see TASK-008 in ../../../../../../TASKS.md).
 */
class HTMLGeneratorRenderingTest {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    private fun date(s: String) = dateFormat.parse(s)

    private fun minimalCvData(
        employmentType: EmploymentType = EmploymentType.FULL_TIME,
        links: List<Link>? = null,
        languages: List<Language>? = null,
        otherSections: List<OtherSection>? = null,
        otherSkills: List<String> = emptyList(),
    ) = CvData(
        personalInfo =
            PersonalInfo(
                name = "Jane Doe",
                location = "Remote",
                phone = "+1 555 0100",
                email = "jane@example.com",
                links = links,
            ),
        professionalSummary = "A concise professional summary.",
        technicalSkills =
            TechnicalSkills(
                backend = "Java",
                frontend = "React",
                databases = "PostgreSQL",
                devopsAndCloud = "Docker",
                toolsAndMethodologies = "Agile",
                otherSkills = otherSkills,
            ),
        experience =
            listOf(
                JobExperience(
                    title = "Senior Engineer",
                    company = "Acme Corp",
                    location = "Remote",
                    employmentType = employmentType,
                    startDate = date("2020-01-01"),
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
                    startDate = date("2016-01-01"),
                    graduationYear = date("2020-01-01"),
                ),
            ),
        languages = languages,
        otherSections = otherSections,
    )

    @Test
    fun `renders the candidate name and every section heading`() {
        val html = generateHtml(minimalCvData())

        assertTrue(html.contains("Jane Doe"))
        assertTrue(html.contains("Professional Summary"))
        assertTrue(html.contains("Technical Skills"))
        assertTrue(html.contains("Professional Experience"))
        assertTrue(html.contains("Education"))
    }

    @Test
    fun `formats the employment type badge from the enum name`() {
        val html = generateHtml(minimalCvData(employmentType = EmploymentType.PET_PROJECT))

        assertTrue(html.contains("Pet-project"))
    }

    @Test
    fun `renders links only when personalInfo links is non-null`() {
        val withoutLinks = generateHtml(minimalCvData(links = null))
        val withLinks =
            generateHtml(minimalCvData(links = listOf(Link(name = "GitHub", url = "https://github.com/janedoe"))))

        assertFalse(withoutLinks.contains("GitHub"))
        assertTrue(withLinks.contains("GitHub"))
    }

    @Test
    fun `renders a Languages section only when languages is present and non-empty`() {
        val withoutLanguages = generateHtml(minimalCvData(languages = null))
        val withLanguages = generateHtml(minimalCvData(languages = listOf(Language(name = "English", level = "Native"))))

        assertFalse(withoutLanguages.contains("Languages"))
        assertTrue(withLanguages.contains("Languages"))
        assertTrue(withLanguages.contains("English: Native"))
    }

    @Test
    fun `renders otherSections headings and entries only when present`() {
        val without = generateHtml(minimalCvData(otherSections = null))
        val with =
            generateHtml(
                minimalCvData(
                    otherSections =
                        listOf(
                            OtherSection(
                                nameOfSection = "Leadership Roles",
                                entries =
                                    listOf(
                                        OtherSectionEntry(
                                            companyOrOrganization = "Student Council",
                                            title = "President",
                                            description = "Led the council.",
                                            startDate = date("2020-01-01"),
                                            endDate = null,
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertFalse(without.contains("Leadership Roles"))
        assertTrue(with.contains("Leadership Roles"))
        assertTrue(with.contains("President"))
        assertTrue(with.contains("Present")) // open-ended entry endDate
    }

    @Test
    fun `renders an Other skills row only when otherSkills is non-empty`() {
        val withoutOtherSkills = generateHtml(minimalCvData(otherSkills = emptyList()))
        val withOtherSkills = generateHtml(minimalCvData(otherSkills = listOf("Public Speaking", "Technical Writing")))

        assertFalse(withoutOtherSkills.contains("Other:"))
        assertTrue(withOtherSkills.contains("Public Speaking, Technical Writing"))
    }
}
