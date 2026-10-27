# IERA App — PRD e MVP

Ferramenta de extração de hinários por IA (Gemini gratuito) + Tesseract, revisão no painel, JSON novo, Constituição e redesign do mobile.

> Complementa `IERA-App_plano-tecnico.md`. Onde houver conflito, **este documento prevalece** (Java apenas; Gemini como motor principal).
> Estado: pronto para começar a Fase 0.

---

## 0. Decisões fechadas

| Tema | Decisão |
|---|---|
| Stack | **Só Java**: Spring Boot (já existente) + painel Angular + app Ionic/Angular. Sem Python. |
| Motor de leitura principal | **API do Gemini**, **apenas modelos com plano gratuito**, rodando entre eles quando a quota de um acaba. |
| Segundo leitor | **Tesseract (Tess4J)**, em local, para comparação. |
| Regra de ouro | A IA **nunca** escreve no JSON final. Tudo passa por revisão humana. |
| Nome da app | **IERA App**. Ainda não publicada (o `appId` pode ser definido agora). |
| Texto | Preservado exatamente como impresso: acentos, apóstrofos, pontuação, quebras de linha, números de estrofe. |

Aviso sobre o plano gratuito: no plano gratuito, a Google pode usar o conteúdo enviado para melhorar os seus produtos. As páginas de um hinário impresso não têm dados pessoais, mas a decisão de enviar as imagens é tua. A extração é a única fase que usa a internet.

---

## 1. Problema e objetivos

**Problema.** O JSON atual de hinos tem erros, hinos incompletos e uma estrutura que impede apresentar as estrofes como poesia. Digitar à mão centenas de páginas é lento e propenso a erros.

**Objetivos do MVP**

1. Transformar fotografias de páginas do hinário em texto estruturado **sem alterar nada do que está impresso**.
2. Manter a ordem por número de página e ligar continuações de hinos entre páginas.
3. Permitir rever cada linha ao lado da foto, corrigir e confirmar.
4. Gerar `hinario-<id>.json` no esquema novo, validado.
5. Migrar e diagnosticar os dados atuais (órfãs, numeração, duplicados).

**Fora do MVP (fases posteriores):** Constituição na app, redesign do mobile, atualização de conteúdo sem nova versão da loja.

**Métricas (metas iniciais, a calibrar com o conjunto de referência da Fase 3)**

- Zero alterações silenciosas: toda a linha exportada foi confirmada por ti.
- Pelo menos 90% das linhas aceites sem edição manual (se ficar abaixo, rever o prompt, o motor ou a captura).
- 100% das divergências Gemini vs Tesseract visíveis no painel.
- Nenhum hino exportado com numeração de estrofes descontínua.

**Utilizador.** Uma só pessoa (tu), na tua máquina.

---

## 2. Arquitetura

```
painel (Angular) ──HTTP──► back-end Spring Boot (Java 17)
                             ├─ pagina      importação, estados, fila
                             ├─ leitura     GeminiProvider ─► API do Gemini (internet, só extração)
                             │              TesseractProvider (local)
                             ├─ comparacao  alinhamento linha a linha, divergências
                             ├─ estrutura   hinos, estrofes, coro, continuação
                             ├─ migracao    JSON atual ─► esquema novo + relatório
                             └─ exportacao  validação + hinario-*.json
                                   │
                                   ▼
                          pasta de dados local (imagens, leituras, staging, export)
```

**Pacotes Java (sugestão)**: `...pagina`, `...leitura` (`OcrProvider`, `GeminiProvider`, `TesseractProvider`, `ModelPool`), `...comparacao`, `...estrutura`, `...migracao`, `...exportacao`, `...config`.

**Configuração (`application.properties`)**

```properties
hinario.dados.dir=${user.home}/iera-dados
# Chave nunca no código nem no painel: variável de ambiente
gemini.enabled=true
gemini.api-key=${GEMINI_API_KEY}
# Ordem de preferência. Validar os nomes no AI Studio antes de usar (ver 5.8).
gemini.modelos=gemini-3.8-flash,gemini-3.6-flash,gemini-3.5-flash-lite,gemini-3.1-flash-lite
gemini.tentativas-por-modelo=2
gemini.espera-429-minuto-segundos=65
tesseract.idioma=por
tesseract.datapath=/usr/share/tesseract-ocr/5/tessdata
```

