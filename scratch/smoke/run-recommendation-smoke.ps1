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

$response = Invoke-RestMethod `
    -Uri "http://localhost:8081/api/v1/recommendations" `
    -Method Post `
    -ContentType "application/json" `
    -Body $bodyJson

$response | ConvertTo-Json -Depth 10