# EVReady AI Recommender Service

Catalog-backed AI recommendation service for EVReady Pakistan.

This service helps users compare EV options from the existing EVReady catalogue using practical needs such as budget, vehicle type, daily distance, charging access, city, family use, and ownership priorities.

The core rule is simple: the model can explain and rank options, but it must stay grounded in trusted EVReady catalogue data. It must not invent vehicles, prices, ranges, battery values, charger availability, dealer availability, route feasibility, or field-verification claims.

## Current Status

EVReady AI Recommender Service is an early MVP backend service.

Implemented so far:

* Spring Boot service skeleton
* PostgreSQL persistence with Liquibase migrations
* EVReady backend catalogue client
* deterministic candidate selection
* Ollama model generation client
* structured recommendation prompt
* strict JSON model-output parsing
* deterministic facts-used enrichment
* safety and traceability validation
* recommendation run persistence
* candidate snapshot persistence
* recommendation result persistence
* stored response metadata
* create recommendation endpoint
* retrieve recommendation endpoint
* candidate selection endpoint
* consistent API error responses
* manual smoke scripts
* focused unit tests for validation and enrichment
* GitHub Actions build workflow

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
* Ollama
* qwen3:4b for local model execution

## Why This Is A Separate Service

The existing EVReady backend owns stable product and operational workflows:

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
* deterministic enrichment
* validation
* recommendation run persistence
* audit history

Keeping this service separate avoids mixing stable product operations with AI orchestration logic.

## Architecture

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
      - structured output parsing
      - deterministic output enrichment
      - safety validation
      - recommendation run storage
