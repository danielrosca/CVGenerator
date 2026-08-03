package md.daniel_rosca.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.util.Date

// The root object for the entire CV data
data class CvData(
    val personalInfo: PersonalInfo,
    val professionalSummary: String,
    val technicalSkills: TechnicalSkills,
    val experience: List<JobExperience>,
    val education: List<EducationEntry>,
    @JsonProperty("languages")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val languages: List<Language>? = null,
    @JsonProperty("otherSections")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val otherSections: List<OtherSection>? = null,
)

// Language section

data class Language(
    val name: String,
    val level: String,
)

data class OtherSection(
    val nameOfSection: String,
    val entries: List<OtherSectionEntry>,
)

data class OtherSectionEntry(
    val companyOrOrganization: String?,
    val title: String,
    val description: String,
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val startDate: Date?,
    @JsonProperty("endDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val endDate: Date?,
)

data class PersonalInfo(
    val name: String,
    val location: String,
    val phone: String,
    val email: String,
    val links: List<Link>?,
)

data class Link(
    val name: String,
    val url: String,
)

data class TechnicalSkills(
    val backend: String,
    val frontend: String,
    val databases: String,
    @JsonProperty("devopsAndCloud")
    val devopsAndCloud: String,
    @JsonProperty("toolsAndMethodologies")
    val toolsAndMethodologies: String,
    @JsonProperty("otherSkills")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val otherSkills: List<String> = emptyList(),
)

enum class EmploymentType {
    FULL_TIME,
    PART_TIME,
    CONTRACT,
    INTERNSHIP,
    PET_PROJECT,
    ;

    companion object {
        fun fromString(value: String): EmploymentType =
            when (value.lowercase()) {
                "full-time" -> FULL_TIME
                "part-time" -> PART_TIME
                "contract" -> CONTRACT
                "internship" -> INTERNSHIP
                "pet-project" -> PET_PROJECT
                else -> throw IllegalArgumentException("Invalid employmentType: $value")
            }
    }
}

data class JobExperience(
    val title: String,
    val company: String,
    val location: String,
    @JsonProperty("employmentType")
    @JsonDeserialize(using = EmploymentTypeDeserializer::class)
    val employmentType: EmploymentType,
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val startDate: Date,
    @JsonProperty("endDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val endDate: Date?,
    val bullets: List<String>,
)

class EmploymentTypeDeserializer : JsonDeserializer<EmploymentType>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): EmploymentType {
        val value = p.text
        return try {
            EmploymentType.fromString(value)
        } catch (e: IllegalArgumentException) {
            // ctxt.mappingException(String) was removed (not just deprecated) in the Jackson
            // version pulled in once TASK-011 added spring-boot-starter-parent's BOM management -
            // reportInputMismatch is the current, non-deprecated replacement.
            ctxt.reportInputMismatch(
                EmploymentType::class.java,
                "employmentType must be one of: full-time, part-time, contract, internship, pet-project. Got: '%s'",
                value,
            )
        }
    }
}

data class EducationEntry(
    val degree: String,
    val institution: String,
    val location: String,
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val startDate: Date,
    @JsonProperty("graduationYear")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val graduationYear: Date,
)