Requisitos na máquina: Java 17, motor Tesseract e ficheiros `.traineddata`.

---

## 3. Modelo de dados

### 3.1 Pasta de dados

```
iera-dados/
  imagens/original/        fotografias originais (nunca alteradas)
  imagens/processada/      versões tratadas
  paginas/0395.json        estado + leituras + estrutura da página (staging)
  hinarios/kmb.staging.json
  export/hinario-kmb.json
  quota/estado.json        contadores e pausas por modelo
  cache/<hash>.json        respostas da IA por hash da imagem
  relatorios/migracao.md
```

### 3.2 Página (staging): `paginas/0395.json`

```json
{
  "pagina": 395,
  "hinarioId": "kmb",
  "imagem": "imagens/original/0395.jpg",
  "hash": "sha256:...",
  "estado": "em_revisao",
  "leituras": {
    "gemini": {
      "modelo": "gemini-3.8-flash",
      "tentativas": [{ "modelo": "...", "resultado": "ok|429|erro|bloqueado", "quando": "..." }],
      "resposta": { }
    },
    "tesseract": { "linhas": [ { "texto": "...", "confianca": 0.91 } ] }
  },
  "divergencias": [
    { "bloco": 0, "linha": 2, "gemini": "Sai-ku akua diniota", "tesseract": "Sai-ku akua diniota.", "tipo": "pontuacao" }
  ],
  "blocos": [ ]
}
```

`estado`: `importada` → `em_fila` → `lida` → `comparada` → `estruturada` → `em_revisao` → `confirmada`. Estados de erro: `pausada_quota`, `erro_leitura`, `bloqueada_pela_ia`.

### 3.3 Esquema de saída (export)

Conforme `IERA-App_plano-tecnico.md`, secção 4.1: hinos com `blocos[]` (`estrofe` | `coro`) e `linhas[]` com `texto` e `recuo`. Todo o texto em Unicode NFC.

---

## 4. Fluxo do utilizador

1. Importa fotos (uma página por foto), indica o número da página, roda ou recorta se preciso.
2. A página entra na fila; o back-end chama o Gemini e o Tesseract.
3. O sistema compara as duas leituras e propõe a estrutura (hino, estrofes, possível coro).
4. Tu reves linha a linha, com a foto ao lado, e confirmas.
5. Exportas o JSON do hinário.

---

## 5. Especificação da IA (rigorosa)

### 5.1 Princípios

1. **Transcrever, nunca interpretar.** O modelo é um leitor, não um editor.
2. **Não corrigir** ortografia, acentos, apóstrofos, hífenes nem pontuação, mesmo que pareçam erros.
3. **Não traduzir** nem "normalizar" para português.
4. **Não completar** texto cortado ou ilegível: marcar e declarar dúvida.
5. **Saída só em JSON** no esquema definido. Nada de Markdown ou comentários.
6. **Quem decide a classificação é o humano.** O modelo descreve o que vê (por exemplo "bloco sem número, itálico"). O Java propõe "possível coro"; tu confirmas.

### 5.2 Pedido à API

- Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent`
- Cabeçalho: `x-goog-api-key: <chave>` (lida da variável de ambiente no back-end).
- Uma **página por pedido**; imagem em JPEG (base64), lado maior até cerca de 2000 px.
- Pedido (estrutura):

```json
{
  "systemInstruction": { "parts": [ { "text": "<5.3: instrução do sistema>" } ] },
  "contents": [ {
    "role": "user",
    "parts": [
      { "inlineData": { "mimeType": "image/jpeg", "data": "<base64>" } },
      { "text": "<5.5: contexto da página>" }
    ]
  } ],
  "generationConfig": {
    "responseMimeType": "application/json",
    "responseSchema": { "<5.4>": "..." },
    "candidateCount": 1
  }
}
```

- **Temperatura**: configurável **por modelo**. Usar 0 quando a documentação do modelo o permitir; se a documentação de uma família recomendar manter o valor por defeito, respeitar e compensar com o prompt, a validação e a comparação com o Tesseract.
- Raciocínio/"thinking" no mínimo permitido pelo modelo (a tarefa é de leitura, não de análise).
- Desativar qualquer ferramenta (pesquisa na web, execução de código).

### 5.3 Instrução do sistema (texto a usar)

```
És um leitor de texto impresso. Recebes a fotografia de UMA página de um hinário em línguas
angolanas (Kimbundu, Kikongo) e/ou português. A tua única tarefa é transcrever o que está
impresso, exatamente como está. Não és editor, corretor nem tradutor.

