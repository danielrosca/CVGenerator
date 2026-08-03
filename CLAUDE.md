# CVGenerator

Kotlin tool that turns a YAML CV description into a styled PDF. Built and maintained before the JobAchiever project existed. JobAchiever's backend (`job-applier`) needs to call this as a service — Phase 3 of `../TASKS.md` wraps it in a small Spring Boot REST layer (`POST /generate`) without disturbing the core generation logic described here.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22, Java 21 target |
| YAML parsing | Jackson (`jackson-module-kotlin` + `jackson-dataformat-yaml`) |
| HTML generation | kotlinx.html DSL (`kotlinx-html-jvm`) |
| PDF generation | **Microsoft Playwright** (headless Chromium) |
| Packaging | Maven Shade (fat jar via `mvn package`, runnable with `java -jar`) |

---

## What It Does

A single, non-Spring, non-CLI-args `fun main()` in `Main.kt`:

1. Reads a YAML file (path is hardcoded in source — see Known Issues) into `CvData` via `ObjectMapper(YAMLFactory()).registerKotlinModule()`.
2. `generateHtml(cvData): String` (`html/HTMLGenerator.kt`) renders it to an HTML string.
3. Writes that HTML to disk.
4. `generatePdf(htmlContent, outputPath)` (`pdf/PDFGenerator.kt`) launches headless Chromium via Playwright, writes the HTML to a temp file, navigates to it with `Media.PRINT` emulation (so `@media print` CSS applies) and `WaitUntilState.NETWORKIDLE`, then exports an A4 PDF with zero margins and `printBackground=true`.

**For the REST wrapper (Phase 3)**: the two functions to expose are `generateHtml(cv: CvData): String` and `generatePdf(htmlContent: String, outputPath: String)` — the wrapper's job is to accept a YAML (or already-parsed `CvData` JSON) body over HTTP, call these two functions in sequence, and stream the resulting PDF bytes back, instead of writing to hardcoded file paths.

---

## YAML Schema (`dto/CvData.kt`)

```
CvData
├── personalInfo: PersonalInfo        # name, location, phone, email, links: List<Link>?
├── professionalSummary: String
├── technicalSkills: TechnicalSkills  # backend, frontend, databases, devopsAndCloud,
│                                     # toolsAndMethodologies, otherSkills: List<String> (never populated in practice)
├── experience: List<JobExperience>   # title, company, location, employmentType, startDate,
│                                     #   endDate (null = "Present"), bullets: List<String>
├── education: List<EducationEntry>   # degree, institution, location, startDate, graduationYear
├── languages: List<Language>?        # name, level (free text, e.g. "Native", "C1")
└── otherSections: List<OtherSection>? # nameOfSection, entries: List<OtherSectionEntry>
                                        #   (companyOrOrganization?, title, description, startDate?, endDate?)
```

- All dates are `java.util.Date`, deserialized strictly as `yyyy-MM-dd` via `@JsonFormat`.
- `employmentType` accepts `full-time | part-time | contract | internship | pet-project` (hyphenated, lowercase) via a custom `EmploymentTypeDeserializer`; anything else throws a Jackson mapping exception with a descriptive message.
- `HTMLGenerator` computes per-job and total experience duration itself (`calculateDuration`/`sumDurations`) from `startDate`/`endDate` — the yaml never states duration directly.
- **`technicalSkills.otherSkills` is parsed but never rendered** by `HTMLGenerator` — it's dead data in the current schema. Decide during the REST wrapper work whether to render it or drop it from the schema; don't silently leave it half-wired.

Existing sample yaml files under `src/main/resources/` (17 total, across `Dec11/`, `Feb9 2026/`, `Google/`, root) are all the same person's CV re-tailored per company/vacancy — useful as fixtures for tests, but note: one (`Dec11/Microinvest_Senior/cv.yaml`) is written in Russian with inconsistent indentation, and `Feb9 2026/MICB/` has a `vacancy.txt` but no matching `cv.yaml`. Don't assume every sample file is a clean, representative fixture without checking it first.

