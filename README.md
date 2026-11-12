# IERA App — Hinário (repositório único)

Ferramenta de extração de hinários por IA (Groq), comparação com o JSON actual, painel de revisão e app mobile.
Especificação: [`docs/IERA-App_PRD-MVP.md`](docs/IERA-App_PRD-MVP.md) (prevalece sobre o resto).

## Estrutura

```
back-end/   → painel/back-end    Spring Boot (Java 17) — API e migração
painel/     → painel/front-end   Angular — importação, revisão, exportação
mobile/     → mobile             Ionic/Angular + Capacitor — app do leitor
docs/       → documentação (PRD, esquema de export)
```

## Requisitos

| Componente | Versão | Nota |
|---|---|---|
| Java | 17+ | back-end |
| Maven | 3.8+ (ou `./mvnw`) | back-end |
| Node.js | 20+ | painel e mobile |
| Chave Groq | variável `GROQ_API_KEY` | leitura OCR |

## Como correr

### 1. Back-end (porta 8080)

```bash
# primeira vez: copiar a seed dos dados (a pasta data/ não vai para o git)
mkdir -p painel/back-end/data
cp mobile/src/assets/data/*.json painel/back-end/data/

cd painel/back-end
./mvnw spring-boot:run
```

Testes: `./mvnw test`

### 2. Painel (porta 4200)

```bash
cd painel/front-end
npm install
npm start
```

Lint: `npm run lint`

### 3. App mobile

```bash
cd mobile
npm install
npm start          # dev server (browser)
npm run build      # build de produção (www/)
npx cap sync android   # depois de um build, para o projeto Android
```

## Dados locais

- Ficheiros de dados do back-end: `painel/back-end/data/` (ignorado pelo git).
  - `hinario.json`, `estrofes.json` — dados atuais (fonte da migração)
  - `export/hinario-<id>.json` — esquema novo (gerado pela migração)
  - `relatorios/migracao.md` — relatório da migração
- Localização configurável em `painel/back-end/src/main/resources/application.properties`:

```properties
hinario.dados.dir=            # vazio = pasta data/ do back-end
# hinario.dados.dir=${user.home}/iera-dados   # alternativa do PRD (secção 2)
```

## Segredos

- A chave da API Groq vive **só** na variável de ambiente `GROQ_API_KEY`
  (nunca no código, nunca no repositório, nunca no painel Angular).
- `.gitignore` cobre `.env*`, chaves avulsas e a pasta `iera-dados/`.

## Estado das fases do MVP

| Fase | Conteúdo | Estado |
|---|---|---|
| 0 | Preparação (CSS do leitor, README, gitignore, appId, chave) | concluída (ver tarefas em baixo) |
| 1 | Base do back-end e migração dos dados atuais | concluída |
| 2 | Importação de páginas | concluída |
| 3 | Leitura com Groq (`ModelPool`, fila, quota) | concluída |
| 4 | Comparação da leitura revista com o JSON actual | concluída |
| 5 | Estruturação e continuação | concluída |
| 6 | Painel: importação e revisão | concluída |
| 7 | Exportação e comparação com o JSON atual | concluída |
| 8 | Constituição | concluída |
| 9 | IERA App (redesign do mobile) | concluída |

### Migração (Fase 1)

```bash
# uma vez, com o back-end a correr:
curl -X POST http://localhost:8080/migracao

# ou no arranque:
./mvnw spring-boot:run -Dspring-boot.run.arguments=--hinario.migracao.auto=true
```

Saídas (em `painel/back-end/data/`):

- `export/hinario-{por,kkr,kko,kmb}.json` — esquema novo (`docs/esquema-export.md`)
- `relatorios/migracao.md` — órfãs, numeração, duplicados, espaços, cópias divergentes

A migração **nunca altera texto** (compara a contagem de caracteres antes/depois)

### Importação de páginas (Fase 2)

```bash
# uma foto (número lido do nome: 0395.jpg -> página 395):
curl -X POST http://localhost:8080/paginas \
  -F "ficheiros=@foto.jpg" -F "pagina=395" -F "hinarioId=kmb" -F "rotacao=0"

# lote (ordenado sempre pelo número de página):
curl -X POST http://localhost:8080/paginas \
  -F "ficheiros=@0395.jpg" -F "ficheiros=@0396.jpg" -F "hinarioId=kkr"

# listar / ver uma página:
curl http://localhost:8080/paginas
curl http://localhost:8080/paginas/395
```

Também há um ecrã no painel: **Páginas** (`http://localhost:4200/paginas`) —
escolher fotos, hinário e rotação, e ver a lista com estado e número.

