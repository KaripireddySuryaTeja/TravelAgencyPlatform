# Travel Agency Platform

A full-stack web platform for browsing, booking and managing guided tours. Customers discover tours and book trips, travel agents verify documents and manage bookings, and administrators moderate feedback and analyse sales and staff performance.

**Stack:** Java 17 · Spring Boot 3.5 · MongoDB · React 18 · JWT · OAuth2 · RabbitMQ · AWS S3 · Azure OpenAI

---

## Table of contents

- [Scope](#scope)
- [Features](#features)
- [Architecture](#architecture)
- [Booking lifecycle](#booking-lifecycle)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Configuration (bring your own keys)](#configuration-bring-your-own-keys)
- [API overview](#api-overview)
- [Testing](#testing)
- [Deployment](#deployment)
- [Project structure](#project-structure)
- [Security notes](#security-notes)
- [License](#license)

## Scope

The platform covers the full journey of a tour booking:

1. **Discovery**: search destinations, filter available tours, read details and approved reviews.
2. **Booking**: choose a date, duration and meal plan, add guests, and submit personal details.
3. **Paperwork**: upload passport and payment confirmation PDFs, which an agent verifies.
4. **Fulfilment**: agents confirm, edit or cancel bookings, and a booking moves through a defined state machine.
5. **Feedback**: customers review their tours, and AI-assisted moderation screens every review before it is published.
6. **Insight**: admins get sales, location and staff-performance reports, plus an AI report analyser for uploaded spreadsheets and PDFs.

Out of scope: online payment processing (payment is evidenced by an uploaded confirmation document) and a native mobile app.

## Features

### Accounts and security
- Sign-up with an emailed verification code, and a live check for already-registered emails.
- JWT sign-in (24 h token lifetime) with automatic account lockout after 5 failed attempts (15 min).
- Password reset by emailed one-time code, with enforced password-strength rules.
- Social login with **Google** and **Facebook** (OAuth2).
- Optional Google **reCAPTCHA v2** on sensitive forms.
- Profile management: name, avatar, password, and email change with a confirmation token.
- Role-based access control: `CUSTOMER`, `TRAVEL_AGENT`, `ADMIN`.

### Tours and bookings
- Tour catalogue with destinations, start dates, durations, per-duration pricing, meal plans, hotel details and capacity tracking.
- Bookings with adults and children, per-guest personal details and meal supplements.
- Edit-then-confirm flow for changes, and cancellation with a configurable free-cancellation window.
- Agent dashboard for managing bookings (edit, cancel with a reason, verify documents, confirm).
- Optional **RabbitMQ** publishing of booking state-change events.

### Travel documents
- PDF-only uploads with size, extension and MIME-type validation.
- Optional **ClamAV** malware scanning.
- **AES-256 encryption at rest** for stored documents.
- Pluggable storage back-ends: local disk, MongoDB, or **AWS S3**.
- Automatic retention clean-up: unverified documents expire, and cancelled bookings have their documents removed.

### Reviews and moderation
- Customers leave a rating and comment on their bookings.
- **Azure OpenAI** moderation returns `APPROVED`, `NEEDS_EDIT` (ask the user to revise) or `FLAGGED` (rejected).
- Admin moderation page to review feedback and toggle review visibility.

### Admin analytics
- Sales, staff-performance and location reports with charts.
- Export to PDF and Excel from the browser.
- **Report Analyzer**: upload CSV, Excel or PDF reports and get an AI-generated analysis with chart data.

## Architecture

```
┌────────────────────┐   REST + JWT    ┌──────────────────────────┐
│  React SPA         │ ─────────────▶  │  Spring Boot API         │
│  (Frontend/)       │ ◀─────────────  │  (Backend/)              │
└────────────────────┘                 │  controller → service →  │
                                       │  repository              │
                                       └───┬──────┬──────┬────────┘
                                           │      │      │
                                     MongoDB   S3/local  RabbitMQ
                                               ClamAV    Azure OpenAI
                                               SMTP      Google/Facebook OAuth
```

The backend follows a layered design (controllers → services → Spring Data repositories) with a global exception handler that returns consistent JSON errors.

## Booking lifecycle

```
BOOKED ──▶ DOCUMENTS_VERIFIED ──▶ CONFIRMED ──▶ STARTED ──▶ FINISHED
   │               │                   │
   └───────────────┴───────────────────┴──▶ CANCELLED
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5 (Web, Security, Validation, Data MongoDB, Mail, AMQP, OAuth2 Client) |
| Auth | JJWT 0.12, BCrypt, Spring OAuth2 |
| Data | MongoDB (in-memory option for local development) |
| Storage | AWS SDK v2 (S3), local disk, MongoDB |
| AI | Azure OpenAI SDK |
| Docs / files | springdoc OpenAPI, Apache POI, PDFBox, Commons CSV |
| Frontend | React 18, React Router 6, Axios, Recharts, jsPDF, SheetJS, react-dropzone |
| Tooling | Maven, Docker, Helm / Kubernetes |

## Getting started

### Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 18+ and npm
- MongoDB (local or Atlas), or use the in-memory `mock-mongo` profile below

### 1. Configure the backend

Create a `.env` file in `Backend/travel-agency-backend/` (it is git-ignored and picked up automatically) from the template:

```bash
cd Backend/travel-agency-backend
cp .env.example .env      # then fill in your own values
```

At minimum set `MONGO_URI`, `JWT_SECRET`, `DOCUMENTS_ENCRYPTION_KEY_BASE64`, the mail settings and the Google OAuth client. Generate the encryption key with:

```bash
openssl rand -base64 32
```

### 2. Run the backend

```bash
mvn spring-boot:run
# or, without installing MongoDB:
mvn spring-boot:run -Dspring-boot.run.profiles=mock-mongo
```

The API is served on `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

On first start the app seeds an admin user, three agents, sample tours, reviews and bookings. Passwords come from `SEED_ADMIN_PASSWORD` and `SEED_AGENT_PASSWORD`. If they are unset, a random password is generated and printed once in the startup log.

### 3. Run the frontend

```bash
cd Frontend/travel-agency-frontend
cp .env.example .env.local
npm install
npm start
```

The app opens on `http://localhost:3000`.

## Configuration (bring your own keys)

No credentials are stored in this repository. Supply your own through environment variables:

| Service | Where to get it | Variables |
|---|---|---|
| MongoDB | Local install or Atlas | `MONGO_URI` |
| JWT signing | Any random string of 32+ characters | `JWT_SECRET` |
| Document encryption | `openssl rand -base64 32` | `DOCUMENTS_ENCRYPTION_KEY_BASE64` |
| SMTP (verification and reset emails) | e.g. Gmail app password | `MAIL_USERNAME`, `MAIL_PASSWORD` |
| Google OAuth | Google Cloud Console | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |
| Facebook OAuth (optional) | Meta for Developers | `FACEBOOK_CLIENT_ID`, `FACEBOOK_CLIENT_SECRET` |
| reCAPTCHA v2 (optional) | google.com/recaptcha | `CAPTCHA_ENABLED`, `CAPTCHA_SITE_KEY`, `CAPTCHA_SECRET_KEY`; frontend: `REACT_APP_RECAPTCHA_SITE_KEY` |
| AWS S3 (optional) | AWS IAM | `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_SESSION_TOKEN`, `AWS_REGION`, `AWS_S3_BUCKET` |
| Azure OpenAI (optional) | Azure portal | `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT` |
| RabbitMQ (optional) | Local or hosted | `APP_RABBITMQ_ENABLED`, `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` |
| ClamAV (optional) | Local or hosted | `CLAMAV_ENABLED`, `CLAMAV_HOST`, `CLAMAV_PORT` |

The complete list is in [`Backend/travel-agency-backend/.env.example`](Backend/travel-agency-backend/.env.example) and [`Frontend/travel-agency-frontend/.env.example`](Frontend/travel-agency-frontend/.env.example).

## API overview

Interactive documentation is available at `/swagger-ui.html` (OpenAPI JSON at `/api-docs`).

| Area | Base path | Highlights |
|---|---|---|
| Auth | `/api/v1/auth` | `sign-up`, `sign-in`, `me`, `check-email`, `password-reset/*`, `email-verification/*`, `login-url/{provider}` |
| Users | `/api/v1/users` | name, image, password, email change and confirm |
| Tours | `/tours` | `destinations`, `available`, `{id}`, `{id}/reviews` |
| Bookings | `/api/v1/bookings` | create, list, update, `cancel`, `confirm-changes`, `verify-documents`, `confirm` |
| Documents | `/api/v1/bookings/{id}/documents` | `upload`, list, `download`, delete |
| Feedback | `/api/v1/bookings/{id}/feedback`, `/api/v1/tours/{id}/feedback` | create, update, read |
| Admin | `/api/v1/admin` | review listing and visibility |
| Reports | `/api/v1/reports`, `/api/reports` | `sales`, `staff-performance`, `locations`, `analyze` |

Reports and admin endpoints require the `ADMIN` role. Booking, document and user endpoints require authentication.

## Testing

```bash
cd Backend/travel-agency-backend
mvn test
```

The backend suite covers authentication, security configuration, JWT handling, sign-up and sign-in services, email verification, and the booking and feedback controllers.

## Deployment

- **Docker:** build the jar with `mvn package`, then build the image from `Backend/travel-agency-backend/Dockerfile`.
- **Kubernetes / Helm:** charts and templates live in `Backend/travel-agency-backend/deploy-templates`. The chart expects a secret named `travel-agency-secret` holding the keys referenced in `values.yaml`, and an optional `azure-openai-secret` (see `deploy-templates/kubernetes/`). Hostnames in the templates are `example.com` placeholders.
- **Frontend:** `npm run build` produces a static bundle in `build/` that can be hosted on any static host (S3, Netlify, Nginx, etc.). Set `REACT_APP_API_BASE_URL` at build time.

## Project structure

```
.
├── Backend/travel-agency-backend/
│   ├── src/main/java/.../controller    REST controllers
│   ├── src/main/java/.../service       business logic (moderation, storage, security, reports)
│   ├── src/main/java/.../repository    Spring Data MongoDB repositories
│   ├── src/main/java/.../security      JWT filter, OAuth2 handlers
│   ├── src/main/resources              application.properties
│   ├── seed-data/                      sample agents, tours and reviews
│   └── deploy-templates/               Helm chart and Kubernetes manifests
└── Frontend/travel-agency-frontend/
    └── src/{pages,components,api,auth,hooks}
```

## Security notes

- Secrets are read from the environment only. `.env` files are git-ignored.
- Passwords are hashed with BCrypt, and uploaded documents are encrypted with AES-256.
- There is no default encryption key and no default admin password.
- If you fork this project, rotate any credential that has ever been committed anywhere.

## License

Released under the [MIT License](LICENSE). Copyright © 2026 Karipireddy Surya Teja.