REGRAS ABSOLUTAS
1. Transcreve cada letra, acento, apóstrofo, hífen e sinal de pontuação tal como aparecem.
   Mantém maiúsculas e minúsculas como impressas (os títulos de hino estão em maiúsculas).
2. NÃO corrijas ortografia, acentuação ou pontuação, mesmo que te pareça um erro.
   NÃO normalizes para português. NÃO traduzas. NÃO modernizes a grafia.
   Palavras que não conheces transcrevem-se letra a letra.
3. Preserva letras e sinais menos comuns (por exemplo á é í ó ú â ê ô ò ã õ ç ń) e os
   apóstrofos dentro e no início de palavras (por exemplo "m’ uhukunii", "’Zuua").
   Usa o apóstrofo tipográfico ’ (U+2019). Se não conseguires distinguir ’ de ', usa ’ e
   regista uma dúvida.
4. Mantém os espaços como impressos: um espaço entre palavras; sem espaços extra nem em falta.
5. UMA linha impressa = UMA entrada em "linhas". Nunca juntes duas linhas impressas e nunca
   dividas uma linha impressa em duas.
6. O número de estrofe (1, 2, 3…) vai SÓ no campo "numeroEstrofe". Nunca o incluas no texto.
   O número do hino e o título vão nos campos próprios.
7. "recuo": 0 se a linha começa na margem normal do bloco; 1 se está recuada; 2 se está
   recuada ainda mais. É relativo às outras linhas do mesmo bloco.
8. Se não conseguires ler um carácter, escreve [?] nesse lugar e regista uma dúvida.
   NUNCA adivinhes nem completes palavras cortadas pela margem ou por sombra.
9. IGNORA: o texto fantasma do verso da folha que transparece, o texto da página vizinha
   visível ao lado, dedos, sombras, e o número de página no rodapé (esse vai em
   "numeroPaginaImpresso"). Texto grande no topo da página que não pertence a nenhum hino
   (título de secção ou título corrente) vai em "cabecalhoPagina".
10. Descreve o que vês; não classifiques além do pedido. Para blocos de texto sem número
    de estrofe, usa o tipo "bloco_sem_numero" e indica "italico" (true/false) conforme as
    letras estejam inclinadas. NÃO decidas tu se é coro.
11. Se o primeiro bloco da página não tem título de hino nem número de estrofe, ou começa
    a meio de uma estrofe, marca "continuacaoDeBlocoAnterior": true nesse bloco.
12. Responde APENAS com JSON válido no esquema fornecido. Sem Markdown, sem explicações,
    sem texto fora do JSON.
```

### 5.4 Esquema de resposta

```json
{
  "numeroPaginaImpresso": 395,
  "cabecalhoPagina": null,
  "blocos": [
    {
      "tipo": "estrofe",
      "numeroHino": null,
      "titulo": null,
      "numeroEstrofe": 2,
      "italico": false,
      "continuacaoDeBlocoAnterior": true,
      "linhas": [ { "texto": "...", "recuo": 0 } ],
      "duvidas": [ { "linha": 0, "trecho": "...", "motivo": "..." } ]
    }
  ]
}
```

- `tipo`: `titulo_hino` | `estrofe` | `bloco_sem_numero`.
- Em `titulo_hino`: preencher `numeroHino` e `titulo`; `linhas` vazio.
- `numeroHino`, `titulo`, `numeroEstrofe`, `numeroPaginaImpresso`, `cabecalhoPagina` podem ser `null`.
- O esquema é enviado em `responseSchema` (usar o formato aceite pela API) **e** revalidado no Java.

### 5.5 Contexto da página (mensagem de texto junto da imagem)

```
Contexto (NÃO transcrever; serve só para o campo "continuacaoDeBlocoAnterior"):
- Esta é a página {N} do hinário "{nome}".
- Hino aberto no fim da página anterior: nº {X}, "{título}", última estrofe: {Y}.
(ou) - Não há hino aberto da página anterior.
Transcreve apenas o que vês na imagem, seguindo as regras.
```

O contexto **nunca** é usado para preencher texto: só ajuda a marcar continuidade. A validação final da ligação é feita no Java (5.7).

### 5.6 Falhas e respostas inválidas

| Situação | Ação |
|---|---|
| JSON inválido ou fora do esquema | repetir **uma vez** no mesmo modelo; depois passar ao seguinte da cadeia |
| Resposta bloqueada (por segurança ou por semelhança com texto protegido) | marcar a tentativa, passar ao modelo seguinte; se todos bloquearem, página fica `bloqueada_pela_ia` e segue só com Tesseract |
| Texto com Markdown (`**`, `#`, crases) ou com numeração dentro do texto | rejeitar e repetir |
| Resposta vazia | tratar como erro e repetir |

