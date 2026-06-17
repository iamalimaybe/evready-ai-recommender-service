# EVReady AI Recommender Service

Catalog-backed AI recommendation service for EVReady Pakistan.

This service is planned as a separate backend that helps users compare EV options from the existing EVReady catalogue using practical usage needs such as budget, vehicle type, daily distance, charging access, city, and ownership priorities.

The core idea is simple: the AI can explain and rank options, but it must stay grounded in trusted EVReady catalogue data. It must not invent vehicles, prices, ranges, battery values, charger availability, dealer availability, or route guarantees.

## Status

Early project skeleton.

Current setup includes:

* Spring Boot service skeleton
* PostgreSQL local database setup
* Liquibase changelog setup
* Environment-based configuration
* Initial project planning documentation

Recommendation APIs, model execution, validation, persistence, and frontend integration are planned next.

## Tech Stack

* Java 17
* Spring Boot
* Gradle
* PostgreSQL
* Spring Data JPA
* Liquibase
* Bean Validation
* Lombok
* Actuator
* Docker Compose
* Ollama planned for local LLM execution

## Why This Is A Separate Service

The existing EVReady backend owns the stable product data and operational workflows:

* vehicles
* chargers
* brands
* charger types
* leads
* contact submissions
* reviews
* feedback
* admin workflows

This recommender service owns only recommendation-specific concerns:

* recommendation requests
* candidate filtering
* model prompting
* structured model output parsing
* validation
* recommendation run persistence
* audit history

Keeping this service separate avoids mixing stable production product operations with AI orchestration logic.

## Planned Architecture

```text
EVReady Frontend
  -> EVReady Backend
      - vehicle catalogue
      - charger directory
      - public forms
      - reviews and feedback
      - admin workflows

  -> EVReady AI Recommender Service
      - recommendation API
      - EVReady catalogue client
      - deterministic candidate filtering
      - LLM explanation and ranking
      - structured output validation
      - recommendation run storage
```

The existing EVReady backend remains the source of catalogue data.

The recommender service should not directly read the existing EVReady production database in version 1.

## Planned First Use Case

```text
Help me choose an EV from the EVReady catalogue.
```

Planned request inputs:

* vehicle type
* budget in PKR
* city
* daily distance in kilometers
* monthly distance in kilometers
* home charging availability
* solar availability
* primary use case
* family size
* user priority
* additional notes

Planned response:

* ranked EV recommendations
* match reasons
* tradeoffs
* facts used
* missing information
* warnings

## Data Trust Rules

The recommender must follow conservative EVReady data rules.

It must not claim:

* live charger availability
* guaranteed route feasibility
* dealer availability
* confirmed current prices
* EVReady field verification
* financing, insurance, tax, or legal advice
* production-scale recommendation accuracy

Vehicle and charger source-confidence labels should be treated as source-confidence signals, not proof that EVReady physically audited each record.

## Planned Validation Rules

Model output must be validated before it is returned as a valid recommendation.

Planned rules include:

* response status is required
* summary is required
* recommendations array is required
* missing information array is required
* warnings array is required
* every recommended vehicle ID must exist in the candidate list
* ranks must be unique
* facts used must reference allowed candidate fields only
* answered responses must include at least one recommendation
* insufficient-candidate responses must not include recommendations
* unsupported live charger, dealer availability, or field-verification claims must be rejected
* raw model output must be stored for audit

## Local Setup

Start PostgreSQL:

```bash
docker compose up -d
```

Run the app:

```bash
./gradlew bootRun
```

On Windows PowerShell:

```powershell
.\gradlew bootRun
```

Default local ports:

```text
Recommender service: 8081
Recommender PostgreSQL: 5435
Existing EVReady backend: 8080
Ollama: 11434
```

## Environment Variables

Example variables are documented in `.env.example`.

Common local values:

```text
DB_NAME=evready_recommender
DB_USER=evready_recommender
DB_PASSWORD=evready_recommender_pass
DB_PORT=5435

SERVER_PORT=8081

EVREADY_API_BASE_URL=http://localhost:8080

LLM_PROVIDER=OLLAMA
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3:4b
OLLAMA_TEMPERATURE=0
OLLAMA_NUM_PREDICT=512
OLLAMA_NUM_CTX=4096
```

Do not commit real secrets.

## Database And Liquibase

Liquibase master changelog:

```text
src/main/resources/db/db.changelog-master.xml
```

SQL changelog folder:

```text
src/main/resources/db/changelog/
```

Planned database tables:

* `recommendation_run`
* `recommendation_candidate_snapshot`
* `recommendation_result`

Candidate snapshots are important because EVReady catalogue data can change. A stored recommendation should remain explainable even if the source vehicle record changes later.

## Planned API

Initial planned endpoints:

```text
POST /api/v1/recommendations
GET /api/v1/recommendations/{id}
```

Optional local/demo endpoint:

```text
GET /api/v1/recommendations
```

## Non-Goals

Do not build these in the first version:

* cart
* checkout
* payments
* booking
* dealer management
* public user accounts
* live charger availability
* route guarantees
* financing advice
* insurance advice
* legal or tax advice
* fine-tuning
* RAG over documents
* embedding-based recommendation

## Documentation

Project planning:

```text
docs/PROJECT_PLAN.md
```

## Branch Strategy

* `main` is production-ready.
* `develop` is the integration/testing branch.
* Feature work should use short-lived `feature/*` branches created from `develop`.
* Pull request flow:

    * `feature/*` to `develop`
    * `develop` to `main`
* Avoid committing directly to `main`.
* Avoid committing directly to `develop` unless intentionally handling integration work.

## Portfolio Positioning

Safe description:

```text
Catalog-backed AI recommender service for EVReady Pakistan that ranks EV options using trusted vehicle facts, deterministic candidate filtering, structured LLM output, validation, and stored recommendation runs.
```

What this project is intended to demonstrate:

* AI integration with an existing deployed product
* separate service architecture
* catalog-backed recommendation
* deterministic filtering before model use
* structured LLM output
* output validation
* candidate evidence snapshots
* recommendation run persistence
* auditability
* conservative handling of uncertain product and charger data
