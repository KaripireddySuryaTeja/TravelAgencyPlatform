# AI-Powered Feedback Moderation – Technical Documentation

## Overview

Every user feedback submission passes through an AI-powered content moderation
gate powered by **Azure OpenAI** before it is saved to the database.

Moderation is **synchronous and blocking**: if a piece of content fails moderation,
it is never persisted.

---

## Architecture

```
HTTP POST /api/v1/bookings/{id}/feedback
          │
          ▼
  FeedbackController.submitFeedback()
          │
          ▼
  FeedbackService.submitFeedback()
     │  1. Validate booking state & ownership
     │  2. Validate rating / comment business rules
     │  3. Check for duplicate submission
     │  4. ▶ Call ModerationService.moderate(comment)
     │         │
     │         ▼
     │   ModerationServiceImpl
     │   (Spring @Service)
     │         │
     │         ▼
     │   AiModerationService
     │   (package-private POJO)
     │         │
     │         ▼
     │   Azure OpenAI Chat Completions API
     │   Tool: submit_moderation
     │   Returns: { "status": "APPROVED|NEEDS_EDIT|FLAGGED", "reason": "..." }
     │         │
     │    ┌────┴──────────────────────────┐
     │  APPROVED         NEEDS_EDIT        FLAGGED
     │    │               │                 │
     │  save()      throw             throw
     │  APPROVED    ContentModeration  FeedbackRejected
     │              Exception (422)    Exception (422)
     ▼
  feedbackRepository.save(feedback)  ← only when APPROVED
  eventPublisher.publishEvent(...)
```

---

## Classes

| Class | Package | Role |
|---|---|---|
| `ModerationService` | `service.moderation` | Interface – single `moderate(String)` method |
| `ModerationServiceImpl` | `service.moderation` | Spring `@Service` – error handling, logging, delegates to `AiModerationService` |
| `AiModerationService` | `service.moderation` | Package-private POJO – raw Azure OpenAI SDK interaction |
| `ModerationResult` | `service.moderation` | Immutable record: `(ModerationStatus status, String reason)` |
| `ModerationStatus` | `enums` | `APPROVED`, `NEEDS_EDIT`, `FLAGGED` |
| `AzureOpenAiConfiguration` | `config` | Spring `@Configuration` – creates `OpenAIClient` bean, reads env vars |
| `FeedbackRejectedException` | `exception` | Thrown for `FLAGGED` content |
| `ContentModerationException` | `exception` | Thrown for `NEEDS_EDIT` or service failure |

---

## Azure OpenAI Tool: `submit_moderation`

The AI model is forced to call the `submit_moderation` function
(`toolChoice = Named("submit_moderation")`).

### Tool schema

```json
{
  "type": "object",
  "properties": {
    "status": {
      "type": "string",
      "enum": ["APPROVED", "FLAGGED", "NEEDS_EDIT"],
      "description": "The moderation decision for the provided content."
    },
    "reason": {
      "type": "string",
      "description": "A concise, human-readable explanation for the moderation decision."
    }
  },
  "required": ["status", "reason"],
  "additionalProperties": false
}
```

### Model settings

| Setting | Value |
|---|---|
| Model | `gpt-4.1-mini-2025-04-14` (configurable) |
| Temperature | `0.0` (deterministic) |
| Tool choice | `required` – always calls `submit_moderation` |

---

## Moderation Rules

### APPROVED
- Constructive and respectful feedback
- Mild profanity used only for emphasis (e.g. *"damn expensive"*)
- Minor spelling errors
- Controversial topics discussed respectfully
- Good-faith criticism

### NEEDS_EDIT
- Excessive or repeated profanity
- Largely incomprehensible text
- Content that can be meaningfully improved and resubmitted

### FLAGGED (permanent rejection)
- Hate speech, harassment, discrimination
- Threats of violence or harm
- Explicit sexual content
- Doxxing / sharing of private information
- Promotion of illegal activities
- Aggressive targeted abuse
- Spam or off-topic promotional content

---

## HTTP Response Reference

| Scenario | HTTP Status | Response Body |
|---|---|---|
| Content approved | 201 Created | `FeedbackResponse` JSON |
| Content needs revision | 422 Unprocessable Entity | `moderationStatus: "NEEDS_EDIT"`, `message: "Please revise..."` |
| Content rejected | 422 Unprocessable Entity | `moderationStatus: "FLAGGED"`, `moderationReason: "..."` |
| Moderation service unavailable | 503 Service Unavailable | `moderationStatus: "UNAVAILABLE"`, `message: "Try again later"` |

**Stack traces are never exposed in HTTP responses.**

---

## Security

- The Azure OpenAI API key is read exclusively from `AZURE_OPENAI_API_KEY` env var.
- The default placeholder `REPLACE_WITH_SECRET` is intentionally non-functional.
- In production (Kubernetes / KubeRocketCI), the key is injected from a Kubernetes
  Secret – see `deploy-templates/kubernetes/azure-openai-secret.yaml`.

---

## Configuration

| Property | Env var | Default |
|---|---|---|
| `app.moderation.ai.endpoint` | `AZURE_OPENAI_ENDPOINT` | `https://your-resource.openai.azure.com/` |
| `app.moderation.ai.api-key` | `AZURE_OPENAI_API_KEY` | `REPLACE_WITH_SECRET` |
| `app.moderation.ai.deployment` | `AZURE_OPENAI_DEPLOYMENT` | `gpt-4.1-mini-2025-04-14` |
| `app.moderation.ai.timeout-seconds` | `AZURE_OPENAI_TIMEOUT_SECONDS` | `30` |

---

## Error Handling Policy

| Error | Cause | Behavior |
|---|---|---|
| `HttpResponseException` 401/403 | Invalid API key | `ContentModerationException(FLAGGED)` – content blocked |
| `HttpResponseException` 429 | Rate limit exceeded | `ContentModerationException(UNAVAILABLE)` – HTTP 503 |
| `HttpResponseException` 5xx | Azure service down | `ContentModerationException(UNAVAILABLE)` – HTTP 503 |
| `IllegalStateException` | JSON parse failure | `ContentModerationException(INVALID_RESPONSE)` – HTTP 503 |
| Generic `Exception` | Unexpected error | `ContentModerationException(ERROR)` – HTTP 503 |

> **Important:** Content is **never published** when moderation cannot be completed.

---

## Responsible AI

- Every moderation decision includes an AI-generated reason.
- Constructive criticism is never rejected.
- Rules are applied consistently (temperature=0 ensures determinism).
- Decisions are transparent and explainable to the submitting user.
- The system does not perform PII collection or profiling.

---

## Running Tests

```bash
# Run only moderation tests
mvn test -pl backend/sprint1 -Dtest="ModerationServiceImplTest,FeedbackServiceTest"

# Run all tests
mvn test -pl backend/sprint1
```

