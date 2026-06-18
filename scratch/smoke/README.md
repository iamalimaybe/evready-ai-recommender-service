# Smoke Scripts

Manual local smoke scripts for EVReady AI Recommender Service.

These scripts are not automated tests. They verify that the local service can:

* call the existing EVReady backend vehicle APIs
* select deterministic candidate vehicles
* create queued recommendation runs
* process recommendations asynchronously
* call the local Ollama model
* parse, enrich, and validate structured model output
* persist recommendation runs and results
* retrieve stored recommendation results by ID
* return consistent API error responses
* fail safely when model generation times out

## Required Local Services

Expected local services:

```text
EVReady backend: http://localhost:8080
EVReady AI Recommender Service: http://localhost:8081
Ollama: http://localhost:11434
```

Expected local model:

```text
qwen3:4b
```

Default recommender base URL:

```text
http://localhost:8081
```

You can override it with:

```powershell
$env:RECOMMENDER_BASE_URL="http://localhost:8081"
```

## Candidate Selection Smoke

```powershell
.\scratch\smoke\run-candidate-selection-smoke.ps1
```

This calls:

```text
POST /api/v1/recommendations/candidates
```

It verifies deterministic catalog-backed candidate selection without calling the LLM.

This smoke should return candidate vehicles, missing information, and warnings based on trusted catalogue data and deterministic request checks.

## Full Recommendation Smoke

```powershell
.\scratch\smoke\run-recommendation-smoke.ps1
```

This calls:

```text
POST /api/v1/recommendations
GET /api/v1/recommendations/{id}
```

The recommendation API is asynchronous.

The POST request should return quickly with:

```text
QUEUED
```

The script then polls the GET endpoint until the recommendation reaches a final status.

In-progress statuses:

```text
PENDING
QUEUED
RUNNING
```

Expected successful final status:

```text
ANSWERED
```

This verifies candidate selection, queued processing, prompt construction, Ollama generation, JSON parsing, deterministic enrichment, validation, persistence, and response mapping.

Notes:

Model responses may vary slightly between runs.

Do not treat one smoke result as proof of recommendation quality. Smoke scripts only verify that the local flow works end to end.

## Recommendation Round-Trip Smoke

```powershell
.\scratch\smoke\run-recommendation-roundtrip-smoke.ps1
```

This verifies the persisted recommendation flow:

```text
POST /api/v1/recommendations
GET /api/v1/recommendations/{id}
GET /api/v1/recommendations/{id}
```

The script creates a recommendation, captures the returned ID, polls until processing reaches a final status, retrieves the same recommendation again, and verifies that the stored ID, status, validation status, and recommendations are still available.

This confirms that the frontend can safely use POST for creating a run and GET for polling and later retrieval.

## Timeout Smoke

```powershell
.\scratch\smoke\run-timeout-smoke.ps1
```

This verifies safe timeout handling for slow or stuck local model generation.

Before running this script, restart the recommender service with a short read timeout:

```powershell
$env:OLLAMA_READ_TIMEOUT="1s"
.\gradlew bootRun
```

Then run the timeout smoke in another terminal:

```powershell
.\scratch\smoke\run-timeout-smoke.ps1
```

Expected final status:

```text
TIMED_OUT
```

Expected validation status:

```text
INVALID
```

Expected failure reason should mention Ollama model generation timing out.

After the timeout smoke, stop the recommender service and clear the temporary environment variable:

```powershell
Remove-Item Env:\OLLAMA_READ_TIMEOUT
```

Then restart the service normally before running regular smokes again.

## Error Response Smoke

```powershell
.\scratch\smoke\run-error-response-smoke.ps1
```

This verifies consistent API error responses for:

```text
POST /api/v1/recommendations with invalid request data
GET /api/v1/recommendations/{id} with a missing ID
```

The script checks HTTP status codes and confirms the response body uses the standard API error shape.

## Suggested Local Verification Order

Run normal smokes first:

```powershell
.\scratch\smoke\run-candidate-selection-smoke.ps1
.\scratch\smoke\run-recommendation-smoke.ps1
.\scratch\smoke\run-recommendation-roundtrip-smoke.ps1
.\scratch\smoke\run-error-response-smoke.ps1
```

Run timeout smoke separately because it requires restarting the recommender service with:

```text
OLLAMA_READ_TIMEOUT=1s
```

## Current API Behavior Covered By Smokes

```text
POST /api/v1/recommendations/candidates
```

Runs deterministic candidate selection only.

```text
POST /api/v1/recommendations
```

Creates a recommendation run and returns quickly with `QUEUED`.

```text
GET /api/v1/recommendations/{id}
```

Returns the current or final stored recommendation state.

Possible in-progress statuses:

```text
PENDING
QUEUED
RUNNING
```

Possible final statuses:

```text
ANSWERED
INSUFFICIENT_CANDIDATES
NEEDS_MORE_INFORMATION
FAILED
TIMED_OUT
```

## Important Safety Expectations

The service should not trust model output directly.

The recommendation flow should preserve raw model output, parse structured output, enrich missing traceability facts where safe, validate claims against deterministic candidate data, and fail safely when the output is unsafe or unusable.

The service should not claim:

* live charger availability
* route feasibility
* dealer stock
* booking availability
* guaranteed prices
* guaranteed range
* Lahore to Islamabad feasibility without separate route and charger verification

For intercity travel, responses should include a deterministic warning such as:

```text
Route distance, charger availability, connector compatibility, pricing, and access should be checked separately for Lahore to Islamabad travel.
```
