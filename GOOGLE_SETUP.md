# Google Forms Integration — Setup Guide

MentorTrack can create **real Google Forms** on a mentor's behalf and pull the responses back into
the app. This uses the Google Forms API with OAuth2 (authorization-code flow). Each mentor connects
their own Google account once; MentorTrack stores their tokens and refreshes them automatically.

The app runs perfectly well **without** this configured — the Forms page simply shows a
"not configured" banner and the rest of the app (mentees, assessments, assignments, tasks,
documents) works normally. Set this up only when you want the Forms feature.

---

## 1. Create a Google Cloud project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Click the project dropdown (top bar) → **New Project**. Name it e.g. `MentorTrack` → **Create**.
3. Make sure the new project is selected before continuing.

## 2. Enable the Google Forms API

1. Navigation menu → **APIs & Services → Library**.
2. Search for **Google Forms API** → open it → **Enable**.
   *(For reading the signed-in user's email we also use the standard `userinfo` endpoint, which needs
   no separate enablement.)*

## 3. Configure the OAuth consent screen

1. **APIs & Services → OAuth consent screen**.
2. User type: **External** → **Create**.
3. Fill in the required fields:
   - App name: `MentorTrack`
   - User support email: your email
   - Developer contact email: your email
   - Save and continue.
4. **Scopes**: click **Add or Remove Scopes** and add:
   - `https://www.googleapis.com/auth/forms.body` — create/edit forms
   - `https://www.googleapis.com/auth/forms.responses.readonly` — read responses
   - `openid`, `.../auth/userinfo.email` — identify the connected account
   - Save and continue.
5. **Test users**: while the app is in "Testing" mode, only listed test users can authorize it.
   Click **Add Users** and add every mentor's Google address (and your own). Save.

> You can leave the app in **Testing** mode for internal/college use. Publishing (and Google's
> verification) is only needed for a public production launch.

## 4. Create OAuth client credentials

1. **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
2. Application type: **Web application**.
3. Name: `MentorTrack Web`.
4. **Authorized redirect URIs** → **Add URI**:
   ```
   http://localhost:8080/api/google/callback
   ```
   (For a deployed backend, add that origin's callback too, e.g.
   `https://your-domain/api/google/callback`.)
5. **Create**. Copy the **Client ID** and **Client secret**.

## 5. Point MentorTrack at your credentials

Set these environment variables before starting the backend (names match
`src/main/resources/application.properties`):

| Variable                 | Value                                             |
| ------------------------ | ------------------------------------------------- |
| `GOOGLE_FORMS_ENABLED`   | `true`                                            |
| `GOOGLE_CLIENT_ID`       | *(the Client ID from step 4)*                     |
| `GOOGLE_CLIENT_SECRET`   | *(the Client secret from step 4)*                 |
| `GOOGLE_REDIRECT_URI`    | `http://localhost:8080/api/google/callback`       |
| `GOOGLE_SYNC_MINUTES`    | `0` (on-demand only) or e.g. `15` for auto-poll   |

**Windows (PowerShell):**
```powershell
$env:GOOGLE_FORMS_ENABLED="true"
$env:GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="xxxx"
./mvnw spring-boot:run
```

**macOS / Linux (bash):**
```bash
export GOOGLE_FORMS_ENABLED=true
export GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=xxxx
./mvnw spring-boot:run
```

## 6. Connect and use

1. Log in as a **mentor** → **Forms** tab.
2. Click **Connect Google** → complete Google's consent screen (use a test-user account).
3. You're redirected back to the app; the banner turns green.
4. **Create form** → build questions → assign to mentees → **Create in Google Forms**.
5. Share the form's **Open** link (or the mentee sees it under their **Forms** tab).
6. After people submit, click **Responses** (auto-syncs) or the **Sync** button to pull submissions
   into the app.

---

## How the flow works (for the curious)

- **Connect** → backend builds a Google auth URL with an HMAC-signed `state` that embeds the mentorId
  (so no server session is needed) and redirects the browser to Google.
- **Callback** (`/api/google/callback`, public) → backend verifies `state`, exchanges the `code` for
  access + refresh tokens, fetches the account email, stores a `GoogleAccount`, and 302s back to the
  frontend `…/mentor/forms?google=connected`.
- **Create** → `forms.create` (title) then `:batchUpdate` (description + one `createItem` per question);
  the Google-assigned question IDs are read back and stored on the `MentorForm`.
- **Sync** → `GET forms/{id}/responses` (paged), deduped by Google's `responseId`, mapped to question
  titles, and saved as `FormResponseRecord`s.
- Access tokens are refreshed automatically when within 60s of expiry using the stored refresh token.

## Troubleshooting

| Symptom | Likely cause / fix |
| ------- | ------------------ |
| Banner says "not configured" | `GOOGLE_FORMS_ENABLED` not `true`, or client id/secret blank. |
| `redirect_uri_mismatch` on Google | The redirect URI in step 4 must **exactly** match `GOOGLE_REDIRECT_URI` (scheme, host, port, path). |
| `403 access_denied` | The Google account isn't in the **Test users** list (step 3.5). |
| Connected but "Create" fails | Confirm the **Google Forms API** is enabled (step 2) and the `forms.body` scope was granted. |
| No refresh token after reconnect | The app requests `access_type=offline&prompt=consent`, which forces a refresh token; if you removed the app's access in your Google account, reconnect. |

> **Security note:** for simplicity this project stores OAuth tokens **in plaintext** in MongoDB
> (`GoogleAccount`). Before any real production use, encrypt them at rest (or use a secrets manager)
> and serve everything over HTTPS.