### 5.7 Pós-validação (Java, determinística)

1. **Normalização**: aplicar Unicode NFC (não altera nenhum carácter visível). Nenhuma outra alteração de texto.
2. **Alfabeto**: assinalar qualquer carácter fora do conjunto esperado (letras latinas com os diacríticos conhecidos, dígitos, pontuação comum, `’`, `-`). Não remover; só marcar.
3. **Comparação com o Tesseract**: alinhar as linhas (alinhamento por distância de edição) e classificar cada par: `igual`, `so_acento`, `so_pontuacao`, `divergente`. **Qualquer diferença é mostrada** no painel; as de acento e pontuação têm destaque, porque são o risco que mais importa.
4. **Sequência de estrofes**: dentro de cada hino, 1, 2, 3… sem buracos nem repetições. Página que abre na estrofe N exige que o hino aberto da página anterior tenha terminado na N−1; caso contrário, pede confirmação em vez de juntar.
5. **Linhas suspeitas**: vazias, com `[?]`, com um único carácter, com dígitos no meio de palavras.
6. **Coro (proposta)**: `bloco_sem_numero` entre estrofes numeradas, com `italico` ou `recuo` ⇒ "possível coro". Tu confirmas.

### 5.8 Modelos gratuitos e rotação

**Realidade a ter presente.** A lista de modelos gratuitos muda com frequência, e as fontes consultadas em 27/10/2026 não coincidem em tudo (por exemplo, um modelo já agendado para ser retirado em 16/10/2026). **Os limites por modelo só aparecem no AI Studio** (página de limites). Por isso:

- A cadeia de modelos vem **da configuração** (`gemini.modelos`), nunca fixa no código.
- Antes de começar, abrir o AI Studio, confirmar que cada modelo da lista tem plano gratuito para a tua conta e anotar os limites.
- A API tem uma chamada para listar modelos (`GET /v1beta/models`); usá-la para confirmar que o nome existe. Ela **não** diz se é gratuito.
- Pedidos **só com modelos gratuitos**. Nunca ativar faturação por engano: a chave deve vir de um projeto sem faturação.
- Os limites aplicam-se **por projeto**, não por chave. Não criar projetos ou contas adicionais para contornar quotas.
- Ordenar a cadeia pela **qualidade medida** no conjunto de referência (5.10), não pelo nome.

**Estado da quota (`quota/estado.json`)**: por modelo, contadores de pedidos por minuto e por dia, e `pausadoAte`. Guardado em disco, para sobreviver a reinícios.

**Algoritmo do `ModelPool`**

1. Para cada pedido, escolher o **primeiro modelo da cadeia** que não esteja pausado.
2. **429 por minuto** (limite de pedidos ou de tokens por minuto): esperar `gemini.espera-429-minuto-segundos` e repetir no mesmo modelo, até `tentativas-por-modelo`.
3. **429 por dia** (quota diária esgotada): marcar o modelo `pausadoAte` o próximo reinício diário (normalmente à meia-noite do fuso do Pacífico; confirmar na documentação) e passar ao modelo seguinte.
4. **503 ou erro temporário**: repetir com espera crescente; depois passar ao seguinte.
5. **Todos os modelos pausados**: a fila **para**; as páginas ficam `pausada_quota`; o painel mostra "Quota esgotada, retoma às HH:MM". A fila retoma sozinha.
6. **Nunca misturar modelos na mesma página**: se um modelo falha a meio, a página volta a ser lida de início por outro modelo.
7. Guardar sempre `modeloUsado` e a lista de tentativas na página (rastreabilidade).

