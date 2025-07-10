package md.daniel_rosca.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// The root object for the entire CV data
data class CvData(
    val personalInfo: PersonalInfo,
    val professionalSummary: String,
    val technicalSkills: TechnicalSkills,
    val experience: List<Job>,
    val education: List<EducationEntry>
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
    @JsonProperty("devopsAndCloud") // Handles the camelCase key in YAML
    val devopsAndCloud: String,
    @JsonProperty("toolsAndMethodologies") // Handles the camelCase key in YAML
    val toolsAndMethodologies: String,
    @JsonProperty("otherSkills") // Handles the camelCase key in YAML
    @JsonInclude(JsonInclude.Include.NON_NULL) // Exclude if empty
    val otherSkills: List<String> = emptyList()
)

data class Job(
    val title: String,
    val company: String,
    val location: String,
    val dates: String,
    val bullets: List<String>
)

data class EducationEntry(
    val degree: String,
    val institution: String,
    val location: String,
    val graduationYear: String
)