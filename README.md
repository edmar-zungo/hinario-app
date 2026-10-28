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
| Tesseract OCR | 5.x + `por.traineddata` | só a partir da Fase 4 (comparação) |
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
| 3 | Leitura com Gemini (`ModelPool`, fila, quota) | por fazer |
| 4 | Tesseract e comparação | por fazer |
| 5 | Estruturação e continuação | por fazer |
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

e falha se alterar. Testes: `cd painel/back-end && ./mvnw test`.

### Tarefas da Fase 0 que dependem de ti

1. **Chave no AI Studio**: criar projeto **sem faturação**, gerar a chave e anotar,
   na página de limites do AI Studio, os modelos gratuitos e os seus limites
   (PRD 5.8). Guardar a chave fora do repositório.
2. **Instalar o Tesseract** (Fase 4, mas já fica feito):

```bash
sudo apt-get install -y tesseract-ocr tesseract-ocr-por
tesseract --version
```

3. **`appId` da IERA App** (decisão registada): `com.ravunana.iera`.
   Aplica-se ao projeto Android na Fase 9 (renomear a app), para não partir
   o build atual.