---

## HTML/PDF Rendering Details

- CSS is **inlined directly in Kotlin source** inside `HTMLGenerator.kt` (a large raw string in a `style { unsafe { ... } } ` block) — `src/main/resources/styles.css` exists but the `<link rel="stylesheet" href="styles.css">` line that would use it is commented out, so that file is currently dead/orphaned. If consolidating styling during later work, decide whether to move the inline CSS back out to `styles.css` and re-enable the link, or delete the orphaned file — don't leave both half-in-use.
- Since real headless Chromium renders the HTML (not flying-saucer), modern CSS (flexbox, grid) in the inline stylesheet works fine — there's no CSS2.1-only limitation to design around.
- `src/main/resources/DejaVuSans.ttf` is not referenced anywhere in the current code (`grep -rn "DejaVu" src/main/kotlin/` finds nothing) — leftover from an earlier, non-Playwright PDF approach. Safe to delete once confirmed unused, or note why it's kept if there's a reason (e.g. planned custom-font support).
- Playwright requires browser binaries to be installed (`playwright install`) — this isn't wired into the Maven build (no `exec-maven-plugin` hook), so a fresh checkout needs that run manually once. The REST wrapper's Dockerfile will need to install the Chromium binary in the image.

---

## Known Issues (found during Phase 0 review — see `../TASKS.md`)

1. ~~`pom.xml`'s `maven-jar-plugin` manifest declared `mainClass = com.cvgenerator.MainKt`~~ — **Fixed in TASK-006.** Now `md.daniel_rosca.MainKt`, and a `maven-shade-plugin` execution bundles a runnable fat jar on `mvn package`; `java -jar target/cv-generator-1.0-SNAPSHOT.jar` runs standalone with no IDE and no classpath args.
2. ~~`flying-saucer-pdf-openpdf` / commented-out iText dependencies~~ — **Fixed in TASK-006.** Both removed from `pom.xml`.
3. **`src/main/resources/DejaVuSans.ttf`** — still not referenced anywhere in code. Kept (not deleted) since it was deliberately committed alongside the CV layout/example-CV work; if a concrete use (e.g. custom font embedding for non-Latin scripts) doesn't materialize, revisit deleting it.
4. **Zero tests exist** — no `src/test/kotlin` content, and no test dependency (JUnit/Kotest/MockK) declared in `pom.xml` at all. (TASK-008)
5. **No linter/formatter configured** — no ktlint, detekt, or `.editorconfig`. (TASK-007)
6. ~~No Maven Wrapper~~ — **Fixed in TASK-006.** `./mvnw`/`mvnw.cmd` committed, pinned to Maven 3.9.11.
7. **`Main.kt`'s hardcoded input/output paths** must change every time you want to generate a different CV — this is expected to go away once Phase 3 replaces the CLI entry point with a REST endpoint, but until then it's the normal (manual) way this tool is used.
8. ~~Error handling in `Main.kt` doesn't fail loudly~~ — **Fixed in TASK-006.** Both the YAML-parse and PDF-generation failure paths now call `exitProcess(1)` after logging to `System.err`, so a broken run returns a non-zero exit code instead of looking like partial success.

---

## Running Locally

```bash
cd CVGenerator
# Edit the hardcoded paths in Main.kt to point at the yaml you want to generate
./mvnw clean package
java -jar target/cv-generator-1.0-SNAPSHOT.jar
```

Running via IDE (IntelliJ run config "MainKt") still works too — `mvn exec:java` is still not configured, the jar is the supported non-IDE path.

## Testing

No tests exist yet — see `../TASKS.md` Phase 0 for the task to add them (YAML parsing edge cases, HTML generation for each optional-field combination, duration calculation logic in `HTMLGenerator.calculateDuration`/`sumDurations` are the highest-value first targets since they're pure functions with no I/O).
