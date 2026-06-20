# EVReady AI Recommender Service

Catalog-backed AI recommendation service for EVReady Pakistan.

This service helps users compare EV options from the existing EVReady catalogue using practical needs such as budget, vehicle type, daily distance, charging access, city, family use, and ownership priorities.

The core rule is simple: the model can explain and rank options, but it must stay grounded in trusted EVReady catalogue data. It must not invent vehicles, prices, ranges, battery values, charger availability, dealer availability, route feasibility, or field-verification claims.

## Current Status

EVReady AI Recommender Service is an MVP backend service integrated with EVReady Pakistan through the existing EVReady backend gateway.

Implemented so far:

* Spring Boot backend service
* PostgreSQL persistence with Liquibase migrations
* EVReady backend catalogue client
* deterministic candidate selection
* asynchronous recommendation processing
* bounded recommendation worker queue
* Ollama model generation client
* configurable Ollama connect and read timeouts
* timeout-safe recommendation status handling
* structured recommendation prompt
* strict JSON model-output parsing
* deterministic facts-used enrichment
* deterministic route and intercity travel warnings
* safety and traceability validation
* recommendation run persistence
* candidate snapshot persistence
* recommendation result persistence
* stored response metadata
* candidate selection endpoint
* create recommendation endpoint
* retrieve recommendation endpoint
* consistent API error responses
* production-shaped backend gateway integration
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
* public API gateway routing

This recommender service owns recommendation-specific concerns:

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

Production-shaped request path:

```text
EVReady Frontend
  -> EVReady Backend API Gateway
      -> EVReady AI Recommender Service
          -> Ollama / local model runtime
```

The EVReady backend remains the public browser-facing API boundary.

The AI recommender service is intended to run as an internal service behind the backend gateway in production. The browser should not call this service directly in production.

The existing EVReady backend remains the source of catalogue data.

The recommender service does not directly read the existing EVReady production database in version 1. It reads catalogue data through backend APIs.

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

These are the recommender service's internal service endpoints.

In production-shaped usage, the public frontend calls the EVReady backend gateway instead of calling these endpoints directly.

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

Creates a recommendation run and returns quickly with an ID.

This endpoint is asynchronous. It does not wait for Ollama to finish before returning.

The synchronous part of the flow:

1. validate the request
2. load EV catalogue data from the EVReady backend
3. select deterministic candidates
4. store the recommendation run
5. store candidate snapshots
6. store initial missing information and warnings
7. return the run ID with status `QUEUED`

The background part of the flow:

1. mark the run as `RUNNING`
2. build the model prompt
3. call Ollama
4. parse model JSON
5. enrich facts used
6. validate output
7. store recommendation results
8. store raw and parsed model output
9. mark the run as a final status

The frontend or API consumer should poll the retrieve endpoint until a final status is reached.

### Get Recommendation

```http
GET /api/v1/recommendations/{id}
```

Returns the current or final stored recommendation run with persisted recommendation results, warnings, missing information, validation status, and failure reason.

This endpoint is used for polling after creating a recommendation.

### Health

```http
GET /actuator/health
```

Returns service health information.

## Public Gateway Endpoints

The public EVReady backend exposes gateway endpoints for frontend usage:

```http
POST /api/v1/ai/recommendations
GET  /api/v1/ai/recommendations/{id}
GET  /api/v1/ai/recommendations/health
```

The backend gateway forwards accepted recommendation requests to this internal recommender service.

The backend gateway is also responsible for public-facing controls such as rate limiting expensive recommendation creation requests.

## Recommendation Statuses

In-progress statuses:

```text
PENDING
QUEUED
RUNNING
```

Final statuses:

```text
ANSWERED
INSUFFICIENT_CANDIDATES
NEEDS_MORE_INFORMATION
FAILED
TIMED_OUT
```

Expected frontend behavior:

1. submit `POST /api/v1/ai/recommendations` to the EVReady backend gateway
2. store the returned `id`
3. show a loading state
4. poll `GET /api/v1/ai/recommendations/{id}` through the backend gateway
5. continue polling while status is `PENDING`, `QUEUED`, or `RUNNING`
6. stop polling when a final status is returned
7. render the recommendation, no-match state, timeout state, or safe failure state

Recommended local polling interval:

```text
3 seconds
```

A frontend may stop polling after a client-side waiting limit, such as 90 seconds, but that does not automatically mean the backend run failed. The stored run can still be checked again by ID.

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

## Example Initial Response

```json
{
  "id": 22,
  "status": "QUEUED",
  "summary": "Recommendation request accepted. Check this id for the generated result.",
  "recommendations": [],
  "missingInformation": [],
  "warnings": [
    "Vehicle prices, specs, range, and availability should be verified before purchase.",
    "Route distance, charger availability, connector compatibility, pricing, and access should be checked separately for Lahore to Islamabad travel."
  ],
  "validationStatus": "NOT_VALIDATED",
  "failureReason": null
}
```

## Example Final Response

