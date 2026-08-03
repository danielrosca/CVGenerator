# CVGenerator

Kotlin tool that turns a YAML CV description into a styled PDF. Built and maintained before the JobAchiever project existed. JobAchiever's backend (`job-applier`) calls this as a service — **TASK-011** added a thin Spring Boot REST layer (`POST /generate`, package `md.daniel_rosca.web`) on top of the core generation logic described below, without disturbing it. `Main.kt`'s standalone CLI entry point still works unchanged for local ad-hoc generation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22, Java 21 target |
| YAML parsing | Jackson (`jackson-module-kotlin` + `jackson-dataformat-yaml`, version-managed by the Spring Boot BOM as of TASK-011) |
| HTML generation | kotlinx.html DSL (`kotlinx-html-jvm`) |
| PDF generation | **Microsoft Playwright** (headless Chromium) |
| REST layer | Spring Boot 3.5.3 (`spring-boot-starter-web`) — added in TASK-011 |
| Packaging | `spring-boot-maven-plugin` (repackaged executable jar via `mvn package`, runnable with `java -jar`) — replaced `maven-shade-plugin` in TASK-011, since shade doesn't merge Spring's `META-INF/spring/*.imports` files correctly |

---

## What It Does

A single, non-Spring, non-CLI-args `fun main()` in `Main.kt`:

1. Reads a YAML file (path is hardcoded in source — see Known Issues) into `CvData` via `ObjectMapper(YAMLFactory()).registerKotlinModule()`.
2. `generateHtml(cvData): String` (`html/HTMLGenerator.kt`) renders it to an HTML string.
3. Writes that HTML to disk.
4. `generatePdf(htmlContent, outputPath)` (`pdf/PDFGenerator.kt`) launches headless Chromium via Playwright, writes the HTML to a temp file, navigates to it with `Media.PRINT` emulation (so `@media print` CSS applies) and `WaitUntilState.NETWORKIDLE`, then exports an A4 PDF with zero margins and `printBackground=true`.

**REST wrapper (`src/main/kotlin/md/daniel_rosca/web/`, added in TASK-011)**:
- `CvGeneratorApplication` — `@SpringBootApplication` entry point (`server.port` defaults to `8090`, see `application.properties`).
- `CvGenerationService` — wraps `generateHtml(cv: CvData): String` + `generatePdf(htmlContent: String, outputPath: String)` behind a single `generate(cvData, outputPdfPath)` call. Both `Main.kt` and `CvGeneratorController` call this instead of duplicating the two-step orchestration.
- `CvGeneratorController` — `POST /generate` deserializes the request body directly into `CvData` (reusing the existing Jackson annotations, no schema duplication), writes the PDF to a temp file, streams the bytes back as `application/pdf`, then deletes the temp file. Malformed JSON is rejected with `400` by Spring's own Jackson integration; PDF-generation failures map to a generic `500` via `ResponseStatusException` (no internal stack trace exposed to the caller).

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
- **`technicalSkills.otherSkills`** — **fixed in TASK-007.** Now rendered as an "Other:" row in the Technical Skills section (joined with `, `), shown only when the list is non-empty — no existing sample yaml populates it, so no existing output changed.

Existing sample yaml files under `src/main/resources/` (17 total, across `Dec11/`, `Feb9 2026/`, `Google/`, root) are all the same person's CV re-tailored per company/vacancy — useful as fixtures for tests, but note: one (`Dec11/Microinvest_Senior/cv.yaml`) is written in Russian with inconsistent indentation, and `Feb9 2026/MICB/` has a `vacancy.txt` but no matching `cv.yaml`. Don't assume every sample file is a clean, representative fixture without checking it first.

---

## HTML/PDF Rendering Details

- CSS is **inlined directly in Kotlin source** inside `HTMLGenerator.kt` (a large raw string in a `style { unsafe { ... } } ` block) — `src/main/resources/styles.css` exists but the `<link rel="stylesheet" href="styles.css">` line that would use it is commented out, so that file is currently dead/orphaned. If consolidating styling during later work, decide whether to move the inline CSS back out to `styles.css` and re-enable the link, or delete the orphaned file — don't leave both half-in-use.
- Since real headless Chromium renders the HTML (not flying-saucer), modern CSS (flexbox, grid) in the inline stylesheet works fine — there's no CSS2.1-only limitation to design around.
- `src/main/resources/DejaVuSans.ttf` is not referenced anywhere in the current code (`grep -rn "DejaVu" src/main/kotlin/` finds nothing) — leftover from an earlier, non-Playwright PDF approach. Safe to delete once confirmed unused, or note why it's kept if there's a reason (e.g. planned custom-font support).
- Playwright requires browser binaries to be installed (`playwright install`) — this isn't wired into the Maven build (no `exec-maven-plugin` hook), so a fresh local checkout needs that run manually once. `Dockerfile` (TASK-011) installs them at image-build time — see the note below on why it installs the *full* default set, not just Chromium.
- **`Playwright.create()` validates the driver's entire default browser manifest on first use, not just the browser your code actually launches.** `PDFGeneratorTest`/`PDFGenerator.kt` only ever call `pw.chromium()`, but a Docker image built with `playwright install --with-deps chromium` (Chromium only) will silently download ~176MiB of Firefox+Webkit from the network on the *first* `/generate` request, before serving it — discovered by testing the built image directly, not assumed. `Dockerfile` installs the full default set (`install --with-deps`, no browser name argument) specifically to avoid this hidden runtime network dependency.
- **The Chromium browser process is pooled, not relaunched per PDF (CHORE-002).** `PDFGenerator.kt`'s private `BrowserPool` object launches Chromium once and keeps it alive for the whole process's lifetime, opening a fresh, isolated `BrowserContext`/`Page` per render and closing it afterward — the previous per-call `Playwright.create()` + `chromium().launch()` measurably cost ~0.85–0.9s/request in steady state just relaunching the browser; pooling brought that to ~0.58–0.6s/request (~30–35% faster), verified both locally and against the built Docker image. All rendering is funneled through one dedicated daemon thread (`Executors.newSingleThreadExecutor`) since Playwright's Java driver isn't safe to call concurrently from multiple threads against the same instance — concurrent `/generate` calls queue rather than run in parallel, which is fine given this project's single-user scope (see root `CLAUDE.md`). A JVM shutdown hook closes the browser/Playwright cleanly on `docker stop`/SIGTERM, and `Browser.isConnected()` is checked before reuse so a crashed browser (e.g. OOM) gets transparently relaunched instead of failing every subsequent request. `generatePdf()`'s public signature is unchanged, so `Main.kt`, `CvGenerationService`, and `CvGeneratorController` needed no changes of their own.

