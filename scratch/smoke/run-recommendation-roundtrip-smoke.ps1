$ErrorActionPreference = "Stop"

$baseUrl = $env:RECOMMENDER_BASE_URL

if ([string]::IsNullOrWhiteSpace($baseUrl)) {
    $baseUrl = "http://localhost:8081"
}

$pollIntervalSeconds = 3
$maxPollAttempts = 30
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

Write-Host "Creating recommendation..."

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

if ($finalResponse.id -ne $postResponse.id) {
    throw "Final response id $($finalResponse.id) did not match POST response id $($postResponse.id)."
}

if ($finalResponse.status -ne "ANSWERED") {
    $finalResponse | ConvertTo-Json -Depth 20
    throw "Expected final status ANSWERED but got $($finalResponse.status)."
}

Write-Host ""
Write-Host "Final polled response:"
$finalResponse | ConvertTo-Json -Depth 20

Write-Host ""
Write-Host "Retrieving persisted recommendation id $($postResponse.id) again..."

$getResponse = Invoke-RestMethod `
    -Uri "$baseUrl/api/v1/recommendations/$($postResponse.id)" `
    -Method Get

if ($getResponse.id -ne $postResponse.id) {
    throw "GET response id $($getResponse.id) did not match POST response id $($postResponse.id)."
}

if ($getResponse.status -ne $finalResponse.status) {
    throw "Persisted GET status $($getResponse.status) did not match final polled status $($finalResponse.status)."
}

if ($getResponse.validationStatus -ne $finalResponse.validationStatus) {
    throw "Persisted GET validationStatus $($getResponse.validationStatus) did not match final polled validationStatus $($finalResponse.validationStatus)."
}

if ($null -eq $getResponse.recommendations -or $getResponse.recommendations.Count -eq 0) {
    $getResponse | ConvertTo-Json -Depth 20
    throw "Expected persisted GET response to include recommendations."
}

Write-Host ""
Write-Host "Persisted GET response:"
$getResponse | ConvertTo-Json -Depth 20

Write-Host ""
Write-Host "Round-trip smoke passed for recommendation id $($postResponse.id)."