O que a importação faz:

- guarda o **original intacto** (byte a byte) em `imagens/original/<n>.<ext>`;
- gera a versão **processada** em `imagens/processada/<n>.jpg` (roda, reduz o
  lado maior para 2000 px, melhora o contraste);
- grava o ficheiro de trabalho `paginas/<n>.json` com `estado: "importada"` e o
  resumo `sha256` do original.

O pré-processamento usa **Java2D** (JDK puro), sem dependências nativas — decisão
registada a 28/10/2026 em vez de JavaCV (mesma função, portátil).

Regras úteis:

- **reimportar** a mesma página substitui tudo e repõe o estado em `importada`;
- um lote **ou entra todo ou não entra**: se algum ficheiro for inválido, o
  pedido dá `400` e **nada** é gravado;
- números repetidos no lote, língua desconhecida ou imagem ilegível → `400`;
- página inexistente em `GET /paginas/{n}` → `404`.

Testes: `cd painel/back-end && ./mvnw test`.

### Leitura OCR com Groq

```bash
# enfileirar uma página para leitura (202 assim que entra na fila):
curl -X POST http://localhost:8080/paginas/395/ler

# ver a fila, os modelos e as quotas:
curl http://localhost:8080/fila
```

O que a leitura faz (PRD 5.x):

- **fila sequencial** por número de página; ao arrancar volta a enfileirar o
  que ficou em `em_fila` de uma sessão anterior;
- envia a foto processada à API compatível com OpenAI (`/chat/completions`) e
  valida a resposta JSON antes de aceitar — a IA **transcreve,
  nunca edita**;
- repete chamadas temporariamente recusadas e respeita respostas 429;
- **pausa sozinha**: sem cota nenhum, a página fica `pausada_quota` e a fila
  tenta de novo passado `hinario.fila.retoma-ms` (a quota do dia revista em
  `quota/estado.json`);
- **cache** por `sha256 da imagem + versão do prompt + modelo` (5.8): reler
  não gasta cota nem chama a API;
- após a resposta da API, a página é estruturada automaticamente para revisão;
- erros → `erro_leitura` ou `bloqueada_pela_ia`; releitura só de estados
  anteriores à revisão (`409` para páginas confirmadas).

Configuração (`application.properties`), substituível por variáveis de
ambiente: `GROQ_API_KEY`, `GROQ_BASE_URL`, `GROQ_OCR_MODEL`,
`GROQ_OCR_MAX_IMAGES`, `GROQ_OCR_MAX_PDF_PAGES`,
`GROQ_OCR_MAX_COMPLETION_TOKENS`, `GROQ_OCR_MAX_PROMPT_WORDS` e
`GROQ_OCR_MAX_IMAGE_BYTES`. A chave não é incluída no repositório; sem ela,
`POST .../ler` responde `409`. O motor envia uma imagem processada por página
(até ao máximo configurado). A interface atual só importa imagens; o limite de
páginas PDF fica configurável, mas não existe importação de PDF nesta versão.
Para executar localmente, defina `GROQ_API_KEY` no ambiente do processo do
back-end. Os testes não usam a rede.

### Comparação com o hinário actual em arquivo

O OCR usa exclusivamente a resposta da API Groq; não há execução local de OCR.
Depois de rever e confirmar as páginas, a
página **Comparar** (`/comparar`) alinha a transcrição nova com o conteúdo
actual de `hinario.json` e `estrofes.json`, mostrando as diferenças sem
alterar nenhum dos lados. A leitura da IA fica guardada em
`leituras.groq.resposta`.

### Estruturação e continuação (Fase 5)

Depois da leitura da IA, o back-end monta a **proposta de estrutura** e
grava-a em `blocos` do `paginas/<n>.json`,
passando a página a `estruturada` — automático, sem novo pedido:

```bash
# releitura/estruturação explícita (só de comparada/estruturada):
curl -X POST http://localhost:8080/paginas/395/estruturar
```

O que a estruturação faz (PRD 5.7):

- blocos de arranque `titulo_hino`, `estrofe`, `coro`, `bloco_sem_numero`,
  sempre com as **linhas verbatim** da leitura — o texto nunca é alterado;
- **coro proposto** (5.7.6): `bloco_sem_numero` entre estrofes numeradas com
  itálico ou recuo vira `coro` com `possivelCoro`; sem isso fica por
  classificar, **nunca juntado por suposição**;
- **ligação entre páginas** (5.7.4): se a página seguinte continuar, o bloco
  abre com o hino/estrofe da anterior e `ligacao.confirmar` só fica `true`
  quando a sequência não fecha (terminou em N−1, abriu em N+2, ou não há
  contexto) — nesse caso **fica marcado para revisão**, sem juntar;
