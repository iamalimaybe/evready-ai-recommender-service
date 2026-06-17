# EVReady AI Recommender Service Project Plan

## Goal

Build a separate AI recommendation service for EVReady Pakistan.

The service helps users compare EV options from the existing EVReady catalogue based on practical usage needs such as budget, vehicle type, daily distance, charging access, city, and ownership priorities.

The recommender must use trusted EVReady catalogue data as the source of truth. AI output is used to explain and rank options, not to invent vehicles, prices, range values, battery sizes, charger claims, dealer availability, or route guarantees.

## Product Positioning

This is not a generic chatbot.

This is a catalog-backed AI recommendation service that plugs into the existing EVReady product.

The portfolio value is to show how AI can be added to an existing deployed product while keeping:

* the core product backend stable
* the recommender service isolated
* model output constrained by trusted catalogue facts
* recommendation runs stored and auditable
* unsupported claims rejected before they reach users

## Non-Goals

Do not build these in the first version:

* Full e-commerce flow
* Cart or checkout
* Payments
* Booking
* Dealer management
* Financing advice
* Insurance advice
* Legal or tax advice
* Public user accounts
* Live charger availability
* Route guarantee
* Dealer availability guarantee
* Automatic charger status updates
* Full admin dashboard
* Fine-tuning
* RAG over documents
* Embedding-based recommendation

## Architecture

The recommender is a separate backend service.

```text
EVReady Frontend
  -> EVReady Backend
      - vehicles
      - brands
      - charger types
      - chargers
      - leads
      - contact submissions
      - reviews
      - feedback
      - admin workflows

  -> EVReady AI Recommender Service
      - recommendation requests
      - candidate filtering
      - LLM explanation
      - structured output validation
      - recommendation run storage
      - audit history
```

The existing EVReady backend remains the source of product and catalogue data.

The recommender service owns only recommendation-specific logic, model execution, validation, persistence, and audit records.

## Data Source Strategy

Version 1 consumes public EVReady backend APIs as the trusted source.

Candidate APIs:

```text
GET /api/v1/vehicles
GET /api/v1/vehicles/{id}
GET /api/v1/brands
GET /api/v1/charger-types
```

Charger data can be used cautiously later, but it should not drive strong claims in version 1 because charger status is not live availability.

Possible charger APIs for later use:

```text
GET /api/v1/chargers
GET /api/v1/chargers/cities
```

The recommender should not directly read the existing EVReady production database in version 1.

Reasons:

* avoid tight coupling
* avoid sharing database credentials between services
* avoid accidental writes to operational data
* keep the recommender reusable
* keep the existing EVReady backend stable

## First Recommendation Use Case

The first user-facing use case:

```text
Help me choose an EV from the EVReady catalogue.
```

Initial request inputs:

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

Example priorities:

* lowest running cost
* lowest upfront price
* family use
* city commute
* longer range
* home charging fit
* bike replacement
* car replacement

## Candidate Filtering

Candidate filtering must happen before the LLM call.

The deterministic filtering layer selects candidate vehicles from the EVReady catalogue using user constraints.

Initial filters:

* vehicle type, if provided
* budget, if provided
* range suitability, if enough distance information is provided
* charging preference, if relevant
* source-confidence handling

The LLM should receive only bounded candidate facts, not the entire catalogue by default.

The candidate list should be small enough for the model to compare clearly.

## LLM Role

The LLM explains and ranks candidates using only the provided candidate facts.

The LLM may:

* explain why a vehicle fits
* explain tradeoffs
* identify missing information
* compare options
* give cautious ownership guidance

The LLM must not:

* invent vehicles
* invent prices
* invent range
* invent battery values
* invent charger availability
* invent dealer availability
* claim live charger status
* claim EVReady field-verified a vehicle or charger
* claim route feasibility as guaranteed
* give financing, legal, tax, or insurance advice

## Structured Model Output

The model should return strict JSON.

Successful output:

```json
{
  "status": "ANSWERED",
  "summary": "Based on the provided catalogue facts, these options appear to fit your budget and usage best.",
  "recommendations": [
    {
      "vehicleId": 12,
      "rank": 1,
      "matchReason": "This option fits the stated budget and has enough listed range for the daily usage assumption.",
      "tradeoffs": [
        "Price and availability should still be verified before purchase.",
        "Listed range may differ from real-world usage."
      ],
      "factsUsed": [
        "pricePkr",
        "rangeKm",
        "batteryCapacityKwh",
        "vehicleType"
      ]
    }
  ],
  "missingInformation": [
    "Exact charging access details were not provided."
  ],
  "warnings": [
    "Vehicle prices and availability can change. Verify with the seller or dealer before purchase."
  ]
}
```

Insufficient candidate output:

```json
{
  "status": "INSUFFICIENT_CANDIDATES",
  "summary": "No catalogue vehicles matched the provided constraints closely enough.",
  "recommendations": [],
  "missingInformation": [
    "Increase budget or relax vehicle type/range requirements."
  ],
  "warnings": []
}
```

Needs more information output:

```json
{
  "status": "NEEDS_MORE_INFORMATION",
  "summary": "More information is needed before a useful recommendation can be made.",
  "recommendations": [],
  "missingInformation": [
    "Please provide a budget or vehicle type."
  ],
  "warnings": []
}
```

## Validation Rules

The recommender service must validate model output before returning or storing it as valid.

Required validation rules:

* `status` is required.
* `summary` is required.
* `recommendations` is required.
* `missingInformation` is required.
* `warnings` is required.
* Valid statuses:

    * `ANSWERED`
    * `INSUFFICIENT_CANDIDATES`
    * `NEEDS_MORE_INFORMATION`
    * `FAILED`
