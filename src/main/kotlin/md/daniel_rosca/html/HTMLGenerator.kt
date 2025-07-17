package md.daniel_rosca.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import md.daniel_rosca.dto.CvData
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import java.text.SimpleDateFormat

private fun Date.toLocalDate(): LocalDate = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun Date.formatToLongDate(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
    return sdf.format(this)
}

private data class Duration(val years: Long, val months: Long) {
    override fun toString(): String {
        val y = if (years > 0) "$years year${if (years > 1) "s" else ""}" else ""
        val m = if (months > 0) "$months month${if (months > 1) "s" else ""}" else ""
        return when {
            years > 0 && months > 0 -> "$y $m"
            years > 0 -> y
            months > 0 -> m
            else -> "1 month"
        }
    }
}

private fun calculateDuration(start: Date, end: Date): Duration {
    val startDate = start.toLocalDate()
    val endDate = end.toLocalDate()
    var months = ChronoUnit.MONTHS.between(startDate, endDate)
    val days = ChronoUnit.DAYS.between(startDate.plusMonths(months), endDate)
    // If there are leftover days, round up to next month
    if (days > 0) months += 1
    if (months < 1) months = 1 // Always at least 1 month
    val years = months / 12
    val remMonths = months % 12
    return Duration(years, remMonths)
}

private fun sumDurations(durations: List<Duration>): Duration {
    var totalMonths = durations.sumOf { it.years * 12 + it.months }
    if (totalMonths < 1) totalMonths = 1
    val years = totalMonths / 12
    val months = totalMonths % 12
    return Duration(years, months)
}

fun generateHtml(cv: CvData): String {
    // Calculate durations for each job
    val jobDurations = cv.experience.map { calculateDuration(it.startDate, it.endDate ?: Date()) }
    val totalDuration = sumDurations(jobDurations)

    return createHTML(xhtmlCompatible = true).html {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
            title("${cv.personalInfo.name} - CV")
            style {
                unsafe {
                    +"""
                        body { 
                            font-family: Arial, Helvetica, sans-serif;
                            line-height: 1.6; 
                            color: #333;
                            max-width: 800px;
                            margin: 40px auto;
                            padding: 20px;
                            background-color: #ffffff;
                        }
                        .header { text-align: center; border-bottom: 2px solid #eee; padding-bottom: 20px; margin-bottom: 20px; }
                        .header h1 { margin: 0; color: #1a1a1a; font-size: 2.5em; }
                        .contact-info { display: flex; justify-content: center; flex-wrap: wrap; gap: 15px; margin-top: 10px; font-size: 0.9em; }
                        .contact-info a { text-decoration: none; color: #007bff; }
                        .section h2 { 
                            font-size: 1.4em; 
                            color: #333;
                            border-bottom: 2px solid #007bff;
                            padding-bottom: 5px; 
                            margin-top: 30px;
                            margin-bottom: 15px;
                        }
                        .job { margin-bottom: 20px; }
                        .job-header { display: flex; justify-content: space-between; align-items: baseline; }
                        .job-header h3 { margin: 0; font-size: 1.1em; }
                        .job-header .company { font-style: italic; color: #555; }
                        .job-header .dates { color: #777; font-size: 0.9em; }
                        .job-header .line { display: block; margin: 2px 0; }
                        .job-header br { line-height: 1.2; }
                        .job ul { padding-left: 20px; margin-top: 10px; }
                        .job li { margin-bottom: 8px; }
                        .preserve-newlines { white-space: pre-line; }
                        .skills-grid { display: grid; grid-template-columns: 150px 1fr; gap: 8px; }
                        .skills-grid strong { color: #1a1a1a; }
                    """
                }
            }
        }
        body {
            div("header") {
                h1 { +cv.personalInfo.name }
                div("contact-info") {
                    span { +cv.personalInfo.location }
                    span { +" | " }
                    span{ +cv.personalInfo.phone }
                    span { +" | " }
                    a(href = "mailto:${cv.personalInfo.email}") { +cv.personalInfo.email }
                    for (link in cv.personalInfo.links) {
                        span { +" | " }
                        a(href = link.url, target = "_blank") { +link.name }
                    }
                }
            }

            div("section") {
                h2 { +"Professional Summary" }
                p { +cv.professionalSummary }
            }

            div("section") {
                h2 { +"Technical Skills" }
                div("skills-grid") {
                    strong { +"Backend: " }
                    span { +cv.technicalSkills.backend }
                    strong { +"Frontend: " }
                    span { +cv.technicalSkills.frontend }
                    strong { +"Databases: " }
                    span { +cv.technicalSkills.databases }
                    strong { +"DevOps & Cloud: " }
                    span { +cv.technicalSkills.devopsAndCloud }
                    strong { +"Tools & Methods: " }
                    span { +cv.technicalSkills.toolsAndMethodologies }
                }
            }

            div("section") {
                h2 { +"Professional Experience (${totalDuration})" }
                cv.experience.zip(jobDurations).forEach { (job, duration) ->
                    div("job") {
                        div("job-header") {
                            div {
                                h3 { +"${job.title} (${duration})" }
                                div("employment-type") {
                                    +job.employmentType.name.replace('_', '-').lowercase().replaceFirstChar { it.uppercase() }
                                }
                                div("company") { +" at ${job.company}" }
                                div("dates") {
                                    +"${job.startDate.formatToLongDate()} – "
                                    if (job.endDate == null) {
                                        +"Present"
                                    } else {
                                        +job.endDate.formatToLongDate()
                                    }
                                }
                            }
                        }
                        ul {
                            job.bullets.forEach { bullet ->
                                li { +bullet }
                            }
                        }
                    }
                }
            }

            div("section") {
                h2 { +"Education" }
                cv.education.forEach { edu ->
                    div("job-header") {
                        h3 { +edu.degree }
                        span("dates") { +"${edu.startDate.formatToLongDate()} – ${edu.graduationYear.formatToLongDate()}" }
                    }
                    p { +"${edu.institution}, ${edu.location}" }
                }
            }

            // Render languages section if present
            cv.languages?.let { langs ->
                if (langs.isNotEmpty()) {
                    div("section") {
                        h2 { +"Languages" }
                        ul {
                            langs.forEach { lang ->
                                li { +"${lang.name}: ${lang.level}" }
                            }
                        }
                    }
                }
            }

            // Render otherSections if present
            cv.otherSections?.forEach { section ->
                div("section") {
                    h2 { +section.nameOfSection }
                    section.entries.forEach { entry ->
                        div("job") {
                            div("job-header") {
                                h3 { +entry.title }
                                if (entry.companyOrOrganization != null) {
                                    span("company") { +" at ${entry.companyOrOrganization}" }
                                    hr {}
                                }
                                if (entry.startDate != null) {
                                    span("dates") {
                                        +entry.startDate.formatToLongDate()
                                        +" - "
                                        if (entry.endDate != null) {
                                            +entry.endDate.formatToLongDate()
                                        } else {
                                            +"Present"
                                        }
                                    }
                                }
                            }
                            p {
                                +entry.description
                            }
                        }
                    }
                }
            }
        }
    }
}