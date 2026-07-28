# Philmed Backend

Spring Boot 3 / Java 21 / MySQL backend for the Philmed booking app.
Designed to run alongside `philmed.html` (the frontend) — see the root-level
`RUNNING-THE-SITE.md` for how the two fit together.

## Why the files are organized this way

Java only allows **one public top-level class per file**, so the only way to
truly merge classes together — while still letting them be used from other
packages — is to nest them inside one outer class per domain. That's what's
here: each domain (user, doctor, appointment, auth, security, emergency,
dashboard) is **one file** containing its entity/DTOs, repository, service,
and controller as nested classes, plus one small shared file for error
handling.

```
com.philmed
├── PhilmedApplication.java        entry point + startup doctor seeding
├── config/SecurityConfig.java     security filter chain + CORS + JWT utility + JWT filter
├── user/User.java                 entity + User.Repository
├── doctor/Doctor.java             entity + Doctor.Repository + Doctor.Service + Doctor.Controller
├── appointment/Appointment.java   entity + repository + service + response/request DTOs + controller
├── auth/Auth.java                 DTOs + Auth.Service + Auth.Controller (register/login)
├── emergency/Emergency.java       Emergency.Controller (public, no auth)
├── dashboard/Dashboard.java       Dashboard.Controller (admin-only counts)
└── common/ErrorHandling.java      one place that turns exceptions into clean JSON
```

References look like `Doctor.Service`, `Appointment.Repository`,
`Auth.Controller` rather than separate class names — that's expected, it's
how nested classes are addressed in Java.

## What this pass added: making it actually work end-to-end

Everything up to this point was two correct pieces that had never spoken to
each other. This pass connects them:

1. **CORS.** `philmed.html` runs from a different origin than the API
   (`file://` or a static server, vs. `localhost:8080`). Without CORS
   enabled, every `fetch()` call from the frontend would be silently
   blocked by the browser before it ever reached a controller. Added a
   `CorsConfigurationSource` bean in `SecurityConfig`, wired into the
   filter chain, configurable via `philmed.cors.allowed-origins`.
2. **Startup data seeding.** A brand-new database has zero doctors, so the
   booking form would have nothing to book. `PhilmedApplication` now seeds
   14 sample doctors (one per specialty the frontend offers, plus two GPs)
   the first time it runs — skipped automatically if the table already has
   rows.

See the audit notes further down for the earlier pass (password leakage,
lazy-loading crash, missing validation) — those fixes are already included
in the files here.

## Setup

1. **Create the database** (or let it auto-create — see below):
   ```sql
   CREATE DATABASE philmed;
   ```
2. **Edit `src/main/resources/application.properties`**:
   - Set `spring.datasource.username` / `spring.datasource.password` to your MySQL credentials.
   - Replace `philmed.jwt.secret` with your own random 32+ character string.
   - Leave `philmed.cors.allowed-origins=*` for local development; set it to
     your real frontend URL(s) before deploying.
3. **Run it**:
   ```bash
   mvn spring-boot:run
   ```
   Hibernate creates the `users`, `doctors`, and `appointments` tables on
   first run, and 14 sample doctors are inserted automatically.

### Creating your first admin account

`POST /api/auth/register` always creates a `PATIENT`. To create the first
admin (needed to add doctors or view `/api/dashboard/stats`), register
normally through the site, then promote that account directly in MySQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

## API overview

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create a patient account, returns a JWT |
| POST | `/api/auth/login` | none | Log in, returns a JWT |
| GET | `/api/doctors` | none | List all doctors |
| GET | `/api/doctors/available` | none | List only currently available doctors |
| GET | `/api/doctors/specialty/{specialty}` | none | Filter by specialty |
| POST / PUT / DELETE | `/api/doctors/**` | ADMIN | Manage doctor records |
| POST | `/api/appointments` | Bearer token | Book an appointment |
| GET | `/api/appointments/my` | Bearer token | The logged-in patient's appointments |
| GET | `/api/appointments/doctor/{doctorId}` | Bearer token | A doctor's schedule |
| PUT | `/api/appointments/{id}/status` | Bearer token | Update status (confirm/complete) |
| DELETE | `/api/appointments/{id}` | Bearer token | Cancel an appointment |
| GET | `/api/emergency/contacts` | none | Emergency phone numbers |
| GET | `/api/dashboard/stats` | ADMIN | Patient/doctor/appointment counts |

Send the JWT from login/register as `Authorization: Bearer <token>` on
every protected request. The frontend does this automatically once you're
signed in.

## Audit notes from the previous pass (still relevant, already fixed here)

1. **Password hashes were one bad response away from leaking.** Fixed with
   an `AppointmentResponse` DTO that only exposes safe fields, plus
   `@JsonIgnore` on `User.getPassword()`.
2. **`LAZY` fetch would have thrown `LazyInitializationException`** the
   first time a controller touched `patient`/`doctor` after the repository
   call returned. Switched both to `EAGER`.
3. **Validation dependency was installed but unused.** Registration, login,
   doctor creation, and booking now validate with `@NotBlank`, `@Email`,
   `@FutureOrPresent`, etc.

## Notes / things to double-check before production

- This was written directly (no compiler available in this environment) —
  run `mvn clean install` locally. Every file was manually checked for
  balanced braces, no duplicate methods, and consistent imports/references,
  but that's not a substitute for an actual build.
- `spring.jpa.hibernate.ddl-auto=update` and `philmed.cors.allowed-origins=*`
  are both convenient for development only. Switch to a migration tool
  (Flyway/Liquibase) and a specific origin list before production.
- The JWT secret and emergency phone numbers are placeholders — replace
  them with real values.
- `/api/appointments/doctor/{doctorId}` currently only requires *some*
  logged-in user, not specifically that doctor — fine for now since doctors
  don't have their own login yet, but worth tightening if you add one.