- **estrofes incompletas**: quando a página seguinte continua a mesma estrofe,
  a da anterior é marcada `incompleta` (a revisão humana decide a divisão);
- múltiplos hinos numa página e hinos que começam no fim de uma página são
  suportados (`hinoAberto`, `continuaDoAnterior`).

Aceitação testada (`EstruturaServiceTest`): o par 394/395 gera o hino 42
completo e o hino 43 ligado às duas páginas (395 a abrir na estrofe 2, sem
confirmação); buracos de sequência ficam `confirmar: true` em vez de juntos.

### Painel: importação e revisão (Fase 6)

A secção **OCR** do painel (`http://localhost:4200/paginas`) mostra a fila e
as quotas (`GET /fila` — aviso quando a quota esgotada, pausas por modelo) e
acções por página: **Ler** (importada/erro), **Rever** (leitura disponível).

O ecrã de revisão (`/paginas/revisar/{n}`) tem três painéis:

- **lista de páginas** com estado (Ctrl+←/→ ou botões navegam entre páginas);
- **foto com zoom** (a versão processada enviada à API; caixa
  "Original" mostra o ficheiro intacto);
- **editor de linhas** com a transcrição Groq: linhas com `[?]` e `duvidas` em
  evidência, recuo visível e tipo de bloco editável (estrofe/coro/bloco sem
  número). Há **paleta de carateres** (á é í ó ú â ê ô ã õ ç ò ń ’) que
  insere no cursor e **modo de texto inteiro** (o número de linhas nunca
  muda: uma linha impressa = uma linha).
- **Atalhos**: `Ctrl+Enter` confirmar · `Ctrl+M` marcar coro · `Ctrl+R`
  recuo (ciclo 0→1→2) · `Ctrl+←/→` página anterior/seguinte.

Endpoints novos (PRD secção 7):

- `PUT /paginas/{n}/linhas` — grava os blocos corrigidos (tipos válidos,
  recuo 0..2, texto normalizado para NFC) e passa a página a `em_revisao`;
- `POST /paginas/{n}/confirmar` — marca todas as linhas `revisto: true` e a
  página a `confirmada`;
- `GET /paginas/{n}/imagem[?original=true]` — a foto para o painel (a
  original continua intacta em disco).

Regra de ouro aplicada e testada (`RevisaoServiceTest`): corrigir **nunca**
toca em `leituras` — o texto original da Groq fica preservado no
ficheiro da página.

### Exportação e comparação (Fase 7)

`POST /exportar` (com `hinarioId` opcional; sem parâmetro, todos os que têm
páginas `confirmada`) monta o hinário a partir da estrutura revisada e grava
`data/export/hinario-<id>.json` no esquema novo (`docs/esquema-export.md`):

- entram **só** as linhas com `revisto: true`;
- estrofe que continua na página seguinte é **juntada** (uma linha impressa =
  uma linha; a divisão de página não é de poesia) — o par 394/395 fecha o hino
  42 completo;
- o `id` (UUID) e o `autor` vêm do `hinario.json` actual, para os favoritos
  continuarem a servir; hino novo recebe UUID novo e autor `IERA`;
- `bloco_sem_numero` por classificar, estrofe marcada incompleta no fim,
  espaços a mais, buracos de sequência e números repetidos **bloqueiam** a
  exportação (o ficheiro não é gravado até o humano corrigir na revisão);
- NFC validado.

`GET /comparar/{hinoId}` compara a leitura nova (páginas confirmadas) com o
JSON actual (`hinario.json` + `estrofes.json`), reutilizando o alinhamento e o
classificador da Fase 4. As diferenças saem classificadas — `divergente`,
`so_acento`, `so_pontuacao`, `so_na_leitura`, `so_no_novo` — para corrigir
erros da migração e preencher hinos incompletos. Só compara, nunca altera
texto.

No painel (`http://localhost:4200`):

- `/exportar` — escolhe o hinário, exporta e mostra, por hinário, os erros que
  bloquearam e os avisos;
- `/comparar` — escolhe um hino e vê, linha a linha, o actual contra a leitura
  nova, com as diferenças coloridas.

Testes (`ExportacaoServiceTest`, `ComparacaoHinoServiceTest`): 394/395 exporta
o hino 42 completo com o UUID do JSON actual; só linhas revistas entram;
`bloco_sem_numero`/incompleta/espaços a mais bloqueiam; sem páginas
confirmadas diz o que falta; sem código exporta todos; a comparação marca
`so_acento` como não-divergente, alinha linha a linha com referência
(bloco/linha) e devolve 404 para hino inexistente.

