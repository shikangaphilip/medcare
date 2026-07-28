# Running the Philmed Website

Two pieces make up the site:

- **`philmed.html`** — the frontend. A single static file (HTML/CSS/JS).
- **`philmed-backend/`** — the Spring Boot API (Java + MySQL) it talks to.

They're two separate downloads in this chat; put them in the same project
folder however you'd like — nothing about their location matters, only that
the backend is running and reachable at the URL the frontend is configured
to call.

## 1. Start the backend first

```bash
cd philmed-backend
# edit src/main/resources/application.properties: MySQL credentials + JWT secret
mvn spring-boot:run
```

It starts on `http://localhost:8080` and seeds 14 sample doctors on first
run. Leave this running.

## 2. Open the frontend

Just open `philmed.html` in a browser (double-click it, or serve it with
any static server — e.g. `npx serve` or the VS Code "Live Server" extension).

The frontend is hard-coded to call the backend at:

```js
const API_BASE = 'http://localhost:8080/api';
```

near the top of the `<script>` block in `philmed.html`. If you run the
backend on a different host/port, or deploy the frontend to a real domain,
update that one line to match — and update
`philmed.cors.allowed-origins` in `application.properties` to allow that
frontend's origin (it defaults to `*`, which is fine for local testing but
should be locked down before you go live).

## 3. Try it end-to-end

1. Click **Sign In** in the nav → **Create Account** → register.
2. Click **Book Appointment** → pick a care type (and specialty, if
   applicable) → a real doctor from the seeded list appears in **Choose
   Doctor** → pick a date/time → **Confirm Appointment Request**.
3. Click **My Appointments** (next to your name in the nav) to see it
   listed with its status.

If step 2's doctor list ever says "No doctors available right now," it
means the frontend couldn't reach the backend (check it's running, check
the browser console for a CORS or network error, and check `API_BASE`
matches where it's actually running).

## What's genuinely NOT included

Being upfront about the gap between "runs" and "production-ready":

- **No HTTPS, no real domain, no deployment config** — both pieces assume
  `localhost` today. Deploying either one (a VPS, Render, Railway, etc.) is
  a separate step with its own setup.
- **No email/SMS** — "we'll contact you within 30 minutes" in the UI is
  aspirational copy; nothing actually sends a message. Wiring that up needs
  a provider (e.g. Twilio, SendGrid) and its own credentials.
- **No doctor login** — doctors are data records an admin manages; they
  don't have their own accounts to check their own schedule yet.
- **No database migrations** — schema changes rely on Hibernate's
  `ddl-auto=update`, fine for a prototype, not for a team shipping changes
  over time.
- **No automated tests.** Everything here was checked by hand (brace
  balance, cross-referenced IDs/functions, careful reading of the Spring/JWT
  APIs) since this environment has no Java compiler or Maven — there's no
  substitute for actually running `mvn clean install` and clicking through
  it yourself.
