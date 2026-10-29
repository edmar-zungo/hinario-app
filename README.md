# IERA App — Hinário (repositório único)

Ferramenta de extração de hinários por IA (Gemini) + Tesseract, painel de revisão e app mobile.
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
| Tesseract OCR | 5.x + `por.traineddata` | Fase 4 (comparação) — **necessário** para comparar |
| Chave Gemini | variável `GEMINI_API_KEY` | só a partir da Fase 3 (extração) |

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

- A chave da API do Gemini vive **só** na variável de ambiente `GEMINI_API_KEY`
  (nunca no código, nunca no repositório, nunca no painel Angular).
- `.gitignore` cobre `.env*`, chaves avulsas e a pasta `iera-dados/`.

## Estado das fases do MVP

| Fase | Conteúdo | Estado |
|---|---|---|
| 0 | Preparação (CSS do leitor, README, gitignore, appId, Tesseract, chave) | concluída (ver tarefas em baixo) |
| 1 | Base do back-end e migração dos dados atuais | concluída |
| 2 | Importação de páginas | concluída |
| 3 | Leitura com Gemini (`ModelPool`, fila, quota) | concluída |
| 4 | Tesseract e comparação | concluída |
| 5 | Estruturação e continuação | concluída |
| 6 | Painel: importação e revisão | por fazer |
| 7 | Exportação e comparação com o JSON atual | por fazer |
| 8 | Constituição | por fazer |
| 9 | IERA App (redesign do mobile) | por fazer |

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

### Leitura com Gemini (Fase 3)

```bash
# enfileirar uma página para leitura (202 assim que entra na fila):
curl -X POST http://localhost:8080/paginas/395/ler

# ver a fila, os modelos e as quotas:
curl http://localhost:8080/fila
```

O que a leitura faz (PRD 5.x):

- **fila sequencial** por número de página; ao arrancar volta a enfileirar o
  que ficou em `em_fila` de uma sessão anterior;
- envia a foto processada com a instrução 5.3 (`src/main/resources/leitura/`)
  e valida a resposta contra o esquema 5.4 antes de aceitar — a IA **transcreve,
  nunca edita**;
- **rotação de modelos** (5.8): cadeia por ordem de preferência, cada um com
  `tentativas-por-modelo`; 429 de minuto/dia pausa o modelo e passa ao seguinte;
- **pausa sozinha**: sem cota nenhum, a página fica `pausada_quota` e a fila
  tenta de novo passado `hinario.fila.retoma-ms` (a quota do dia revista em
  `quota/estado.json`, fuso `gemini.fuso-quota`);
- **cache** por `sha256 da imagem + versão do prompt + modelo` (5.8): reler
  não gasta cota nem chama a API;
- estados: `em_fila → lida`; erros → `erro_leitura` (400/5xx em todos os
  modelos) ou `bloqueada_pela_ia`; releitura só de estados anteriores à
  revisão (`409` para `comparada` em diante).

Config (`application.properties`): `gemini.*` — a chave **só** de
`GEMINI_API_KEY` (vazia por omissão); com a chave vazia, `POST .../ler` dá
`409` dizendo o que falta. Os testes não usam a rede.

### Tesseract e comparação (Fase 4)

O mesmo `POST /paginas/{n}/ler` passa a correr os **dois leitores**: depois do
Gemini, o Tesseract lê a imagem processada em local e o back-end **compara**
as duas leituras (PRD 5.7.3):

- alinhamento das linhas por distância de edição e classificação de cada par:
  `igual`, `so_acento`, `so_pontuacao`, `divergente` — **qualquer diferença**
  fica em `divergencias` da página, com o texto dos dois lados;
- a página passa a `comparada`; se o Gemini falhar mas o Tesseract ler, a
  página fica com a leitura local (`bloqueada_pela_ia` "segue só com
  Tesseract", PRD 5.6);
- o alfabeto é verificado (5.7.2): caracteres fora do conjunto conhecido
  ficam marcados em `leituras.gemini.validacao.alfabeto` — **nunca removidos**;
- `gemini.enabled=false` ⇒ modo só-Tesseract: a fila continua a funcionar e as
  páginas ficam `lida` com `leituras.tesseract`;
- se o binário não estiver instalado, a leitura principal não falha: a página
  fica `lida` com `leituras.tesseract.erro`.

Config (`application.properties`): `tesseract.enabled`, `tesseract.idioma`
(`por`), `tesseract.binario`, `tesseract.datapath` (vazio = `TESSDATA_PREFIX`
do sistema), `tesseract.psm`, `tesseract.timeout-segundos`.

**Decisão registada** (28/10/2026): o Tesseract corre por `ProcessBuilder`
sobre o binário do sistema, em vez de Tess4J — mesma escolha da Fase 2
(Java2D em vez de JavaCV): zero dependências nativas no Maven; o motor é
requisito da máquina de qualquer forma. `GET /fila` passou a incluir
`tesseractDisponivel`.

### Estruturação e continuação (Fase 5)

Depois de a página ficar `comparada`, o back-end monta a **proposta de
estrutura** (5.7.4–5.7.6) e grava-a em `blocos` do `paginas/<n>.json`,
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

### Tarefas da Fase 0 que dependem de ti

1. **Chave no AI Studio** (agora **necessária** para ler de verdade): criar
   projeto **sem faturação**, gerar a chave, exportar `GEMINI_API_KEY` e anotar,
   na página de limites do AI Studio, os modelos gratuitos e os seus limites
   (PRD 5.8); confirmar também o portão de qualidade 5.10. Guardar a chave fora
   do repositório.
2. **Instalar o Tesseract** (necessário desde a Fase 4 para a comparação;
   sem ele, as páginas ficam só com a leitura do Gemini e
   `leituras.tesseract.erro`):

```bash
sudo apt-get install -y tesseract-ocr tesseract-ocr-por
tesseract --version
```

3. **`appId` da IERA App** (decisão registada): `com.ravunana.iera`.
   Aplica-se ao projeto Android na Fase 9 (renomear a app), para não partir
   o build atual.
