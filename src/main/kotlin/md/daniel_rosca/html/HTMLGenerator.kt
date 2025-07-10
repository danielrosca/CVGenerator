package md.daniel_rosca.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import md.daniel_rosca.dto.CvData

fun generateHtml(cv: CvData): String {
    return createHTML(xhtmlCompatible = true).html {
        head {
            meta(charset = "UTF-8")
            title("${cv.personalInfo.name} - CV")
            style {
                // Professional and clean CSS for the CV
                unsafe {
                    +"""
                        body { 
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
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
                        .job ul { padding-left: 20px; margin-top: 10px; }
                        .job li { margin-bottom: 8px; }
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
                    a(href = "mailto:${cv.personalInfo.email}") { +cv.personalInfo.email }
                    span { +" | " }
                    a(href = "https://${cv.personalInfo.linkedin}", target = "_blank") { +cv.personalInfo.linkedin }
                    span { +" | " }
                    a(href = "https://${cv.personalInfo.github}", target = "_blank") { +cv.personalInfo.github }
                }
            }

            div("section") {
                h2 { +"Professional Summary" }
                p { +cv.professionalSummary }
            }

            div("section") {
                h2 { +"Technical Skills" }
                div("skills-grid") {
                    strong { +"Backend:" }
                    span { +cv.technicalSkills.backend }
                    br {}
                    strong { +"Frontend:" }
                    span { +cv.technicalSkills.frontend }
                    br {}
                    strong { +"Databases:" }
                    span { +cv.technicalSkills.databases }
                    br {}
                    strong { +"DevOps & Cloud:" }
                    span { +cv.technicalSkills.devopsAndCloud }
                    br {}
                    strong { +"Tools & Methods:" }
                    span { +cv.technicalSkills.toolsAndMethodologies }
                    br {}
                }
            }

            div("section") {
                h2 { +"Professional Experience" }
                cv.experience.forEach { job ->
                    div("job") {
                        div("job-header") {
                            h3 {
                                +job.title
                                span("company") { +" at ${job.company}" }
                            }
                            span("dates") { +job.dates }
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
                        span("dates") { +edu.graduationYear }
                    }
                    p { +"${edu.institution}, ${edu.location}" }
                }
            }
        }
    }
}