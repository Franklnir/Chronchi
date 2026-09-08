param(
    [string]$OutputDirectory = "$PSScriptRoot\..\app\src\main\res\drawable-nodpi"
)

$ErrorActionPreference = "Stop"

$apps = @(
    @{ Id = "whatsapp_business"; Package = "com.whatsapp.w4b" },
    @{ Id = "telegram"; Package = "org.telegram.messenger" },
    @{ Id = "instagram"; Package = "com.instagram.android" },
    @{ Id = "facebook"; Package = "com.facebook.katana" },
    @{ Id = "messenger"; Package = "com.facebook.orca" },
    @{ Id = "tiktok"; Package = "com.zhiliaoapp.musically" },
    @{ Id = "x"; Package = "com.twitter.android" },
    @{ Id = "threads"; Package = "com.instagram.barcelona" },
    @{ Id = "gmail"; Package = "com.google.android.gm" },
    @{ Id = "linkedin"; Package = "com.linkedin.android" },
    @{ Id = "jobstreet"; Package = "com.jobstreet.jobstreet" },
    @{ Id = "dana"; Package = "id.dana" },
    @{ Id = "ovo"; Package = "ovo.id" },
    @{ Id = "gojek"; Package = "com.gojek.app" },
    @{ Id = "shopee"; Package = "com.shopee.id" },
    @{ Id = "mybca"; Package = "com.bca.mybca.omni.android" },
    @{ Id = "bca_mobile"; Package = "com.bca" },
    @{ Id = "brimo"; Package = "id.co.bri.brimo" },
    @{ Id = "livin"; Package = "id.bmri.livin" },
    @{ Id = "wondr"; Package = "id.bni.wondr" },
    @{ Id = "octo"; Package = "id.co.cimbniaga.mobile.android" },
    @{ Id = "seabank"; Package = "id.co.bankbkemobile.digitalbank" },
    @{ Id = "jago"; Package = "com.jago.digitalBanking" },
    @{ Id = "superbank"; Package = "id.co.bankfama.android" },
    @{ Id = "shopee_partner"; Package = "com.shopeepay.merchant.id" },
    @{ Id = "tokopedia"; Package = "com.tokopedia.tkpd" },
    @{ Id = "lazada"; Package = "com.lazada.android" },
    @{ Id = "grab"; Package = "com.grabtaxi.passenger" }
)

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromMinutes(5)
$client.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0")
$client.DefaultRequestHeaders.AcceptLanguage.ParseAdd("id-ID,id;q=0.9,en;q=0.8")

try {
    $pageRequests = foreach ($app in $apps) {
        $uri = "https://play.google.com/store/apps/details?id=$($app.Package)&hl=id&gl=ID"
        [PSCustomObject]@{
            App = $app
            Task = $client.GetStringAsync($uri)
        }
    }

    $imageRequests = foreach ($request in $pageRequests) {
        try {
            $html = $request.Task.GetAwaiter().GetResult()
            $match = [regex]::Match($html, '<meta[^>]+property="og:image"[^>]+content="([^"]+)"')
            if (-not $match.Success) {
                throw "Google Play icon metadata not found"
            }
            $iconUrl = [System.Net.WebUtility]::HtmlDecode($match.Groups[1].Value)
            $iconUrl = $iconUrl -replace '=s0[^=]*$', '=s192-rw'
            [PSCustomObject]@{
                App = $request.App
                Task = $client.GetByteArrayAsync($iconUrl)
            }
        } catch {
            Write-Host "FAILED_METADATA $($request.App.Id) $($_.Exception.Message)"
        }
    }

    foreach ($request in $imageRequests) {
        try {
            $bytes = $request.Task.GetAwaiter().GetResult()
            if ($bytes.Length -lt 128) {
                throw "Downloaded icon is unexpectedly small"
            }

            $extension = if (
                $bytes.Length -ge 12 -and
                [Text.Encoding]::ASCII.GetString($bytes, 0, 4) -eq "RIFF" -and
                [Text.Encoding]::ASCII.GetString($bytes, 8, 4) -eq "WEBP"
            ) {
                "webp"
            } elseif ($bytes[0] -eq 0x89 -and $bytes[1] -eq 0x50) {
                "png"
            } elseif ($bytes[0] -eq 0xFF -and $bytes[1] -eq 0xD8) {
                "jpg"
            } else {
                throw "Unsupported image format"
            }

            $path = Join-Path $OutputDirectory "ic_app_$($request.App.Id).$extension"
            [System.IO.File]::WriteAllBytes($path, $bytes)
            Write-Output "DOWNLOADED $($request.App.Id) $($bytes.Length) $path"
        } catch {
            Write-Output "FAILED_IMAGE $($request.App.Id) $($_.Exception.Message)"
        }
    }
} finally {
    $client.Dispose()
}
