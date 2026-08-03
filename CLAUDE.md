# CVGenerator

Kotlin tool that turns a YAML CV description into a styled PDF. Built and maintained before the JobAchiever project existed. JobAchiever's backend (`job-applier`) needs to call this as a service — Phase 3 of `../TASKS.md` wraps it in a small Spring Boot REST layer (`POST /generate`) without disturbing the core generation logic described here.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22, Java 21 target |
| YAML parsing | Jackson (`jackson-module-kotlin` + `jackson-dataformat-yaml`) |
| HTML generation | kotlinx.html DSL (`kotlinx-html-jvm`) |
| PDF generation | **Microsoft Playwright** (headless Chromium) — see Known Issues, the `pom.xml`-declared `flying-saucer-pdf-openpdf` dependency is dead code, not actually used |
| Packaging | Plain Maven, no fat-jar/shade/assembly plugin configured |

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

1. **`pom.xml`'s `maven-jar-plugin` manifest declares `mainClass = com.cvgenerator.MainKt`**, but the actual package is `md.daniel_rosca` — running `java -jar` on a plain `mvn package` output would fail with `ClassNotFoundException`. There's also no shade/assembly plugin to bundle dependencies into a runnable fat jar, so `mvn package` alone doesn't currently produce anything directly runnable — the project is only ever run from the IDE today.
2. **`flying-saucer-pdf-openpdf` is declared as a dependency but not used anywhere in code** — dead weight, remove it (or the equally-unused commented-out iText dependencies) once confirmed.
3. **`src/main/resources/DejaVuSans.ttf`** — see above, orphaned resource.
4. **Zero tests exist** — no `src/test/kotlin` content, and no test dependency (JUnit/Kotest/MockK) declared in `pom.xml` at all.
5. **No linter/formatter configured** — no ktlint, detekt, or `.editorconfig`.
6. **No Maven Wrapper** (`mvnw`) committed.
7. **`Main.kt`'s hardcoded input/output paths** must change every time you want to generate a different CV — this is expected to go away once Phase 3 replaces the CLI entry point with a REST endpoint, but until then it's the normal (manual) way this tool is used.
8. **Error handling in `Main.kt` doesn't fail loudly**: both the YAML-parse and PDF-generation try/catch blocks `println` the error and continue/return without a non-zero exit code — a broken run currently looks like partial success from the outside (e.g. in a CI log or a script checking exit codes). Fix as part of Phase 3 since the REST wrapper needs real error propagation (HTTP 400/500) anyway.

---

## Running Locally

```bash
cd CVGenerator
# Edit the hardcoded paths in Main.kt to point at the yaml you want to generate
mvn compile
# Run via IDE (IntelliJ run config "MainKt") — `mvn exec:java` is not configured
```

## Testing

No tests exist yet — see `../TASKS.md` Phase 0 for the task to add them (YAML parsing edge cases, HTML generation for each optional-field combination, duration calculation logic in `HTMLGenerator.calculateDuration`/`sumDurations` are the highest-value first targets since they're pure functions with no I/O).
