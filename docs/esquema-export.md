# Esquema de export — `hinario-<id>.json`

Definido pelo PRD (secção 3.3) e concretizado na Fase 1.
Ficheiros Java: `painel/back-end/src/main/java/com/ravunana/hinario/exportacao/`.

> Onde o PRD e este documento divergirem, **o PRD prevalece**.

## Ficheiros

| Língua (`Linguas`) | Código / `id` | Nome | Ficheiro |
|---|---|---|---|
| `PORTUGUES` | `por` | Hinário Português | `hinario-por.json` |
| `KIKONGO_REVISADO` | `kkr` | Hinário Kikongo (revisado) | `hinario-kkr.json` |
| `KIKONGO_ORIGEM` | `kko` | Hinário Kikongo (origem) | `hinario-kko.json` |
| `KINBUMDU` | `kmb` | Hinário Kimbundu | `hinario-kmb.json` |

Localização: `<hinario.dados.dir>/export/` (por omissão `painel/back-end/data/export/`).
Um ficheiro por língua, para que a numeração de hinos seja independente entre línguas.

## Estrutura

```json
{
  "id": "kmb",
  "lingua": "kmb",
  "nome": "Hinário Kimbundu",
  "geradoEm": "2026-10-28T08:23:09.247000071Z",
  "hinos": [
    {
      "id": "078248bc-39ae-4a3e-945b-6a2c42b4ed0f",
      "numero": 42,
      "pagina": 394,
      "titulo": "NGANA JEZU",
      "autor": "IERA",
      "blocos": [
        {
          "tipo": "estrofe",
          "numero": 1,
          "linhas": [
            { "texto": "Ngana Jezu, nga mu zola,", "recuo": 0 },
            { "texto": "Muxima diami dise.", "recuo": 0 }
          ]
        },
        {
          "tipo": "coro",
          "linhas": [
            { "texto": "Nga mu zola, Jesus!", "recuo": 0 }
          ]
        }
      ]
    }
  ]
}
```

### Raiz

| Campo | Tipo | Regras |
|---|---|---|
| `id` | string | código da língua, minúsculas `[a-z0-9]{2,10}` |
| `lingua` | string | igual a `id` |
| `nome` | string | nome legível do hinário |
| `geradoEm` | string (ISO-8601 UTC) | data/hora da geração |
| `hinos` | array | nunca vazio; ordenado por `pagina`, depois `numero` |

### Hino

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID | **mantém o UUID do `hinario.json` atual** — favoritos existentes continuam a servir |
| `numero` | inteiro | número do hino na sua língua |
| `pagina` | inteiro | página em que o hino começa no impresso |
| `titulo` | string | nunca vazio; texto exatamente como impresso |
| `autor` | string ou null | verbatim do JSON atual |
| `blocos` | array | nunca vazio; ordem impressa (estrofes e coros alternando) |

### Bloco

| Campo | Tipo | Regras |
|---|---|---|
| `tipo` | `"estrofe"` \| `"coro"` | não há outros valores |
| `numero` | inteiro ou null | obrigatório (≥ 1) em `estrofe`; **sempre null** em `coro` |
| `linhas` | array | nunca vazio |

### Linha

| Campo | Tipo | Regras |
|---|---|---|
| `texto` | string | Unicode **NFC**; nunca é alterado (sem trim, sem correções); pode ser `""` quando a linha impressa está em branco — isso é um aviso para a revisão, nunca é removido |
| `recuo` | 0 \| 1 \| 2 | 0 = margem normal, 1 = recuada, 2 = mais recuada; a migração dos dados antigos grava sempre `0` (o JSON antigo não tinha esta informação) |

## Regras de transformação (Fase 1)

1. **Nenhum texto é alterado.** A `descricao` da estrofe é cortada em `linhas` apenas
   nas quebras de linha impressas (`\n`); `String.join("\n", linhas)` devolve o texto
   original byte a byte. A migração compara a contagem de caracteres antes e depois e
   falha se diferir.
2. Estrofes na **ordem original do ficheiro** `estrofes.json` (não por número).
3. `refrao: true` ⇒ bloco `coro` (sem número). `refrao: false` ⇒ bloco `estrofe`
   com o número original.
4. Estrofes cujo hino não existe em `hinario.json` (órfãs) **não são exportadas**;
   ficam listadas no relatório.
5. A escrita é **atómica** (ficheiro temporário + `move`).

## Validação

`ExportacaoValidador` devolve `erros` (bloqueiam a exportação) e `avisos`
(mostrados, nunca alteram texto):

- **Erros**: estrutura Bean Validation (hino sem blocos, bloco sem linhas,
  estrofe sem número, coro com número, recuo fora de 0..2, texto fora de NFC),
  `id`/`lingua` mal formados, hinário sem hinos.
- **Avisos**: número de estrofe ≤ 0 ou repetido, buracos na sequência 1..N,
  número de hino repetido, hino fora de ordem de página, linhas vazias,
  espaços a mais, caracteres fora do alfabeto esperado (letras latinas com
  diacríticos, dígitos, pontuação comum, `’`, `-`).

## Relatório

`<hinario.dados.dir>/relatorios/migracao.md` — gerado pela mesma migração
(`POST /migracao`, ou no arranque com `hinario.migracao.auto=true`), com:
órfãs, hinos sem estrofes, numeração (duplicados, buracos, fora do intervalo),
sequência de estrofes, estrofes sem quebra de linha > 80 caracteres, espaços a
mais, cópias de hino divergentes e a integridade de texto (caracteres antes/depois).

## Compatibilidade

- O JSON antigo (`hinario.json` + `estrofes.json`) continua a ser a fonte dos
  serviços existentes até à Fase 7.
- A app mobile lê hoje o JSON antigo; a mudança para este esquema é a Fase 9
  (leitor com `recuo` e coro destacado). O `id` do hino não muda, por isso os
  favoritos guardados não se perdem.
