# Travel Agency Frontend

React single-page app for the Travel Agency Platform.

## Setup

```bash
cp .env.example .env.local   # then fill in your own values
npm install
npm start
```

| Variable | Description |
|---|---|
| `REACT_APP_API_BASE_URL` | Backend base URL (default `http://localhost:8080`) |
| `REACT_APP_RECAPTCHA_SITE_KEY` | Your own Google reCAPTCHA v2 **site** key |

`.env.local` is git-ignored. Never commit real keys.
