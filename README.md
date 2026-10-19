# Travel Agency Platform

Full-stack travel agency app: a Spring Boot (Java 17, MongoDB) backend and a React frontend.

```
Backend/sprint1/                       Spring Boot API
Frontend/travel-agency-frontend/       React app
```

## Bring your own keys

No credentials are stored in this repo. You supply your own:

| Service | Where to get it | Config |
|---|---|---|
| MongoDB | local install or Atlas | `MONGO_URI` |
| JWT signing | any random 32+ char string | `JWT_SECRET` |
| Document encryption | `openssl rand -base64 32` | `DOCUMENTS_ENCRYPTION_KEY_BASE64` |
| SMTP (password reset emails) | e.g. Gmail app password | `MAIL_USERNAME`, `MAIL_PASSWORD` |
| Google OAuth | Google Cloud Console | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |
| reCAPTCHA v2 (optional) | google.com/recaptcha | `CAPTCHA_*` (backend), `REACT_APP_RECAPTCHA_SITE_KEY` (frontend) |
| AWS S3 (optional) | AWS IAM | `AWS_*` |
| Azure OpenAI (optional, content moderation) | Azure portal | `AZURE_OPENAI_*` |

See [Backend/sprint1/.env.example](Backend/sprint1/.env.example) for the full list and
[Frontend/travel-agency-frontend/.env.example](Frontend/travel-agency-frontend/.env.example) for the frontend.

Seeded admin/agent passwords come from `SEED_ADMIN_PASSWORD` / `SEED_AGENT_PASSWORD`;
if unset, a random one is generated and printed in the startup log.

## Run locally

```bash
# backend (after exporting the variables above)
cd Backend/sprint1 && mvn spring-boot:run

# frontend
cd Frontend/travel-agency-frontend && cp .env.example .env.local && npm install && npm start
```

## Kubernetes / Helm

`Backend/sprint1/deploy-templates` expects a secret named `travel-agency-secret` containing the keys
referenced in `values.yaml`. Hostnames there are `example.com` placeholders.
