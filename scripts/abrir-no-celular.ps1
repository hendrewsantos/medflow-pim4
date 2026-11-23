<#
  Abre o MedFlow para ser usado no celular, pelo navegador, sem instalar nada no aparelho.
  O celular e o computador precisam estar na MESMA rede Wi-Fi.

  Uso (PowerShell, a partir da raiz do repositório):
    ./scripts/abrir-no-celular.ps1                 sobe banco, API e site e mostra o endereço para digitar no celular
    ./scripts/abrir-no-celular.ps1 -SomenteMostrar só descobre e mostra o endereço (não sobe nada)

  Requisitos: Docker Desktop aberto, .NET 8 SDK e Node 22 ou mais novo.
  Senha do MySQL: usa a variável MYSQL_ROOT_PASSWORD, ou o padrão de desenvolvimento do docker-compose.yml.
#>
param([switch]$SomenteMostrar)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

# Endereço IPv4 da rede local (ignora loopback, link-local e adaptadores virtuais de Docker, WSL e VPN).
$candidatos = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
  Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' -and $_.InterfaceAlias -notmatch 'vEthernet|WSL|Docker|VirtualBox|VMware|Loopback|Hyper-V|Tailscale|VPN' }
$ip = ($candidatos | Sort-Object { if ($_.InterfaceAlias -match 'Wi-?Fi|Wireless|Sem fio') { 0 } else { 1 } } | Select-Object -First 1).IPAddress
if (-not $ip) { throw 'Não encontrei o IP da rede local. Conecte o computador ao Wi-Fi e tente de novo.' }

Write-Host ''
Write-Host "IP deste computador na rede: $ip"
Write-Host "No celular (mesmo Wi-Fi), abra:  http://$($ip):5173" -ForegroundColor Green
if ($SomenteMostrar) { return }

$senha = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { 'troque-esta-senha' }

Write-Host 'Subindo o banco (MySQL em contêiner, porta 3307)...'
Push-Location $raiz
docker compose up -d mysql | Out-Null
Pop-Location
Start-Sleep -Seconds 15

Write-Host 'Subindo a API (porta 5080, aceitando conexões da rede)...'
$env:ASPNETCORE_ENVIRONMENT = 'Development'
$env:ConnectionStrings__MedFlow = "server=localhost;port=3307;database=medflow;user=root;password=$senha"
$env:Jwt__Chave = 'chave-local-de-teste-com-pelo-menos-32-caracteres'
$env:ASPNETCORE_URLS = 'http://0.0.0.0:5080'
$env:Cors__Origens__0 = 'http://localhost:5173'
$env:Cors__Origens__1 = "http://$($ip):5173"
Start-Process -FilePath dotnet -WorkingDirectory "$raiz\backend\src\MedFlow.Api" -ArgumentList 'run', '--no-launch-profile'

Write-Host 'Subindo o site (porta 5173, aceitando conexões da rede)...'
$env:VITE_API_URL = "http://$($ip):5080"
$env:VITE_BOTOES_PAGAMENTO = 'true'
Start-Process -FilePath npm.cmd -WorkingDirectory "$raiz\frontend" -ArgumentList 'run', 'dev', '--', '--host'

Write-Host ''
Write-Host "Pronto. Em alguns segundos, no celular (mesmo Wi-Fi): http://$($ip):5173" -ForegroundColor Green
Write-Host 'Se não abrir, aceite o aviso do Firewall do Windows (redes privadas) ou libere as portas 5080 e 5173.'
Write-Host 'Contas de teste: veja a seção "Contas de teste" do README.'