```

The existing EVReady backend remains the source of catalogue data.

The recommender service does not directly read the existing EVReady production database in version 1.

## Main Use Case

```text
Help me choose an EV from the EVReady catalogue.
```

Supported request inputs:

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

Response includes:

* recommendation run ID
* status
* summary
* ranked EV recommendations
* match reasons
* tradeoffs
* facts used
* missing information
* warnings
* validation status
* failure reason if the response could not be completed safely

## API Endpoints

### Select Candidates

```http
POST /api/v1/recommendations/candidates
```

Runs deterministic catalog-backed candidate selection without calling the LLM.

This is useful for debugging the candidate pool before model ranking.

### Create Recommendation

```http
POST /api/v1/recommendations
```

Runs the full recommendation flow:

1. load EV catalogue data from EVReady backend
2. select deterministic candidates
3. store recommendation run
4. store candidate snapshots
5. build prompt
6. call Ollama
7. parse model JSON
8. enrich facts used
9. validate output
10. store recommendation results
11. store response metadata
12. return validated response

### Get Recommendation

```http
GET /api/v1/recommendations/{id}
```

Returns a stored recommendation run with persisted recommendation results, warnings, missing information, validation status, and failure reason.

## Example Request

```json
{
  "vehicleType": "CAR",
  "budgetPkr": 9000000,
  "city": "Lahore",
  "dailyDistanceKm": 45,
  "monthlyDistanceKm": 1200,
  "homeChargingAvailable": true,
  "solarAvailable": false,
  "primaryUseCase": "family commute",
  "familySize": 4,
  "priority": "longer range",
  "additionalNotes": "Occasional Lahore to Islamabad travel."
}
```

## Example Response Shape

```json
{
  "id": 11,
  "status": "ANSWERED",
  "summary": "Three EVs within budget with DC fast charging support; range covers daily commute but longer trips require separate checks.",
  "recommendations": [
    {
      "vehicleId": 66,
      "rank": 1,
      "matchReason": "Price within budget, range covers daily commute (45km), supports DC fast charging.",
      "tradeoffs": [
        "Unverified catalogue data; range may vary in real-world use."
      ],
      "factsUsed": [
        "pricePkr",
        "rangeKm",
        "dcFastCharging",
        "verificationStatus"
      ]
    }
  ],
  "missingInformation": [],
  "warnings": [
    "Vehicle prices, specs, range, and availability should be verified before purchase.",
    "Route distance, charger availability, connector compatibility, pricing, and access should be checked separately for Lahore to Islamabad travel."
  ],
  "validationStatus": "VALID",
  "failureReason": null
}
```

## Safety And Validation

The model output is not trusted directly.

The service validates that:

* returned status is supported
* required JSON fields are present
* recommended vehicle IDs exist in the deterministic candidate list
* recommendation ranks are unique
* `factsUsed` values are allowed
* recommendation text does not contain unsupported live charger availability claims
* recommendation text does not claim route feasibility from catalogue facts alone
* recommendation text does not claim EVReady field verification unless supported
* recommendation text does not imply dealer availability, stock, guaranteed price, or live charger status
* recommendation text does not invent charging infrastructure claims
* fields mentioned in recommendation text are covered by `factsUsed`

The service also applies deterministic enrichment before validation.

For example, if the model mentions DC fast charging but omits `dcFastCharging` from `factsUsed`, the service adds that field before validation while preserving the original raw model output.

This gives the system both:

* original raw model output for audit
* enriched and validated output for the API response

## Data Trust Rules

This service only uses catalogue data returned by the existing EVReady backend.

Important constraints:

* vehicle prices should be treated as catalogue/reference data
* vehicle range values should be verified before purchase
* `verificationStatus` represents source confidence, not a physical EVReady audit
* charger availability is not live
* route feasibility is not guaranteed
* dealer availability and stock are not checked
* financing, insurance, legal, and tax advice are out of scope

The recommender can say a vehicle supports DC fast charging if that field is present in the catalogue data.

It must not say DC fast charging is available in a way that implies live charger availability.

## Database And Liquibase

Liquibase master changelog:

```text
src/main/resources/db/db.changelog-master.xml
```

SQL changelog folder:

```text
src/main/resources/db/changelog/
```

Current persistence tables:

* `recommendation_run`
* `recommendation_candidate_snapshot`
* `recommendation_result`

Recommendation runs store:

* original request JSON
* run status
* summary
* raw model output
* parsed/enriched output JSON
* validation status
* failure reason
* model metadata
* run config
* missing information
* warnings
* timestamps

Candidate snapshots are important because EVReady catalogue data can change. A stored recommendation should remain explainable even if the source vehicle record changes later.

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

Expected local services:

```text
EVReady backend: http://localhost:8080
EVReady AI Recommender Service: http://localhost:8081
Ollama: http://localhost:11434
PostgreSQL: localhost:5435
```

Expected local model:

```text
qwen3:4b
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
OLLAMA_NUM_PREDICT=1024
OLLAMA_NUM_CTX=4096
```

Do not commit real secrets.

## Local Smoke Scripts

Smoke scripts are stored under:

```text
scratch/smoke
```

Available scripts:

```powershell
.\scratch\smoke\run-candidate-selection-smoke.ps1
.\scratch\smoke\run-recommendation-smoke.ps1
.\scratch\smoke\run-recommendation-roundtrip-smoke.ps1
.\scratch\smoke\run-error-response-smoke.ps1
```

### Candidate Selection Smoke

```powershell
.\scratch\smoke\run-candidate-selection-smoke.ps1
```

Verifies deterministic catalogue-backed candidate selection without calling the LLM.

### Full Recommendation Smoke

```powershell
.\scratch\smoke\run-recommendation-smoke.ps1
```

Verifies candidate selection, prompt construction, Ollama generation, JSON parsing, enrichment, validation, persistence, and response mapping.

### Recommendation Round-Trip Smoke

```powershell
.\scratch\smoke\run-recommendation-roundtrip-smoke.ps1
```

Verifies the persisted recommendation flow:

```text
POST /api/v1/recommendations
GET /api/v1/recommendations/{id}
```

The script creates a recommendation, captures the returned ID, retrieves the same recommendation, and verifies the stored ID and status match.

### Error Response Smoke

```powershell
.\scratch\smoke\run-error-response-smoke.ps1
```

Verifies consistent API error responses for:

```text
POST /api/v1/recommendations with invalid request data
GET /api/v1/recommendations/{id} with a missing ID
```

The script checks HTTP status codes and confirms the response body uses the standard API error shape.

## Testing

Run unit tests:

```powershell
.\gradlew clean test
```

Run full build:

```powershell
.\gradlew clean build
```

Current automated tests focus on recommendation safety behavior, especially:

* facts-used enrichment
* model-output validation
* unsupported wording rejection
* candidate-list enforcement

## CI

GitHub Actions runs:

```bash
./gradlew clean build
```

on pushes and pull requests for:

* `develop`
* `main`
* `feature/**`

Current CI does not require PostgreSQL, Ollama, or the EVReady backend because the automated tests are unit-level.

## Consistent API Errors

The service returns a consistent error shape for validation, invalid JSON, missing recommendations, upstream client failures, and unexpected server errors.

Example validation error shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed.",
  "fieldErrors": [
    {
      "field": "budgetPkr",
      "message": "must be greater than 0"
    }
  ]
}
```

Example not-found error shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Recommendation run not found: 999999",
  "fieldErrors": []
}
```

## Current Non-Goals

This service does not currently provide:

* user accounts
* authentication
* production deployment
* frontend integration
* live charger availability
* route planning
* dealer stock checks
* booking
* payments
* financing advice
* insurance advice
* tax advice
* embedding/RAG-based recommendation
* fine-tuning

## Documentation

Project planning:

```text
docs/PROJECT_PLAN.md
```

Smoke script documentation:

```text
scratch/smoke/README.md
```

## Branch Strategy

* `main` should stay release-ready.
* `develop` is the integration/testing branch.
* Feature work should use short-lived `feature/*` branches created from `develop`.

Pull request flow:

```text
feature/* -> develop
develop -> main
```

Avoid committing directly to `main`.

Avoid committing directly to `develop` unless intentionally handling integration work.

## Portfolio Positioning

Safe description:

```text
Catalog-backed AI recommender service for EVReady Pakistan that ranks EV options using trusted vehicle facts, deterministic candidate filtering, structured LLM output, validation, and stored recommendation runs.
```

What this project demonstrates:

* AI integration with an existing deployed product
* separate service architecture
* catalog-backed recommendation
* deterministic filtering before model use
* structured LLM output
* deterministic model-output enrichment
* safety validation
* candidate evidence snapshots
* recommendation run persistence
* auditability
* conservative handling of uncertain product and charger data
