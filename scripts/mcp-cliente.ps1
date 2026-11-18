# mcp-cliente.ps1 - Cliente MCP minimo para Windows PowerShell 5.1 (pruebas del servidor del supermercado).
# Uso en CADA ventana nueva:   . C:\Users\pablo\Documents\lab-mcp\supermercado-mcp\scripts\mcp-cliente.ps1
# (el punto inicial es importante: carga las funciones en la ventana actual)  y despues:  Start-Mcp
# Solo ASCII a proposito: evita problemas de codificacion al guardar el archivo.

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$url = "http://localhost:8089/mcp"
$script:sid = $null      # Mcp-Session-Id que entrega el servidor
$script:rid = 0          # contador de ids JSON-RPC
$script:last = $null     # ultimo objeto devuelto por una herramienta

# Envia un mensaje JSON-RPC y devuelve la respuesta ya convertida
function Invoke-Mcp {
    param([Parameter(Mandatory)][hashtable]$Body)

    $json    = $Body | ConvertTo-Json -Depth 10 -Compress
    $bytes   = [System.Text.Encoding]::UTF8.GetBytes($json)
    $headers = @{ "Accept" = "application/json, text/event-stream" }
    if ($script:sid) { $headers["Mcp-Session-Id"] = $script:sid }

    $r = Invoke-WebRequest -Uri $url -Method Post -Headers $headers `
         -ContentType "application/json; charset=utf-8" -Body $bytes -UseBasicParsing

    if ($r.Headers["Mcp-Session-Id"]) { $script:sid = $r.Headers["Mcp-Session-Id"] }
    Write-Host "[$($Body.method)] HTTP $($r.StatusCode)" -ForegroundColor DarkGray

    # Decodificamos los bytes como UTF-8 (si no, las tildes salen mal en PowerShell 5.1)
    $text = [System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
    if (-not $text.Trim()) { return $null }          # p. ej. 202 sin cuerpo

    if ($r.Headers["Content-Type"] -like "*text/event-stream*") {
        $text = (($text -split "\r?\n") | Where-Object { $_ -match '^data:' } |
                 ForEach-Object { $_ -replace '^data:\s?', '' }) -join ""
    }
    return ($text | ConvertFrom-Json)
}

# Handshake: initialize + notifications/initialized
function Start-Mcp {
    $script:sid = $null
    $script:rid += 1
    $null = Invoke-Mcp @{ jsonrpc="2.0"; id=$script:rid; method="initialize"; params=@{
        protocolVersion="2025-03-26"; capabilities=@{}
        clientInfo=@{ name="powershell-client"; version="1.0" } } }
    $null = Invoke-Mcp @{ jsonrpc="2.0"; method="notifications/initialized" }
    Write-Host "Sesion MCP: $script:sid" -ForegroundColor Green
}

# tools/list: muestra cada herramienta y los nombres de sus parametros
function Get-Tools {
    $script:rid += 1
    $resp = Invoke-Mcp @{ jsonrpc="2.0"; id=$script:rid; method="tools/list" }
    $resp.result.tools | Select-Object name,
        @{ n="parametros"; e={ ($_.inputSchema.properties.PSObject.Properties.Name) -join ", " } } |
        Format-Table -AutoSize
}

# Llama una herramienta y muestra el resultado
function Invoke-Tool {
    param([Parameter(Mandatory)][string]$Name, [hashtable]$ToolArgs = @{})

    $script:rid += 1
    $resp = Invoke-Mcp @{ jsonrpc="2.0"; id=$script:rid; method="tools/call"
                          params=@{ name=$Name; arguments=$ToolArgs } }
    $script:last = $null
    if ($resp.error) { Write-Host "ERROR JSON-RPC: $($resp.error.message)" -ForegroundColor Red; return }
    if ($resp.result.isError) { Write-Host ">> La herramienta devolvio isError=true" -ForegroundColor Yellow }

    foreach ($c in $resp.result.content) {
        $o = $null
        try { $o = $c.text | ConvertFrom-Json -ErrorAction Stop } catch {}
        if     ($o -is [string]) { Write-Host $o }
        elseif ($null -ne $o)    { $script:last = $o
                                   if ($o.PSObject.Properties.Name -contains "productos") { Show-Pedido } else { ($o | Format-Table -AutoSize | Out-String).TrimEnd() | Write-Host } }
        else                     { Write-Host $c.text }
    }
}

# Consulta directa a MySQL: la "verdad" con la que se contrasta lo que dice el servidor
# (necesita $env:MYSQL_PWD definida en esta ventana)
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
function Show-Sql {
    param([Parameter(Mandatory)][string]$Query)
    & $mysql -u root -h 127.0.0.1 -P 3306 --default-character-set=utf8mb4 -e "USE supermercado; $Query"
}
# Muestra el ultimo pedido devuelto por una herramienta: encabezado, lineas y total
function Show-Pedido {
    $p = $script:last
    if (-not $p) { return }
    Write-Host ("PEDIDO {0} | {1} | {2} | entrega {3} {4}" -f $p.id, $p.estado, $p.cliente, $p.fechaEntrega, $p.franja) -ForegroundColor Green
    Write-Host ("Direccion: {0}" -f $p.direccionEntrega) -ForegroundColor Green
    if ($p.productos.Count -gt 0) {
        $p.productos | Format-Table productoId, nombre, cantidad, unidad, precioUnitario, subtotal -AutoSize | Out-String | Write-Host
    } else { Write-Host "(sin productos)" }
    Write-Host ("TOTAL: {0}" -f $p.total) -ForegroundColor Green
}