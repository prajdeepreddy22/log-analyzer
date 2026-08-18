param(
    [string] $BaseUrl = $env:LOGAI_BASE_URL
)

$ErrorActionPreference = "Stop"

function Initialize-HttpClientSupport {
    try {
        Add-Type -AssemblyName System.Net.Http -ErrorAction Stop
        [System.Net.Http.HttpClient] | Out-Null
        [System.Net.Http.HttpRequestMessage] | Out-Null
        [System.Net.Http.HttpCompletionOption] | Out-Null
    } catch {
        throw "System.Net.Http could not be loaded. Install .NET Framework 4.7.2+ or run this script with PowerShell 7. Details: $($_.Exception.Message)"
    }
}

Initialize-HttpClientSupport

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = "http://localhost:8080/api"
}

$BaseUrl = $BaseUrl.TrimEnd("/")

function Invoke-LogAiJson {
    param(
        [ValidateSet("GET", "POST", "PATCH", "DELETE")]
        [string] $Method,
        [string] $Path,
        [object] $Body = $null,
        [string] $BearerToken = $null
    )

    $headers = @{
        "Content-Type" = "application/json"
    }

    if (-not [string]::IsNullOrWhiteSpace($BearerToken)) {
        $headers["Authorization"] = "Bearer $BearerToken"
    }

    $uri = "$BaseUrl$Path"

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
    }

    return Invoke-RestMethod `
        -Method $Method `
        -Uri $uri `
        -Headers $headers `
        -Body ($Body | ConvertTo-Json -Depth 8)
}

function Wait-SseEvent {
    param(
        [System.IO.StreamReader] $Reader,
        [string] $ExpectedEvent,
        [int] $TimeoutSeconds
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    $eventName = $null
    $data = $null

    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        $remaining = [int] [Math]::Max(
            1,
            ($deadline - [DateTimeOffset]::UtcNow).TotalMilliseconds
        )

        $readTask = $Reader.ReadLineAsync()

        if (-not $readTask.Wait($remaining)) {
            break
        }

        $line = $readTask.Result

        if ($null -eq $line) {
            throw "SSE stream closed before $ExpectedEvent was received."
        }

        if ($line.StartsWith("event:")) {
            $eventName = $line.Substring("event:".Length).Trim()
            continue
        }

        if ($line.StartsWith("data:")) {
            $data = $line.Substring("data:".Length).Trim()
            continue
        }

        if (-not [string]::IsNullOrWhiteSpace($line) -or
                [string]::IsNullOrWhiteSpace($eventName)) {
            continue
        }

        if ($eventName -eq $ExpectedEvent) {
            if ([string]::IsNullOrWhiteSpace($data)) {
                throw "SSE event $ExpectedEvent was received without data."
            }

            return ($data | ConvertFrom-Json)
        }

        $eventName = $null
        $data = $null
    }

    throw "SSE stream did not receive $ExpectedEvent within $TimeoutSeconds seconds."
}

$unique = [Guid]::NewGuid().ToString("N").Substring(0, 12)
$username = "sse_$unique"
$password = "Password@123"

Write-Host "Creating temporary test user..."
$auth = Invoke-LogAiJson `
    -Method POST `
    -Path "/auth/register" `
    -Body @{
        displayName = "SSE Test User"
        username = $username
        email = "$username@example.com"
        password = $password
    }

if ([string]::IsNullOrWhiteSpace($auth.token)) {
    throw "Registration did not return a JWT token."
}

$jwt = $auth.token

Write-Host "Creating ingest API token..."
$tokenResponse = Invoke-LogAiJson `
    -Method POST `
    -Path "/settings/tokens" `
    -BearerToken $jwt `
    -Body @{
        name = "sse-test-$unique"
    }

if ([string]::IsNullOrWhiteSpace($tokenResponse.token)) {
    throw "API token creation did not return the one-time raw token."
}

$apiToken = $tokenResponse.token

Write-Host "Creating log source..."
$source = Invoke-LogAiJson `
    -Method POST `
    -Path "/log-sources" `
    -BearerToken $jwt `
    -Body @{
        sourceName = "SSE Test $unique"
        sourceType = "WATCHER"
    }

if ($null -eq $source.id) {
    throw "Log source creation did not return an id."
}

$encodedToken = [System.Uri]::EscapeDataString($jwt)
$sseUri = "$BaseUrl/events/stream?token=$encodedToken"

$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(45)
$request = [System.Net.Http.HttpRequestMessage]::new(
    [System.Net.Http.HttpMethod]::Get,
    $sseUri
)
$request.Headers.Accept.ParseAdd("text/event-stream")
$request.Headers.CacheControl =
    [System.Net.Http.Headers.CacheControlHeaderValue]::new()
$request.Headers.CacheControl.NoCache = $true

Write-Host "Opening SSE connection..."
Write-Host "SSE URL: $BaseUrl/events/stream?token=<redacted>"
$response = $client.SendAsync(
    $request,
    [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead
).GetAwaiter().GetResult()

Write-Host "SSE HTTP status: $([int] $response.StatusCode)"
Write-Host "SSE Content-Type: $($response.Content.Headers.ContentType)"

if ([int] $response.StatusCode -ne 200) {
    throw "SSE endpoint returned HTTP $([int] $response.StatusCode)."
}

$stream = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
$reader = [System.IO.StreamReader]::new($stream)

try {
    $connectedEvent = Wait-SseEvent `
        -Reader $reader `
        -ExpectedEvent "CONNECTED" `
        -TimeoutSeconds 10

    if ($connectedEvent.type -ne "CONNECTED") {
        throw "Unexpected SSE event type: $($connectedEvent.type)"
    }

    Write-Host "CONNECTED received. Submitting unique ingestion batch..."
    $batchTimestamp = [DateTimeOffset]::UtcNow.ToString("o")
    $ingestion = Invoke-LogAiJson `
        -Method POST `
        -Path "/ingest/stream" `
        -BearerToken $apiToken `
        -Body @{
            sourceId = $source.id
            batchTimestamp = $batchTimestamp
            lines = @(
                "2026-08-18 19:25:10 ERROR [AuthService] NullPointerException $unique",
                "2026-08-18 19:25:12 WARN [PaymentService] Payment gateway timeout $unique"
            )
        }

    if ($ingestion.duplicate -ne $false -or $ingestion.processedLines -ne 2) {
        throw "Ingestion did not process the expected 2 unique lines."
    }

    $event = Wait-SseEvent `
        -Reader $reader `
        -ExpectedEvent "LOG_INGESTED" `
        -TimeoutSeconds 15

    if ($event.type -ne "LOG_INGESTED") {
        throw "Unexpected SSE event type: $($event.type)"
    }

    if ([int64] $event.data.sourceId -ne [int64] $source.id) {
        throw "LOG_INGESTED sourceId mismatch."
    }

    if ([int] $event.data.count -ne 2) {
        throw "LOG_INGESTED count mismatch."
    }

    Write-Host "PASS: CONNECTED and LOG_INGESTED received for sourceId=$($source.id), count=2."
} finally {
    $reader.Dispose()
    $response.Dispose()
    $client.Dispose()
}