---

## Known Issues (found during Phase 0 review — see `../TASKS.md`)

1. ~~`pom.xml`'s `maven-jar-plugin` manifest declared `mainClass = com.cvgenerator.MainKt`~~ — **Fixed in TASK-006, superseded in TASK-011.** `mvn package` now produces a Spring Boot repackaged jar (`spring-boot-maven-plugin`, replacing TASK-006's `maven-shade-plugin`) whose main class starts the REST server (`md.daniel_rosca.web.CvGeneratorApplicationKt`); `java -jar target/cv-generator-1.0-SNAPSHOT.jar` now boots the HTTP server, not the old CLI flow. `Main.kt`'s CLI path is still fully runnable via IDE or `java -cp <classpath> md.daniel_rosca.MainKt`.
2. ~~`flying-saucer-pdf-openpdf` / commented-out iText dependencies~~ — **Fixed in TASK-006.** Both removed from `pom.xml`.
3. **`src/main/resources/DejaVuSans.ttf`** — still not referenced anywhere in code. Kept (not deleted) since it was deliberately committed alongside the CV layout/example-CV work; if a concrete use (e.g. custom font embedding for non-Latin scripts) doesn't materialize, revisit deleting it.
4. ~~Zero tests exist~~ — **Fixed in TASK-008.** 24 tests across 4 classes; see Testing section below.
5. ~~No linter/formatter configured~~ — **Fixed in TASK-007.** `ktlint-maven-plugin` bound to `verify`. Two rules disabled via `.editorconfig` with documented justification: `package-name` (the existing `md.daniel_rosca` package predates this and renaming it would ripple through every file, `pom.xml`'s manifest config, and the `md.daniel_rosca.web` REST wrapper package added in TASK-011) and `no-wildcard-imports` for `HTMLGenerator.kt` only (kotlinx.html's DSL exposes dozens of tag-builder functions; explicit imports for that one file would be a long, low-value list — the wildcard is the idiomatic pattern kotlinx.html's own docs use).
6. ~~No Maven Wrapper~~ — **Fixed in TASK-006.** `./mvnw`/`mvnw.cmd` committed, pinned to Maven 3.9.11.
7. **`Main.kt`'s hardcoded input/output paths** must change every time you want to generate a different CV via the CLI. **Not fixed in TASK-011 by design** — the REST endpoint (`POST /generate`) is now the way to generate an arbitrary CV without editing source, and `Main.kt` is deliberately kept as-is for local ad-hoc use alongside it, not replaced.
8. ~~Error handling in `Main.kt` doesn't fail loudly~~ — **Fixed in TASK-006.** Both the YAML-parse and PDF-generation failure paths now call `exitProcess(1)` after logging to `System.err`, so a broken run returns a non-zero exit code instead of looking like partial success.

---

## Running Locally

**As the REST server** (what `java -jar` now runs, since TASK-011):

```bash
cd CVGenerator
./mvnw clean package
java -jar target/cv-generator-1.0-SNAPSHOT.jar   # starts on :8090 (SERVER_PORT env var overrides)
curl -X POST http://localhost:8090/generate \
  -H "Content-Type: application/json" \
  --data @path/to/cvdata.json \
  --output out.pdf
```

Or via Docker: `docker build -t cv-generator . && docker run -p 8090:8090 cv-generator`.

**As the standalone CLI** (unchanged, `Main.kt`):

```bash
cd CVGenerator
# Edit the hardcoded paths in Main.kt to point at the yaml you want to generate
java -cp "target/classes:$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" md.daniel_rosca.MainKt
```

Running via IDE (IntelliJ run config "MainKt") still works too — `mvn exec:java` is still not configured.

## Testing

```bash
./mvnw test      # CvDataDeserializationTest, HTMLGeneratorDurationTest, HTMLGeneratorRenderingTest,
                  # PDFGeneratorTest, CvGenerationServiceTest, CvGeneratorControllerTest (28 tests total)
./mvnw verify    # + ktlint check
```

No `maven-failsafe-plugin` in this repo — `CvGeneratorControllerTest` is a `@SpringBootTest`/`MockMvc` test that needs no Testcontainers/Docker, so it's named `*Test` (not `*IT`) and runs under plain `./mvnw test`, same as everything else.

`HTMLGeneratorDurationTest` reaches `calculateDuration`/`sumDurations`/`Duration` (all private, no public seam) via reflection for setup/invocation only — production code is untouched. `PDFGeneratorTest` requires Playwright's Chromium to be available locally (downloaded automatically on first run in a fresh environment, same as running the app itself).
