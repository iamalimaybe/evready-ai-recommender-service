$ErrorActionPreference = "Stop"

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
    -Uri "http://localhost:8081/api/v1/recommendations" `
    -Method Post `
    -ContentType "application/json" `
    -Body $bodyJson

if ($null -eq $postResponse.id) {
    throw "POST response did not include an id."
}

Write-Host ""
Write-Host "POST response:"
$postResponse | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "Retrieving recommendation id $($postResponse.id)..."

$getResponse = Invoke-RestMethod `
    -Uri "http://localhost:8081/api/v1/recommendations/$($postResponse.id)" `
    -Method Get

if ($getResponse.id -ne $postResponse.id) {
    throw "GET response id $($getResponse.id) did not match POST response id $($postResponse.id)."
}

if ($getResponse.status -ne $postResponse.status) {
    throw "GET response status $($getResponse.status) did not match POST response status $($postResponse.status)."
}

Write-Host ""
Write-Host "GET response:"
$getResponse | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "Round-trip smoke passed for recommendation id $($postResponse.id)."