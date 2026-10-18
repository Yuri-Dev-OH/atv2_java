param([string]$BaseUrl = 'http://localhost:8081')
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$clienteUri = $null

# Somente a raiz e conhecida; todas as outras URIs vem dos links HATEOAS das respostas.
function Requisitar([string]$Metodo, [string]$Uri, $Corpo = $null, [string]$Aceita = 'application/hal+json') {
    $parametros = @{ Method = $Metodo; Uri = $Uri; Headers = @{ Accept = $Aceita } }
    if ($null -ne $Corpo) {
        $parametros.ContentType = 'application/json; charset=utf-8'
        $parametros.Body = [Text.Encoding]::UTF8.GetBytes(($Corpo | ConvertTo-Json -Depth 10))
    }
    Invoke-RestMethod @parametros
}
function Link($Recurso, [string]$Relacao) {
    $link = $Recurso._links.$Relacao
    if ($null -eq $link) { throw "Link '$Relacao' ausente." }
    $link.href -replace '\{.*\}', ''
}
function Conferir([bool]$Condicao, [string]$Mensagem) {
    if (!$Condicao) { throw $Mensagem }
}

try {
    $raiz = Requisitar GET "$BaseUrl/"
    $colecao = Link $raiz 'clientes'
    Write-Host "Nivel 3: raiz aponta para $colecao"

    $cliente = Requisitar POST $colecao @{ nome = 'Cliente demonstracao'; nomeSocial = 'Demo'; dataNascimento = '1995-04-10' }
    $clienteUri = Link $cliente 'self'
    $consultado = Requisitar GET $clienteUri
    Conferir ($consultado.nome -eq 'Cliente demonstracao') 'Falha na consulta do cliente.'
    $cliente = Requisitar PUT $clienteUri @{ nome = 'Cliente atualizado'; nomeSocial = ''; dataNascimento = '1995-04-10' }
    Conferir ($cliente.nome -eq 'Cliente atualizado') 'Falha na atualizacao do cliente.'
    $pagina = Requisitar GET "$colecao`?pagina=0&tamanho=10"
    Conferir ($pagina.page.totalElements -ge 1 -and $null -ne $pagina._links.first) 'Falha na listagem paginada.'

    $formulario = Requisitar GET $clienteUri -Aceita 'application/prs.hal-forms+json'
    Conferir ($formulario._templates.default.method -eq 'PUT' -and $formulario._templates.cadastrar.method -eq 'POST') 'Falha nos templates HAL-FORMS.'
    Write-Host 'HAL-FORMS: cliente anuncia PUT, DELETE e cadastro de endereco.'

    $enderecoUri = Link $cliente 'endereco'
    $endereco = @{ estado = 'SP'; cidade = 'Sao Paulo'; bairro = 'Centro'; rua = 'Rua Um'; numero = '10'; codigoPostal = '01001000'; informacoesAdicionais = 'Casa' }
    $criado = Requisitar POST $enderecoUri $endereco
    Conferir ((Link $criado 'cliente') -eq $clienteUri) 'Endereco sem link para o cliente.'
    $endereco.codigoPostal = '01311000'
    $null = Requisitar PUT (Link $criado 'self') $endereco
    $consultaEndereco = Requisitar GET $enderecoUri
    Conferir ($consultaEndereco.codigoPostal -eq '01311000') 'Falha na atualizacao do CEP.'

    $numero = [guid]::NewGuid().ToString('N')
    $documento = Requisitar POST (Link $cliente 'documentos') @{ tipo = 'RG'; numero = $numero }
    $documentoUri = Link $documento 'self'
    $null = Requisitar PUT $documentoUri @{ tipo = 'CNH'; numero = $numero }
    $consultaDocumento = Requisitar GET $documentoUri
    Conferir ($consultaDocumento.tipo -eq 'CNH') 'Falha na atualizacao do documento.'
    $listaDocumentos = Requisitar GET (Link $documento 'documentos')
    Conferir (@($listaDocumentos._embedded.documentos).Count -eq 1) 'Falha na listagem de documentos.'

    $telefone = Requisitar POST (Link $cliente 'telefones') @{ ddd = '11'; numero = '999998888' }
    $telefoneUri = Link $telefone 'self'
    $null = Requisitar PUT $telefoneUri @{ ddd = '12'; numero = '988887777' }
    $consultaTelefone = Requisitar GET $telefoneUri
    Conferir ($consultaTelefone.ddd -eq '12') 'Falha na atualizacao do telefone.'
    $listaTelefones = Requisitar GET (Link $telefone 'telefones')
    Conferir (@($listaTelefones._embedded.telefones).Count -eq 1) 'Falha na listagem de telefones.'

    $cabecalho = Invoke-WebRequest -Method Head -Uri $clienteUri -UseBasicParsing
    Conferir ($cabecalho.StatusCode -eq 200 -and $cabecalho.Content.Length -eq 0) 'Falha no HEAD.'
    try {
        $null = Invoke-WebRequest -Method Patch -Uri $clienteUri -ContentType 'application/json' -Body '{}' -UseBasicParsing
        throw 'PATCH deveria retornar 405.'
    } catch {
        $resposta = $_.Exception.Response
        Conferir ($null -ne $resposta -and [int]$resposta.StatusCode -eq 405) 'PATCH deveria retornar 405.'
    }
    Write-Host 'Nivel 2: HEAD 200 sem corpo e PATCH 405.'

    $null = Requisitar DELETE $documentoUri
    $null = Requisitar DELETE $telefoneUri
    $null = Requisitar DELETE $enderecoUri
    $final = Requisitar GET $clienteUri
    Conferir ($final.documentos.Count -eq 0 -and $final.telefones.Count -eq 0 -and $null -eq $final.endereco) 'Falha na exclusao dos dados vinculados.'
    $null = Requisitar DELETE $clienteUri
    $clienteUri = $null
    Write-Host 'CRUDs de cliente, endereco, documento e telefone verificados por HTTP, navegando apenas por links.'
} finally {
    if ($null -ne $clienteUri) { $null = Requisitar DELETE $clienteUri }
}
