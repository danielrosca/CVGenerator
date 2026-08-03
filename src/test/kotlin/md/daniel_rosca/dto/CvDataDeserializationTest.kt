package md.daniel_rosca.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CvDataDeserializationTest {
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private fun minimalYaml(employmentType: String = "full-time") =
        """
        personalInfo:
          name: "Test Person"
          location: "Test City"
          phone: "+1 555 0100"
          email: "test@example.com"
        professionalSummary: "A summary."
        technicalSkills:
          backend: "Java"
          frontend: "React"
          databases: "PostgreSQL"
          devopsAndCloud: "Docker"
          toolsAndMethodologies: "Agile"
        experience:
          - title: "Engineer"
            company: "Acme"
            location: "Remote"
            employmentType: "$employmentType"
            startDate: "2020-01-01"
            bullets:
              - "Did things"
        education:
          - degree: "BSc"
            institution: "Some University"
            location: "Remote"
            startDate: "2016-01-01"
            graduationYear: "2020-01-01"
        """.trimIndent()

    @Test
    fun `minimal yaml with optional fields omitted parses with nulls`() {
        val cv = mapper.readValue(minimalYaml(), CvData::class.java)

        assertNull(cv.personalInfo.links)
        assertNull(cv.languages)
        assertNull(cv.otherSections)
        assertEquals(emptyList<String>(), cv.technicalSkills.otherSkills)
    }

    @Test
    fun `full yaml with every optional field present parses correctly`() {
        val yaml =
            """
            personalInfo:
              name: "Test Person"
              location: "Test City"
              phone: "+1 555 0100"
              email: "test@example.com"
              links:
                - name: "LinkedIn"
                  url: "https://linkedin.com/in/test"
            professionalSummary: "A summary."
            technicalSkills:
              backend: "Java"
              frontend: "React"
              databases: "PostgreSQL"
              devopsAndCloud: "Docker"
              toolsAndMethodologies: "Agile"
              otherSkills:
                - "Public Speaking"
            experience:
              - title: "Engineer"
                company: "Acme"
                location: "Remote"
                employmentType: "full-time"
                startDate: "2020-01-01"
                endDate: "2022-01-01"
                bullets:
                  - "Did things"
            education:
              - degree: "BSc"
                institution: "Some University"
                location: "Remote"
                startDate: "2016-01-01"
                graduationYear: "2020-01-01"
            languages:
              - name: "English"
                level: "Native"
            otherSections:
              - nameOfSection: "Leadership"
                entries:
                  - title: "President"
                    companyOrOrganization: "Council"
                    startDate: "2020-01-01"
                    endDate: "2021-01-01"
                    description: "Led stuff"
            """.trimIndent()

        val cv = mapper.readValue(yaml, CvData::class.java)

        assertEquals(1, cv.personalInfo.links?.size)
        assertEquals("LinkedIn", cv.personalInfo.links?.get(0)?.name)
        assertEquals(listOf("Public Speaking"), cv.technicalSkills.otherSkills)
        assertEquals(1, cv.languages?.size)
        assertEquals(1, cv.otherSections?.size)
        assertEquals("President", cv.otherSections?.get(0)?.entries?.get(0)?.title)
    }

    @Test
    fun `job without an endDate is open-ended (null, rendered as Present downstream)`() {
        val cv = mapper.readValue(minimalYaml(), CvData::class.java)

        assertNull(cv.experience[0].endDate)
    }

    @ParameterizedTest
    @CsvSource(
        "full-time,FULL_TIME",
        "part-time,PART_TIME",
        "contract,CONTRACT",
        "internship,INTERNSHIP",
        "pet-project,PET_PROJECT",
    )
    fun `each valid employmentType string maps to the correct enum value`(
        input: String,
        expected: String,
    ) {
        val cv = mapper.readValue(minimalYaml(employmentType = input), CvData::class.java)

        assertEquals(EmploymentType.valueOf(expected), cv.experience[0].employmentType)
    }

    @Test
    fun `invalid employmentType string fails with a clear error message`() {
        val exception =
            assertThrows(Exception::class.java) {
                mapper.readValue(minimalYaml(employmentType = "senior-freelancer"), CvData::class.java)
            }

        assertTrue(exception.message?.contains("employmentType must be one of") == true)
    }

    @Test
    fun `a date in the wrong format fails clearly`() {
        val yaml = minimalYaml().replace("2020-01-01", "01/20/2020")

        assertThrows(InvalidFormatException::class.java) {
            mapper.readValue(yaml, CvData::class.java)
        }
    }
}