* The model must not return `FAILED` for normal insufficient data cases.
* Every recommended `vehicleId` must exist in the candidate list sent to the model.
* Recommendation ranks must be unique.
* `ANSWERED` must include at least one recommendation.
* `INSUFFICIENT_CANDIDATES` must not include recommendations.
* `NEEDS_MORE_INFORMATION` must not include recommendations.
* `factsUsed` must only reference allowed candidate fields.
* Output must not contain unsupported charger or live availability claims.
* Output must not contain EVReady field-verification claims.
* Output must not contain dealer availability claims.
* Raw model output must be stored for audit.

## Storage Model

The recommender service owns its own database.

First tables:

```text
recommendation_run
recommendation_candidate_snapshot
recommendation_result
```

### recommendation_run

Stores each recommendation request and model execution result.

Fields:

```text
id
request_json
status
summary
raw_model_output
parsed_output_json
validation_status
failure_reason
model_name
model_provider
temperature
run_config_json
created_at
updated_at
```

### recommendation_candidate_snapshot

Stores the candidate vehicle facts used for a run.

Fields:

```text
id
recommendation_run_id
vehicle_id
candidate_json
rank_before_llm
created_at
```

Candidate snapshots are important because vehicle catalogue data can change later. A stored recommendation should remain explainable even if the source vehicle record changes after the run.

### recommendation_result

Stores validated ranked recommendations.

Fields:

```text
id
recommendation_run_id
vehicle_id
rank
match_reason
tradeoffs_json
facts_used_json
created_at
```

## API Contract

Initial public API:

```text
POST /api/v1/recommendations
GET /api/v1/recommendations/{id}
```

Optional local/demo API:

```text
GET /api/v1/recommendations
```

### POST /api/v1/recommendations

Request:

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
  "priority": "lowest running cost",
  "additionalNotes": "Occasional Lahore to Islamabad travel."
}
```

Response:

```json
{
  "id": 1,
  "status": "ANSWERED",
  "summary": "Based on the provided catalogue facts, these options appear to fit best.",
  "recommendations": [
    {
      "vehicleId": 12,
      "rank": 1,
      "matchReason": "Fits the budget and has enough listed range for the stated daily use.",
      "tradeoffs": [
        "Verify price and availability before purchase."
      ],
      "factsUsed": [
        "pricePkr",
        "rangeKm"
      ]
    }
  ],
  "missingInformation": [],
  "warnings": [
    "Listed range and price should be verified before purchase."
  ]
}
```

## Backend Package Direction

Use feature-based packages.

Initial package shape:

```text
com.evready.recommender
  common
  config
  evready
  llm
  recommendation
```

Suggested responsibilities:

```text
common
  shared errors, shared helpers, common response types

config
  application configuration properties

evready
  client for existing EVReady backend APIs
  DTOs for vehicle/catalogue data fetched from EVReady backend

llm
  model provider abstraction
  Ollama implementation
  model request/response DTOs

recommendation
  API controller
  request/response DTOs
  domain entities
  repositories
  service interfaces
  service implementations
  validators
```

Keep repositories inside the owning feature.

Do not let the recommender service directly access the existing EVReady backend database.

## Frontend Integration Plan

Add a page to the existing EVReady frontend later:

```text
/recommend
```

Possible UI sections:

* recommendation form
* ranked recommendation cards
* why this matched
* tradeoffs
* missing information
* warnings
* optional audit/details panel for portfolio/demo mode

Do not expose raw prompts or raw model output in a normal public user flow unless intentionally designed and safe.

## Local Development Plan

Local stack:

```text
EVReady backend on port 8080
EVReady AI Recommender Service on port 8081
PostgreSQL for recommender on port 5435
Ollama on port 11434
EVReady frontend on port 5173
```

Initial local config:

```text
EVREADY_API_BASE_URL=http://localhost:8080
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3:4b
```

## Deployment Plan

Start with local development only.

Production deployment should be considered after the local version is useful, validated, and safe.

Possible production API domain later:

```text
https://ai-api.evready.pk
```

or:

```text
https://recommender-api.evready.pk
```

Do not deploy public LLM-backed endpoints without:

* rate limiting
* request size limits
* CORS restrictions
* safe logging
* environment-based model configuration
* clear failure handling
* abuse protection plan

## Security And Privacy Notes

* Do not store unnecessary personal data.
* Do not expose raw prompts or raw model output publicly by default.
* Do not log sensitive user text unnecessarily.
* Do not put API keys or secrets in frontend variables.
* Keep model provider keys in backend environment variables only.
* Keep CORS restricted to intended frontend origins.
* Add rate limiting before exposing public model-backed endpoints.
* Validate request sizes and free-text fields.

## Portfolio Positioning

Safe portfolio description:

```text
A catalog-backed AI recommendation service for EVReady Pakistan that ranks EV options using trusted vehicle facts, deterministic candidate filtering, structured LLM output, validation, and stored recommendation runs.
```

What it demonstrates:

* AI integration with an existing deployed product
* Separate service architecture
* Catalog-backed recommendations
* Deterministic filtering before model use
* Structured LLM output
* Output validation
* Candidate evidence snapshots
* Recommendation run persistence
* Auditability
* Conservative handling of uncertain product and charger data

Avoid claiming:

* live recommendation accuracy
* real user adoption
* dealer partnership
* field verification
* live charger availability
* production-scale recommender traffic
* fine-tuning
* RAG
* semantic search
* embeddings
* personalization
* payments
