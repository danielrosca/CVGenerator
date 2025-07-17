package md.daniel_rosca.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date

// The root object for the entire CV data
data class CvData(
    val personalInfo: PersonalInfo,
    val professionalSummary: String,
    val technicalSkills: TechnicalSkills,
    val experience: List<JobExperience>,
    val education: List<EducationEntry>,
    @JsonProperty("otherSections")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val otherSections: List<OtherSection>? = null
)

data class OtherSection(
    val nameOfSection: String,
    val entries: List<OtherSectionEntry>
)

data class OtherSectionEntry(
    val companyOrOrganization: String?,
    val title: String,
    val description: String,
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val startDate: Date,
    @JsonProperty("endDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val endDate: Date?
)

data class PersonalInfo(
    val name: String,
    val location: String,
    val phone: String,
    val email: String,
    val linkedin: String,
    val github: String,
    val website: String
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
    val otherSkills: List<String> = emptyList()
)

data class JobExperience(
    val title: String,
    val company: String,
    val location: String,
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val startDate: Date,
    @JsonProperty("endDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val endDate: Date?,
    val bullets: List<String>
)

data class EducationEntry(
    val degree: String,
    val institution: String,
    val location: String,
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val startDate: Date,
    @JsonProperty("graduationYear")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val graduationYear: Date
)