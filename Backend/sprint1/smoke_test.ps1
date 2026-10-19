$ErrorActionPreference = 'SilentlyContinue'
$MONGO_URI = 'mongodb://localhost:27017'
$JWT_SECRET = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 })) }
$baseUrl = 'http://localhost:8080/api/v1'

Write-Host 'Checking backend status...'
try {
    Invoke-WebRequest -Uri 'http://localhost:8080/api-docs' -TimeoutSec 2 > $null
    Write-Host 'Backend is already UP.'
} catch {
    Write-Host 'Backend is DOWN. Starting backend...'
    $env:MONGO_URI = $MONGO_URI
    $env:JWT_SECRET = $JWT_SECRET
    Start-Process -FilePath 'mvn' -ArgumentList 'spring-boot:run' -WorkingDirectory 'c:\Users\AkshayAdak\Desktop\sprint22\sprint1' -WindowStyle Hidden
    
    $timeout = 90
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        try {
            Invoke-WebRequest -Uri 'http://localhost:8080/api-docs' -Method Get -TimeoutSec 2 > $null
            Write-Host 'Backend is now UP.'
            break
        } catch {
            Start-Sleep -Seconds 5
            $elapsed += 5
        }
    }
}

Write-Host 'Testing endpoints...'
# 2. Login
$loginBody = @{ email = 'admin@mail.com'; password = $env:SEED_ADMIN_PASSWORD } | ConvertTo-Json
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/auth/sign-in" -Method Post -ContentType 'application/json' -Body $loginBody
    $token = $res.token
    Write-Host "[PASS] Authentication successful."
} catch {
    Write-Host "[FAIL] Authentication failed: $(.Exception.Message)"
    exit
}

# 3. Reports
try {
    $perf = Invoke-WebRequest -Uri "$baseUrl/reports/staff-performance?from=2025-01-01&to=2025-01-31" -Headers @{Authorization="Bearer $token"}
    $data = $perf.Content | ConvertFrom-Json
    Write-Host "Staff Performance: Status=$(.StatusCode), Rows=$(.Count)"
} catch { Write-Host "Staff Performance: Status=$(.Exception.Response.StatusCode.value__)" }

try {
    $sales = Invoke-WebRequest -Uri "$baseUrl/reports/sales?from=2025-01-01&to=2025-01-31" -Headers @{Authorization="Bearer $token"}
    $data = $sales.Content | ConvertFrom-Json
    Write-Host "Sales Report: Status=$(.StatusCode), Rows=$(.Count)"
} catch { Write-Host "Sales Report: Status=$(.Exception.Response.StatusCode.value__)" }

# 4. Public Feedback
try {
    $fb = Invoke-WebRequest -Uri "$baseUrl/tours/tour001/feedback"
    $data = $fb.Content | ConvertFrom-Json
    Write-Host "Public Feedback (Tour): Status=$(.StatusCode), Rows=$(.Count)"
} catch { Write-Host "Public Feedback (Tour): Status=$(.Exception.Response.StatusCode.value__)" }

# 5. POST Feedback
$fbBody = @{ rating = 5; comment = 'great trip' } | ConvertTo-Json
try {
    $res = Invoke-WebRequest -Uri "$baseUrl/bookings/book0001/feedback" -Method Post -ContentType 'application/json' -Body $fbBody
    Write-Host "POST Feedback (No Auth): Status=$(.StatusCode)"
} catch {
    Write-Host "POST Feedback (No Auth): Status=$(.Exception.Response.StatusCode.value__)"
}

try {
    $res = Invoke-WebRequest -Uri "$baseUrl/bookings/book0001/feedback" -Method Post -Headers @{Authorization="Bearer $token"} -ContentType 'application/json' -Body $fbBody
    Write-Host "POST Feedback (Auth): Status=$(.StatusCode), Response=$(.Content)"
} catch {
    Write-Host "POST Feedback (Auth): Status=$(.Exception.Response.StatusCode.value__)"
}

# 6. GET Feedback for Booking
try {
    $res = Invoke-WebRequest -Uri "$baseUrl/bookings/book0001/feedback" -Headers @{Authorization="Bearer $token"}
    Write-Host "GET Booking Feedback: Status=$(.StatusCode), Response=$(.Content)"
} catch {
    Write-Host "GET Booking Feedback: Status=$(.Exception.Response.StatusCode.value__)"
}
