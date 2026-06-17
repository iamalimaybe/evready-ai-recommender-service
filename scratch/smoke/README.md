# Smoke Scripts

Manual local smoke scripts for EVReady AI Recommender Service.

These scripts are not automated tests. They verify that the service can:

- call the existing EVReady backend vehicle APIs
- select deterministic candidate vehicles
- call the local Ollama model
- return structured recommendation responses

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

## Candidate Selection Smoke

```text
.\scratch\smoke\run-candidate-selection-smoke.ps1
```

This calls:

```text
POST /api/v1/recommendations/candidates
```

It verifies deterministic catalog-backed candidate selection without calling the LLM.

## Full Recommendation Smoke

```text
.\scratch\smoke\run-recommendation-smoke.ps1
```

This calls:

```text
POST /api/v1/recommendations
```

It verifies candidate selection, prompt construction, Ollama generation, JSON parsing, validation, persistence, and response mapping.

Notes

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
```

The script creates a recommendation, captures the returned ID, retrieves the same recommendation, and verifies the stored ID and status match.