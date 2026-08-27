# MentorTrack

A mentor–mentee management system. Mentors manage their assigned mentees by tracking
**assessments, assignments, and tasks**, upload **documents** to each mentee's profile, and create
**Google Forms** whose **responses are gathered back inside the app**.

- **Backend** — Spring Boot 4 (Java 21) REST API, MongoDB, stateless JWT auth, GridFS for files,
  Google Forms API integration.
- **Frontend** — React SPA (JavaScript, Vite) with role-based navigation, in [`frontend/`](frontend).

---

## Roles

| Role | Can do |
| ---- | ------ |
| **ADMIN** | Create/edit/disable/delete users, assign mentees to mentors, view system stats. |
| **MENTOR** | See assigned mentees; record assessments; set & grade assignments; manage tasks; upload/download documents; create Google Forms and view gathered responses. |
| **MENTEE** | View own assessments, assignments (with file submission), tasks (update own status); upload/download own documents; fill out assigned forms. |

Route prefixes map to roles in Spring Security (`/api/admin/**`, `/api/mentor/**`, `/api/mentee/**`),
with service-layer ownership checks (a mentor only ever touches their own mentees; a mentee only their
own records).

---

## Prerequisites

- **Java 21+** (the project ships the Maven wrapper `./mvnw`)
- **Node.js 18+** and npm (for the frontend)
- **Docker** (easiest way to run MongoDB) — or a local MongoDB on `:27017`

---

## Quick start

### 1. Start MongoDB
```bash
docker compose up -d
```
This runs `mongo:8` on `localhost:27017` with database `mentortrack` (see [docker-compose.yml](docker-compose.yml)).

### 2. Start the backend (port 8080)
```bash
./mvnw spring-boot:run
```
On first launch a **DataInitializer** seeds an admin account (and, unless disabled, a demo mentor +
mentee). Watch the log for `Started MentorMenteeApplication`.

### 3. Start the frontend (port 5173)
```bash
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**. The Vite dev server proxies `/api` and `/google` to the backend on
`:8080`, so there are no CORS issues in development.

### 4. Log in

| Role | Email | Password |
| ---- | ----- | -------- |
| Admin | `admin@mentortrack.local` | `admin123` |
| Mentor *(demo)* | `mentor@mentortrack.local` | `mentor123` |
| Mentee *(demo)* | `mentee@mentortrack.local` | `mentee123` |

> Change the admin credentials via `ADMIN_EMAIL` / `ADMIN_PASSWORD`, and set `SEED_DEMO_DATA=false`
> to skip the demo mentor/mentee.

---

## Configuration

All settings live in [`src/main/resources/application.properties`](src/main/resources/application.properties)
and are overridable via environment variables:

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `MONGODB_URI` | `mongodb://localhost:27017/mentortrack` | MongoDB connection |
| `JWT_SECRET` | *(dev placeholder)* | HMAC key for signing JWTs — **set a long random value in production** |
| `JWT_TTL_MINUTES` | `720` | Token lifetime |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Allowed CORS origin |
| `FRONTEND_BASE_URL` | `http://localhost:5173` | Where the Google callback redirects back to |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | `admin@mentortrack.local` / `admin123` / `Administrator` | Seeded admin |
| `SEED_DEMO_DATA` | `true` | Seed a demo mentor + mentee |
| `GOOGLE_FORMS_ENABLED` | `false` | Turn the Google Forms feature on |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | *(blank)* | OAuth client credentials |
| `GOOGLE_REDIRECT_URI` | `http://localhost:8080/api/google/callback` | OAuth redirect |
| `GOOGLE_SYNC_MINUTES` | `0` | `>0` enables the background response poller (every N minutes) |

### Google Forms
The Forms feature needs a one-time Google Cloud setup (project, enable Forms API, OAuth consent + test
users, Web client). Full walkthrough: **[GOOGLE_SETUP.md](GOOGLE_SETUP.md)**. Without it, the whole
app still works; only the Forms tab is inert.

---

## Architecture

```
Browser (React SPA :5173)
   │  fetch /api/**  (Authorization: Bearer <JWT>)
   ▼
Spring Boot REST API (:8080)
   ├── Spring Security (stateless) — JwtAuthFilter validates the token, sets ROLE_*
   ├── Controllers  /api/{auth,admin,mentor,mentee,google}/**
   ├── Services     ownership checks + business logic
   ├── MongoDB      users, assessments, assignments, tasks, forms, responses, google accounts
   ├── GridFS       uploaded document bytes
   └── RestClient ──► Google Forms API (create forms, pull responses)
```

