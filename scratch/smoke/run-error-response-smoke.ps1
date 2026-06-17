$ErrorActionPreference = "Stop"

function Read-ErrorResponseBody {
    param($ErrorRecord)

    $response = $ErrorRecord.Exception.Response

    if ($null -eq $response) {
        throw "No HTTP response was returned."
    }

    $stream = $response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $body = $reader.ReadToEnd()
    $reader.Close()

    return $body
}

function Invoke-ExpectHttpError {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Body,
        [int]$ExpectedStatus
    )

    try {
        if ([string]::IsNullOrWhiteSpace($Body)) {
            Invoke-RestMethod -Uri $Uri -Method $Method
        } else {
            Invoke-RestMethod `
                -Uri $Uri `
                -Method $Method `
                -ContentType "application/json" `
                -Body $Body
        }

        throw "Expected HTTP $ExpectedStatus but request succeeded."
    } catch {
        $response = $_.Exception.Response

        if ($null -eq $response) {
            throw $_
        }

        $actualStatus = [int]$response.StatusCode

        if ($actualStatus -ne $ExpectedStatus) {
            throw "Expected HTTP $ExpectedStatus but got HTTP $actualStatus."
        }

        $bodyText = Read-ErrorResponseBody $_

        if ([string]::IsNullOrWhiteSpace($bodyText)) {
            throw "Expected error response body but body was empty."
        }

        return $bodyText | ConvertFrom-Json
    }
}

Write-Host "Testing validation error response..."

$invalidBodyJson = @{
    vehicleType = "CAR"
    budgetPkr = -1
} | ConvertTo-Json -Compress

$validationError = Invoke-ExpectHttpError `
    -Method "Post" `
    -Uri "http://localhost:8081/api/v1/recommendations" `
    -Body $invalidBodyJson `
    -ExpectedStatus 400

if ($validationError.status -ne 400) {
    throw "Validation error payload status was not 400."
}

if ($validationError.message -ne "Request validation failed.") {
    throw "Unexpected validation error message: $($validationError.message)"
}

if ($null -eq $validationError.fieldErrors -or $validationError.fieldErrors.Count -eq 0) {
    throw "Validation error response did not include fieldErrors."
}

$validationError | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "Testing not-found error response..."

$notFoundError = Invoke-ExpectHttpError `
    -Method "Get" `
    -Uri "http://localhost:8081/api/v1/recommendations/999999" `
    -ExpectedStatus 404

if ($notFoundError.status -ne 404) {
    throw "Not-found error payload status was not 404."
}

if ($notFoundError.message -ne "Recommendation run not found: 999999") {
    throw "Unexpected not-found message: $($notFoundError.message)"
}

$notFoundError | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "Error response smoke passed."