### Constituição (Fase 8)

A Constituição da IERA está integrada na app a partir da transcrição oficial
(`docs/constituicao_iera_v2.json`, schema 2.0, 69 páginas do PDF emendado de
15-10-2023).

- **Geração**: `node docs/gerar-constituicao.mjs` transforma a transcrição em
  `mobile/src/assets/data/constituicao.json` — texto verbatim (regra de ouro,
  só normalização NFC e estrutura), notas editoriais do Word excluídas e
  **IDs compostos** (`I-art-1`, `II-art-7`, `III-art-3`) porque a numeração de
  artigos repete-se entre as três partes. O script valida a contagem (129
  artigos) e a unicidade dos IDs e falha se a fonte divergir.
- **Conteúdo**: 3 partes (Estatutos, Regulamento da Ordem Interna, Declaração
  da Fé), 31 capítulos, 11 secções, 129 artigos, prémbulo, introdução e
  apêndice das Emendas (1979, 2006, 2013, 2023).
- **App**: `ConstituicaoService` (carregamento único em memória, cache RxJS,
  pesquisa sem acentos com excerto de contexto); tab **Constituição** com
  metadados, índice navegável (partes → capítulos → secções → artigos) e
  pesquisa; leitor `/constituicao-read/:unidadeId` com blocos legais
  (n.º, alíneas, §§, departamentos/sectores), navegação anterior/seguinte e
  os mesmos temas/tamanho de letra do leitor de hinos.
- **Pendência humana (PRD 9.4)**: a revisão e aprovação do texto por um
  responsável da igreja mantém-se como último passo antes de publicar a app;
  a transcrição de origem já veio verificada automaticamente.

Para regenerar após qualquer emenda futura: editar/actualizar a transcrição em
`docs/`, correr o script e revalidar os 129 artigos.

### IERA App — Redesign do Mobile (Fase 9)

A aplicação móvel foi completamente redesenhada e alinhada com o esquema novo de exportação (`hinario-*.json`):

- **Identidade e Configuração**: Renomeada para **IERA App**, `appId: com.ravunana.iera` no `capacitor.config.ts`, `build.gradle` e `strings.xml`.
- **Esquema Export e Dados**: Modelos TypeScript em `hino-export.model.ts` alinhados com o esquema novo; `hinario-por.json`, `hinario-kko.json`, `hinario-kkr.json`, `hinario-kmb.json` em `mobile/src/assets/data/`.
- **Navegação por Separadores (Tabs)**:
  - 📖 **Hinos**: Lista filtrável por hinário com segmentação por língua e modal "Ir ao nº" directo;
  - 🔍 **Pesquisa**: Busca global e por língua, sem distinção de acentos (`removerAcentos()`), com atalho numérico;
  - ⭐ **Favoritos**: Gestão reativa de favoritos com contagem e remoção direta;
  - 📜 **Constituição**: Índice navegável das 3 partes (129 artigos) e pesquisa sem acentos, tudo offline.
- **Leitor de Poesia Modernizado (`/hino-read/:id`)**:
  - Renderização fiel de blocos com recuo (`recuo-0`, `recuo-1`, `recuo-2`) e destaque de coros (`tipo === 'coro'`);
  - Navegação fluida para hino anterior e seguinte dentro da mesma coletânea;
  - Temas de leitura: **Claro**, **Sépia** e **Escuro**, persistidos em `@capacitor/preferences`;
  - Controlo de tamanho de fonte (`A-` / `A+`, 14px a 28px) persistido entre sessões;
  - Toggle de favoritos em tempo real diretamente na barra de leitura.
- **Armazenamento e Performance**:
  - Favoritos armazenados por `id` em `@capacitor/preferences` com chave `'favoritos_ids'`;
  - Migração transparente e automática dos favoritos antigos do `localStorage`;
  - Carregamento único dos hinários em memória com cache RxJS (`shareReplay(1)`) no `HinarioService`;
  - Correção dos formatos de fontes locais `@font-face` para `format("truetype")`.

### Tarefas da Fase 0 que dependem de ti

1. **Chave Groq** (necessária para ler de verdade): gerar a chave no painel
   Groq e exportar `GROQ_API_KEY` no ambiente do back-end. Guardar a chave fora
   do repositório.
2. 2. **`appId` da IERA App** (decisão registada): `com.ravunana.iera`.
   Aplica-se ao projeto Android na Fase 9 (renomear a app), para não partir
   o build atual.