**Cache.** Chave = hash da imagem + versão do prompt + modelo. Reprocessar a mesma imagem não gasta quota.

**Fila.** Sequencial, um pedido de cada vez (os limites por minuto são baixos). Prioridade por número de página.

### 5.9 Segurança e privacidade

- Chave em variável de ambiente (`GEMINI_API_KEY`), fora do repositório (`.gitignore`) e **nunca no painel Angular**. Nos registos, mascarar a chave.
- Enviar só a imagem da página e o contexto da secção 5.5. Nenhum outro dado.
- Interruptor `gemini.enabled=false` para voltar a modo só-Tesseract.
- Aviso na primeira utilização: o plano gratuito pode usar o conteúdo enviado para melhorar produtos da Google.

### 5.10 Conjunto de referência (portão de qualidade)

- 10 a 20 páginas representativas, **transcritas e verificadas por ti** (o "texto de ouro"): início de hino, continuação, coro em itálico, duas músicas na mesma página, linhas recuadas, sombras, página inclinada.
- Medir, por modelo: erro por carácter, **erros de acento** à parte, erros de pontuação, linhas juntas ou divididas, correções indevidas (palavra "arranjada").
- Ordenar a cadeia de modelos pelos resultados.
- **Critério de paragem**: se o Gemini "corrigir" palavras de forma sistemática em Kimbundu ou Kikongo, ou ficar abaixo da meta da secção 1, reavaliar: Tesseract como principal e Gemini só como segundo leitor.

---

## 6. Fases do MVP (sequenciais)

Cada fase só começa quando a anterior cumpre os critérios de aceitação.

### Fase 0: Preparação (meio dia)

**Tarefas**
- Corrigir o CSS do leitor atual (`white-space: pre-line; text-align: left`) para ver já os hinos linha a linha.
- Organizar o repositório: `mobile`, `painel`, `back-end`; README com como correr cada parte; `.gitignore` com chaves e pasta de dados.
- Criar a chave no AI Studio (projeto **sem faturação**) e anotar os modelos gratuitos e os limites.
- Instalar Tesseract e `.traineddata` (por exemplo `por`).
- Definir o `appId` da IERA App.

**Aceitação**: o leitor mostra estrofes linha a linha; o README permite arrancar tudo; limites anotados.

### Fase 1: Base do back-end e migração dos dados atuais

**Tarefas**
- `application.properties` com `hinario.dados.dir` (substituir os caminhos fixos); gravação atómica de ficheiros.
- Records Java para o esquema novo e validação (Bean Validation + verificações próprias).
- Comando de migração (`CommandLineRunner`): `hinario.json` + `estrofes.json` ⇒ `hinario-*.json` novo; mapeamento de línguas (`KINBUMDU` ⇒ `kmb`); estrofes ordenadas pela ordem original; coro vira bloco `coro`.
- Relatório `migracao.md`: órfãs, buracos e repetições de numeração, números duplicados e fora do intervalo, estrofes sem `\n` com mais de 80 caracteres, espaços a mais, cópias de hino divergentes.
- Corrigir `getAutor().equals(null)` e a pesquisa sensível a acentos.

**Aceitação**: ficheiros novos válidos; relatório gerado; nenhum texto alterado pela migração (comparar contagem de caracteres antes e depois).

### Fase 2: Importação de páginas

**Tarefas**
- Endpoint de upload (`POST /paginas`): imagem, número da página, hinário. Guarda o original, calcula o hash, cria o ficheiro da página em `importada`.
- Pré-processamento mínimo com JavaCV: rodar, converter para JPEG com lado maior até 2000 px, melhorar contraste. Recorte **manual** no painel (o automático fica para depois).
- Importação em lote (várias fotos), sempre ordenadas pelo número de página.

**Aceitação**: uma foto entra, fica guardada, aparece na lista com estado e com o número de página correto.

### Fase 3: Leitura com Gemini (o núcleo)