```json
{
  "id": 22,
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

## Example Timeout Response

If model generation takes too long, the run is marked safely as `TIMED_OUT`.

```json
{
  "id": 18,
  "status": "TIMED_OUT",
  "summary": null,
  "recommendations": [],
  "missingInformation": [
    "Home charging availability was not provided."
  ],
  "warnings": [
    "Vehicle prices, specs, range, and availability should be verified before purchase."
  ],
  "validationStatus": "INVALID",
  "failureReason": "Ollama model generation timed out after PT1S."
}
```

The exact timeout duration depends on environment configuration.

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

## Deterministic Warnings

The service adds some warnings deterministically before the model response is available.

Examples:

* vehicle prices, specs, range, and availability should be verified before purchase
* home charging concerns when home charging is unavailable
* route and charger checks for intercity travel

For Lahore to Islamabad travel, the service should include:

```text
Route distance, charger availability, connector compatibility, pricing, and access should be checked separately for Lahore to Islamabad travel.
```

This warning is not left to the model. It is added by backend logic so it appears consistently in queued, running, answered, failed, and timed-out recommendation states.

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

## Slow Model Handling

Local Ollama generation can be slow, especially when the model is cold or the machine is under load.

The service handles this by:

* returning `QUEUED` quickly from `POST /api/v1/recommendations`
* processing model generation in a background worker
* allowing clients to poll `GET /api/v1/recommendations/{id}`
* limiting concurrent recommendation processing
* limiting queue capacity
* applying configurable Ollama connect and read timeouts
* marking slow or stuck model runs as `TIMED_OUT`

This prevents the public request flow from depending on one long synchronous browser request.

## Queue And Timeout Configuration

Default behavior is conservative for local development:

```text
1 recommendation worker
10 queued recommendation jobs
5 second Ollama connect timeout
90 second Ollama read timeout
```

Environment variables:

```text
RECOMMENDATION_PROCESSING_CORE_POOL_SIZE=1
RECOMMENDATION_PROCESSING_MAX_POOL_SIZE=1
RECOMMENDATION_PROCESSING_QUEUE_CAPACITY=10

OLLAMA_CONNECT_TIMEOUT=5s
OLLAMA_READ_TIMEOUT=90s
```

If the queue is full, the service should fail the run safely instead of allowing unlimited public work to pile up.

## Access Control And Production Exposure

This MVP does not currently include user accounts or authentication.

For local development, the frontend may call the recommender service directly:

```text
http://localhost:8081
```

For production, the recommender service should not be treated as a public unrestricted API.

Preferred production direction:

```text
Browser / evready.pk
  -> existing EVReady backend or controlled API gateway
      -> internal EVReady AI Recommender Service
```

Current production-shaped integration uses the EVReady backend as the public gateway.

Important notes:

* CORS is not a security boundary.
* A frontend-only API key is not a secret.
* Browser JavaScript cannot safely hide backend credentials.
* The recommender should not be deployed as an unrestricted public model-generation endpoint.
* Public recommendation creation should be protected at the backend gateway or another controlled backend boundary.
* The model runtime should not be exposed directly to browsers.

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
OLLAMA_CONNECT_TIMEOUT=5s
OLLAMA_READ_TIMEOUT=90s

RECOMMENDATION_PROCESSING_CORE_POOL_SIZE=1
RECOMMENDATION_PROCESSING_MAX_POOL_SIZE=1
RECOMMENDATION_PROCESSING_QUEUE_CAPACITY=10
```

Production values should come from server environment configuration, not committed files.

Do not commit real secrets, `.env` files, database passwords, service credentials, or private deployment notes.

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
.\scratch\smoke\run-timeout-smoke.ps1
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

Verifies async recommendation creation, polling, candidate selection, prompt construction, Ollama generation, JSON parsing, enrichment, validation, persistence, and response mapping.

Expected behavior:

```text
POST returns QUEUED
GET eventually returns ANSWERED
validationStatus is VALID
```

### Recommendation Round-Trip Smoke

```powershell
.\scratch\smoke\run-recommendation-roundtrip-smoke.ps1
```

Verifies the persisted recommendation flow:

```text
POST /api/v1/recommendations
GET /api/v1/recommendations/{id}
GET /api/v1/recommendations/{id}
```

The script creates a recommendation, captures the returned ID, polls until a final status is reached, retrieves the same recommendation again, and verifies the stored ID, status, validation status, and recommendation results.

### Timeout Smoke

```powershell
.\scratch\smoke\run-timeout-smoke.ps1
```

Verifies safe timeout handling.

Before running this script, restart the recommender service with a short read timeout:

```powershell
$env:OLLAMA_READ_TIMEOUT="1s"
.\gradlew bootRun
```

Then run the timeout smoke in another terminal:

```powershell
.\scratch\smoke\run-timeout-smoke.ps1
```

Expected behavior:

```text
POST returns QUEUED
GET eventually returns TIMED_OUT
validationStatus is INVALID
failureReason mentions Ollama model generation timeout
```

After the timeout smoke, stop the recommender service and clear the temporary environment variable:

```powershell
Remove-Item Env:\OLLAMA_READ_TIMEOUT
```

Then restart the service normally.

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

Manual smoke scripts are still required to verify the local end-to-end flow because CI does not run PostgreSQL, Ollama, or the existing EVReady backend.

## CI

GitHub Actions runs:

```bash
./gradlew clean build
```

on pushes and pull requests for:

* `develop`
* `main`
* `feature/**`

Current CI check:

```text
Build and test
```

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
* direct public production exposure
* live charger availability
* route planning guarantees
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
* backend-gateway production integration
* catalog-backed recommendation
* async model processing for slow local LLMs
* timeout-safe recommendation handling
* deterministic filtering before model use
* structured LLM output
* deterministic model-output enrichment
* safety validation
* candidate evidence snapshots
* recommendation run persistence
* auditability
* conservative handling of uncertain product, charger, and route data
