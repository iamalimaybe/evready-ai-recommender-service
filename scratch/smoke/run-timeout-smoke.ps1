$ErrorActionPreference = "Stop"

$baseUrl = $env:RECOMMENDER_BASE_URL

if ([string]::IsNullOrWhiteSpace($baseUrl)) {
    $baseUrl = "http://localhost:8081"
}

$pollIntervalSeconds = 3
$maxPollAttempts = 10
$inProgressStatuses = @("PENDING", "QUEUED", "RUNNING")

function Wait-ForRecommendationResult {
    param(
        [long]$RecommendationId
    )

    for ($attempt = 1; $attempt -le $maxPollAttempts; $attempt++) {
        Start-Sleep -Seconds $pollIntervalSeconds

        $result = Invoke-RestMethod `
            -Uri "$baseUrl/api/v1/recommendations/$RecommendationId" `
            -Method Get

        Write-Host "Attempt $attempt - Status: $($result.status)"

        if ($inProgressStatuses -notcontains $result.status) {
            return $result
        }
    }

    throw "Recommendation id $RecommendationId did not reach a final status after $maxPollAttempts polling attempts."
}

Write-Host "Timeout smoke expects the recommender service to be running with:"
Write-Host "OLLAMA_READ_TIMEOUT=1s"
Write-Host ""

$bodyJson = @{
    vehicleType = "CAR"
    budgetPkr = 9000000
    city = "Lahore"
    dailyDistanceKm = 45
    monthlyDistanceKm = 1200
    homeChargingAvailable = $true
    solarAvailable = $false
    primaryUseCase = "family commute"
    familySize = 4
    priority = "longer range"
    additionalNotes = "Occasional Lahore to Islamabad travel."
} | ConvertTo-Json -Compress

Write-Host "Creating recommendation request..."

$postResponse = Invoke-RestMethod `
    -Uri "$baseUrl/api/v1/recommendations" `
    -Method Post `
    -ContentType "application/json" `
    -Body $bodyJson

if ($null -eq $postResponse.id) {
    throw "POST response did not include an id."
}

if ($postResponse.status -ne "QUEUED") {
    throw "Expected POST status QUEUED but got $($postResponse.status)."
}

Write-Host ""
Write-Host "POST response:"
$postResponse | ConvertTo-Json -Depth 20

Write-Host ""
Write-Host "Polling recommendation id $($postResponse.id)..."

$finalResponse = Wait-ForRecommendationResult -RecommendationId $postResponse.id

if ($finalResponse.status -ne "TIMED_OUT") {
    $finalResponse | ConvertTo-Json -Depth 20
    throw "Expected final status TIMED_OUT but got $($finalResponse.status)."
}

if ($finalResponse.validationStatus -ne "INVALID") {
    $finalResponse | ConvertTo-Json -Depth 20
    throw "Expected validationStatus INVALID but got $($finalResponse.validationStatus)."
}

if ([string]::IsNullOrWhiteSpace($finalResponse.failureReason)) {
    $finalResponse | ConvertTo-Json -Depth 20
    throw "Expected timeout failureReason to be present."
}

Write-Host ""
Write-Host "Final response:"
$finalResponse | ConvertTo-Json -Depth 20

Write-Host ""
Write-Host "Timeout smoke passed for recommendation id $($postResponse.id)."