**Tarefas**
- `OcrProvider` (interface) e `GeminiProvider` com o pedido de 5.2, a instrução 5.3, o esquema 5.4 e o contexto 5.5.
- `ModelPool` com rotação de modelos, estado de quota em disco e cache (5.8).
- Fila sequencial e estados `em_fila`, `lida`, `pausada_quota`, `erro_leitura`, `bloqueada_pela_ia`.
- Validação de esquema e rejeição de respostas inválidas (5.6).
- **Portão de qualidade**: correr o conjunto de referência (5.10) e decidir a ordem da cadeia.

**Aceitação**: cumpre as metas da secção 1 no conjunto de referência; um teste forçado de 429 mostra a passagem automática para outro modelo; ao esgotar todos, a fila pausa e retoma.

### Fase 4: Tesseract e comparação

**Tarefas**
- `TesseractProvider` (Tess4J) com linhas e confiança.
- Alinhamento linha a linha e classificação de divergências (`so_acento`, `so_pontuacao`, `divergente`).
- Verificação do alfabeto (5.7).

**Aceitação**: no conjunto de referência, todas as diferenças de acento e pontuação entre as duas leituras aparecem listadas.

### Fase 5: Estruturação e continuação

**Tarefas**
- Montar hinos a partir dos blocos: título, estrofes, `recuo`; "possível coro" pela regra 5.7.6.
- Ligar páginas por ordem numérica: página que abre em continuação anexa ao hino aberto; validar a sequência de estrofes; pedir confirmação nas dúvidas.
- Detetar vários hinos na mesma página; marcar estrofe incompleta no fim.

**Aceitação**: o par de páginas 394/395 de exemplo gera um só hino 42 completo e o início do hino 43 (com a página 395 a abrir na estrofe 2); casos duvidosos ficam marcados, nunca juntos por suposição.

### Fase 6: Painel Angular, importação e revisão

**Tarefas**
- Secção **OCR**: lista de páginas com estado e fila (com aviso de quota).
- Ecrã de revisão em três painéis: lista de páginas, **foto com zoom**, editor de linhas com estrutura do hino.
- Editor linha a linha mostrando Gemini e Tesseract; diferenças de acento e pontuação destacadas; linhas com `[?]` e dúvidas em evidência.
- Paleta de carateres (á é í ó ú â ê ô ã õ ç ò ń ’) para inserir no cursor; atalhos de teclado (confirmar, marcar coro, recuo, página seguinte).
- Edição do hino em texto inteiro além do formulário por estrofe.
- Confirmar: marca as linhas como `revisto`.

**Aceitação**: uma página completa passa de foto a texto confirmado só com o painel; o texto original da leitura é preservado mesmo depois de corrigires.

### Fase 7: Exportação e comparação com o JSON atual

**Tarefas**
- `POST /exportar`: só linhas `revisto`; validações finais (numeração contínua, sem duplicados por língua, NFC, sem espaços a mais, esquema válido); gera `hinario-<id>.json`.
- Vista de comparação por hino: leitura nova contra o JSON atual, com diferenças destacadas, para corrigir erros e preencher hinos incompletos.
- Teste de compatibilidade: abrir o JSON exportado no leitor da app mobile.

**Aceitação**: JSON exportado válido e aberto no leitor; ao menos um hino antes incompleto fica completo e confirmado.

> **Fim do MVP da ferramenta.** Daqui para a frente, o trabalho é produzir conteúdo (rever e completar hinos) e a app.

### Fase 8: Constituição

**Tarefas**
- Limpar `constituicao_iera_estruturada.json`: separar títulos colados ao texto (26 artigos e 14 capítulos), remover a nota editorial do comentário do Word, juntar as linhas do apêndice "EMENDAS", rever hifenizações.
- IDs compostos (`I-art-1`), porque os números de artigo repetem-se por parte.
- Gerar `constituicao.json` conforme o plano técnico (secção 6.3).
- Revisão do texto por um responsável da igreja antes de publicar.

**Aceitação**: ficheiro limpo, sem alteração de texto além de estrutura; aprovado por quem de direito.

### Fase 9: IERA App (redesign do mobile)

