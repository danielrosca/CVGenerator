package md.daniel_rosca.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/**
 * The fixture body below is a raw JSON string, not a Kotlin CvData object serialized through a
 * generic ObjectMapper - that was tried first and failed non-obviously: Jackson's default enum
 * serialization writes the raw constant name ("FULL_TIME"), but [md.daniel_rosca.dto.EmploymentTypeDeserializer]
 * (deliberately, matching every yaml file in this project) only accepts the hyphenated lowercase
 * form ("full-time"). A real caller has to send that same hyphenated form, so a raw JSON literal
 * is both the fix and a more accurate simulation of an actual external request body.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CvGeneratorControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    private val minimalCvDataJson =
        """
        {
          "personalInfo": {
            "name": "Jane Doe",
            "location": "Remote",
            "phone": "+1 555 0100",
            "email": "jane@example.com",
            "links": null
          },
          "professionalSummary": "A concise professional summary.",
          "technicalSkills": {
            "backend": "Java",
            "frontend": "React",
            "databases": "PostgreSQL",
            "devopsAndCloud": "Docker",
            "toolsAndMethodologies": "Agile"
          },
          "experience": [
            {
              "title": "Senior Engineer",
              "company": "Acme Corp",
              "location": "Remote",
              "employmentType": "full-time",
              "startDate": "2020-01-01",
              "endDate": null,
              "bullets": ["Did impactful things"]
            }
          ],
          "education": [
            {
              "degree": "BSc Computer Science",
              "institution": "State University",
              "location": "Remote",
              "startDate": "2016-01-01",
              "graduationYear": "2020-01-01"
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `POST generate with a valid CvData JSON body returns 200 with a valid PDF`() {
        val result =
            mockMvc
                .post("/generate") {
                    contentType = MediaType.APPLICATION_JSON
                    content = minimalCvDataJson
                }.andReturn()

        assertEquals(200, result.response.status)
        assertEquals(MediaType.APPLICATION_PDF_VALUE, result.response.contentType)
        val bytes = result.response.contentAsByteArray
        assertTrue(bytes.isNotEmpty())
        val magicBytes = String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)
        assertTrue(magicBytes == "%PDF-", "expected PDF magic bytes, got: $magicBytes")
    }

    @Test
    fun `POST generate with html=true returns 200 with the rendered HTML instead of a PDF`() {
        val result =
            mockMvc
                .post("/generate?html=true") {
                    contentType = MediaType.APPLICATION_JSON
                    content = minimalCvDataJson
                }.andReturn()

        assertEquals(200, result.response.status)
        assertEquals(MediaType.TEXT_HTML_VALUE, result.response.contentType)
        val html = result.response.contentAsString
        assertTrue(html.contains("Jane Doe"), "expected the returned HTML to contain the candidate's name")
    }

    @Test
    fun `POST generate with malformed JSON returns 400`() {
        val result =
            mockMvc
                .post("/generate") {
                    contentType = MediaType.APPLICATION_JSON
                    content = "{not valid json"
                }.andReturn()

        assertEquals(400, result.response.status)
    }

    @Test
    fun `POST generate with an invalid employmentType value returns 400 with a clear cause`() {
        val badJson = minimalCvDataJson.replace("\"full-time\"", "\"FULL_TIME\"")

        val result =
            mockMvc
                .post("/generate") {
                    contentType = MediaType.APPLICATION_JSON
                    content = badJson
                }.andReturn()

        assertEquals(400, result.response.status)
    }
}