- **Auth** — `POST /api/auth/login` returns a JWT; the SPA stores it in `localStorage` and sends it as
  a bearer token. `JwtService` signs/verifies with HMAC-SHA256 using only the JDK (no external JWT lib).
- **Documents** — stored in MongoDB **GridFS** via `GridFsTemplate`; metadata in a `DocumentMeta`
  collection. Assignment submissions reuse the same store.
- **Google** — the OAuth code flow is hand-rolled with a signed `state` (embeds the mentorId, so the
  server stays stateless); tokens are stored per mentor and refreshed on demand.

### Backend layout (`src/main/java/com/example/MentorMentee`)
```
model/       Mongo documents + enums (User, Assessment, Assignment, Task, DocumentMeta,
             MentorForm, FormResponseRecord, GoogleAccount, ...)
repository/  Spring Data MongoRepository interfaces
security/    JwtService, JwtAuthFilter, SecurityConfig, AuthUser, CustomUserDetailsService
config/      DataInitializer (seeding), GoogleProperties, MongoConfig (GridFsTemplate)
dto/         request/response records
service/     UserService, Assessment/Assignment/Task/Document services,
             GoogleOAuthService, GoogleFormsService, FormService, FormSyncScheduler
web/         REST controllers + GlobalExceptionHandler
```

### Frontend layout (`frontend/src`)
```
api.js               fetch wrapper (injects JWT, unwraps JSON, file downloads)
auth.jsx             auth context (login/logout, restores session via /api/auth/me)
components/          ProtectedRoute, Layout (navbar), ui.jsx (Modal, alerts, badges)
pages/Login.jsx
pages/admin/Users.jsx
pages/mentor/        Dashboard, MenteeDetail (+tabs/), Forms (+forms/ builder & responses)
pages/mentee/Dashboard.jsx
```

---

## REST API (summary)

| Method & path | Role | Purpose |
| ------------- | ---- | ------- |
| `POST /api/auth/login` · `GET /api/auth/me` | public / any | Authenticate; current user |
| `GET/POST/PUT/DELETE /api/admin/users…` | ADMIN | User CRUD |
| `PUT /api/admin/users/{id}/mentor` · `GET /api/admin/mentors` · `GET /api/admin/stats` | ADMIN | Assign mentor; lists; stats |
| `GET /api/mentor/mentees[/{id}]` | MENTOR | Assigned mentees + summaries |
| `…/mentees/{id}/assessments` · `/api/mentor/assessments/{id}` | MENTOR | Assessments CRUD |
| `…/mentees/{id}/assignments` · `/api/mentor/assignments/{id}/grade` | MENTOR | Assignments + grading |
| `…/mentees/{id}/tasks` · `/api/mentor/tasks/{id}[/status]` | MENTOR | Tasks CRUD + status |
| `…/mentees/{id}/documents` · `/api/mentor/documents/{id}/download` | MENTOR | Documents up/down/delete |
| `GET/POST /api/mentor/forms` · `/{id}[/sync]` | MENTOR | Create/list forms, view & sync responses |
| `GET /api/mentor/google/status` · `/connect` · `DELETE /api/mentor/google` | MENTOR | Google account linking |
| `GET /api/google/callback` | public | OAuth redirect target |
| `GET /api/mentee/{profile,assessments,assignments,tasks,documents,forms}` | MENTEE | Own data |
| `PUT /api/mentee/tasks/{id}/status` · `POST /api/mentee/assignments/{id}/submit` | MENTEE | Update task; submit file |

---

## Tests

```bash
./mvnw test
```

Ships focused unit tests for the bespoke security code (`JwtServiceTest`, `JwtAuthFilterTest`) —
HMAC sign/verify round-trips, JSON escaping, tampered/expired/wrong-secret rejection, and the auth
filter turning a token into a `ROLE_*` principal. They need **no** MongoDB or Google credentials, so
they run green anywhere. (Full end-to-end verification is done by running the app against Mongo as
described above.)

## Production build

```bash
# frontend static bundle → frontend/dist
cd frontend && npm run build

# backend jar → target/*.jar
./mvnw clean package
```

For a single-artifact deployment you can serve `frontend/dist` from any static host (or copy it into
the backend's `src/main/resources/static`) and point it at the API. Remember to set a strong
`JWT_SECRET`, real `FRONTEND_ORIGIN`/`FRONTEND_BASE_URL`, and serve over HTTPS.

---

## Notes & limitations

- OAuth tokens are stored **unencrypted** for simplicity — encrypt at rest before production use.
- The Google form itself is public-with-link; "assigning" a form just surfaces it on those mentees'
  dashboards inside MentorTrack.
- Deleting a form in MentorTrack removes the stored copy + responses, **not** the form in Google Forms.