**Tarefas**
- Renomear para **IERA App** (`appName`, `appId`, manifesto, ícone, título).
- Navegação por separadores (Início, Hinos, Constituição, Favoritos); "Ir ao nº"; pesquisa sem acentos.
- Leitor de poesia com `recuo`, coro destacado, seguinte/anterior no mesmo hinário, tamanho da letra e tema guardados.
- Serviços com carregamento único e em memória; favoritos por ID em `@capacitor/preferences`; corrigir `format("truetype")` e testar a cobertura de acentos da fonte.
- Leitor e pesquisa da Constituição.

**Aceitação**: todos os hinos exportados e a Constituição abrem e pesquisam offline; favoritos antigos migrados para os IDs novos.

---

## 7. API do back-end (MVP)

| Método e rota | Função |
|---|---|
| `POST /paginas` | importar foto, página e hinário |
| `GET /paginas` | listar páginas com estado |
| `GET /paginas/{n}` | detalhe (leituras, divergências, blocos) |
| `POST /paginas/{n}/ler` | pôr na fila e ler com Gemini + Tesseract |
| `PUT /paginas/{n}/linhas` | gravar correções (texto, recuo, tipo de bloco) |
| `POST /paginas/{n}/confirmar` | marcar linhas como `revisto` |
| `GET /fila` | estado da fila e das quotas por modelo |
| `GET /comparar/{hinoId}` | leitura nova contra JSON atual |
| `POST /migracao` | correr a migração e gerar o relatório |
| `POST /exportar` | validar e gerar `hinario-*.json` |

---

## 8. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| O modelo "corrige" palavras em Kimbundu/Kikongo | prompt rígido, segundo leitor (Tesseract), diferenças sempre visíveis, revisão obrigatória, portão de qualidade na Fase 3 |
| Lista e limites dos modelos gratuitos mudam | cadeia por configuração; confirmar no AI Studio; estado de quota em disco |
| Quota diária esgotada | rotação de modelos; fila que pausa e retoma; cache |
| Resposta bloqueada por semelhança com texto protegido | passar ao modelo seguinte; recurso a Tesseract e marcação |
| Plano gratuito pode usar o conteúdo para melhorar produtos | só páginas impressas, sem dados pessoais; interruptor para desligar o Gemini; plano pago opcional |
| Mistura de modelos dá resultados inconsistentes | um modelo por página; `modeloUsado` registado |
| Fotografias de má qualidade | captura padronizada, recorte manual, app de scanner |
| Chave exposta | variável de ambiente, `.gitignore`, nunca no painel |
| Esquema novo parte favoritos antigos | migração de favoritos para IDs novos na Fase 9 |

---

## 9. Decisões em aberto

1. `appId` definitivo da IERA App.
2. O coro repete-se depois de cada estrofe em cada hinário? (`coroRepete`)
3. Números fora do intervalo em KIKONGO_ORIGEM e KINBUMDU: número do hino ou página?
4. Quem aprova o texto da Constituição?
5. Quantas páginas tem cada hinário a digitalizar (para estimar a quota diária e os dias de trabalho)?

---

## 10. Para começar hoje

1. Fase 0: CSS do leitor, chave no AI Studio (projeto sem faturação), anotar os modelos gratuitos e limites.
2. Tirar e enviar as 10 a 20 páginas de referência e transcrevê-las com cuidado (texto de ouro).
3. Fase 1: comando de migração e relatório.
4. Fase 3 só depois do portão de qualidade do ponto 2.

---

## Anexo A: exemplo de `ModelPool` (pseudocódigo)

```
lerPagina(pagina):
  para cada modelo em cadeia (por ordem):
    se modelo.pausadoAte > agora: continuar
    para tentativa em 1..tentativasPorModelo:
      r = chamar(modelo, pagina)
      se r.ok e esquemaValido(r): registar(pagina, modelo, ok); devolver r
      se r.429MinutoLimite: dormir(esperaMinuto); continuar
      se r.429Diario:       modelo.pausadoAte = proximoReinicioDiario(); parar este modelo
      se r.bloqueada:       registar(bloqueada); parar este modelo
      se r.503 ou erro:     dormir(crescente(tentativa))
  pagina.estado = (todosPausados ? pausada_quota : erro_leitura)
```

## Anexo B: fotografar as páginas

- Uma página por foto, de cima, luz uniforme, sem dedo, sem a página vizinha.
- Preferir uma app de scanner (corrige perspetiva e recorta).
- Nomear ficheiros pelo número da página (por exemplo `0395.jpg`).
