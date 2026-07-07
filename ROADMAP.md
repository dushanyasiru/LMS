# O/L ICT Tuition — LMS Build Roadmap

A custom Learning Management System for a solo ICT tuition (Grade 10 & 11).
**Stack:** Spring Boot (Java 17) backend + no-build React frontend + Supabase Postgres.

## What it does
- **One teacher login** (you) + **student logins** (you create each account).
- Upload **notes, lecture-wise** (organised under lessons).
- Upload **papers** (past papers, model papers, etc.).
- Students **submit answer files** to assignments.
- You give **marks + written feedback** in-app; students see their grades.

---

## Tech stack (uses tools already installed)
| Layer | Choice | Why |
|-------|--------|-----|
| Backend | **Spring Boot 3 (Java 17, Maven)** | REST API + serves the frontend. JDK 17 + Maven already installed. |
| Auth | **Spring Security + JWT** | Standard token login; teacher vs student roles. |
| Data access | **Spring Data JPA (Hibernate)** | Maps Java objects to database tables. |
| Database | **Supabase Postgres** (hosted, free) | Reuse the project already created. |
| File storage | Local `uploads/` folder (dev) → cloud later | Simple to start; swap for cloud on deploy. |
| Frontend | **No-build React** (CDN + Babel) | React without Node. Served by Spring Boot. |
| Run everything | **`mvn spring-boot:run`** → http://localhost:8080 | One command; no Node, no Python. |

**Why no-build React?** Node here is v14 (Vite needs 18+). Loading React from a CDN needs no Node. If Node is upgraded later, we can move to a Vite build with no data loss.

---

## Project structure (inside `LMS/`)
```
LMS/
  pom.xml                                  # Maven build + dependencies
  .gitignore
  src/main/java/com/ict/lms/
      LmsApplication.java                  # app entry point
      web/        (REST controllers)       # /api/... endpoints
      model/      (JPA entities)           # Phase 2
      repo/       (Spring Data repos)      # Phase 2
      security/   (Spring Security + JWT)  # Phase 3
      service/    (business logic)         # Phase 4+
  src/main/resources/
      application.properties               # app config
      application-secret.properties        # DB password + JWT key (git-ignored)
      static/
          index.html                       # React app (login + dashboards)
          css/styles.css
```

---

## Data model (JPA entities → Postgres tables)
- **AppUser** — `id`, `email`, `passwordHash`, `fullName`, `role` (`TEACHER`/`STUDENT`), `grade` (10/11), `createdAt`.
- **Lesson** — `id`, `title`, `description`, `grade`, `lessonNo`, `createdAt`.
- **Resource** — files. `id`, `title`, `category` (`NOTE`/`PAPER`), `lesson` (nullable), `grade`, `filePath`, `originalName`, `createdAt`.
- **Assignment** — `id`, `title`, `description`, `grade`, `questionFilePath` (nullable), `dueDate`, `createdAt`.
- **Submission** — `id`, `assignment`, `student`, `filePath`, `originalName`, `submittedAt`, `marks` (nullable), `feedback` (nullable), `gradedAt` (nullable).

**Authorization:** enforced in the backend (Spring Security) — students only see content for their grade and only their own submissions; only the teacher creates content and edits marks.

---

## Build phases
### Phase 1 — Skeleton ✅ (in progress)
- `pom.xml`, `LmsApplication`, a `/api/ping` test endpoint, and a no-build React page that calls it.
- **Verify:** `mvn spring-boot:run` → http://localhost:8080 shows "Connected".

### Phase 2 — Database
- Add JPA + PostgreSQL driver to `pom.xml`.
- Put the Supabase connection string + password in `application-secret.properties`.
- Create the entities above; Hibernate auto-creates the tables.

### Phase 3 — Auth (login)
- Add Spring Security + JWT deps.
- `POST /api/auth/login` → returns a token; React stores it and sends it on every request.
- Seed **your teacher account** on first startup.
- React login page → routes to teacher or student view based on role.

### Phase 4 — Teacher features
- Lessons CRUD + **upload notes** into a lesson.
- **Upload papers.**
- Create **assignments** (optional question file + due date).
- **Add students** (email, name, grade, temp password).

### Phase 5 — Student features
- Browse lessons + download notes (their grade).
- Browse/download papers.
- View assignments and **upload answer files**.

### Phase 6 — Marking
- Teacher opens a submission → enter **marks + feedback**.
- Student sees **grade + comments**.

### Phase 7 — Deploy
- Host the backend (e.g. Render/Railway free tier) connected to Supabase Postgres.
- Move file storage from local folder to cloud (e.g. Supabase Storage) so uploads survive restarts.

---

## How we verify each phase
`mvn spring-boot:run` in `LMS/`, open http://localhost:8080. After each phase test the real flow: log in as teacher, upload a note, add a student, log in as that student (private window), submit an assignment, then mark it.
