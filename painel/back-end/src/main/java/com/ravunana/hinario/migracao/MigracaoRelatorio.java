package com.ravunana.hinario.migracao;

import java.time.Instant;
import java.util.List;

/**
 * Relatório {@code relatorios/migracao.md} (PRD Fase 1).
 */
public record MigracaoRelatorio(
        Instant geradoEm,
        int hinosLidos,
        int estrofesLidas,
        long caracteresAntes,
        long caracteresDepois,
        int estrofesNaoNfc,
        List<ResumoLingua> resumos,
        List<String> erros,
        List<SecaoRelatorio> secoes) {

    public record ResumoLingua(String codigo, String nome, int hinos, int estrofes, int coros,
                               String ficheiro, int erros, int avisos) {
    }

    public String markdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Relatório de migração\n\n");
        sb.append("Gerado: ").append(geradoEm).append("\n\n");

        sb.append("## Ficheiros gerados\n\n");
        if (resumos.isEmpty()) {
            sb.append("Nenhum ficheiro gerado.\n\n");
        } else {
            sb.append("| Língua | Código | Hinos | Estrofes | Coros | Ficheiro | Validação |\n");
            sb.append("|---|---|---:|---:|---:|---|---|\n");
            for (ResumoLingua r : resumos) {
                sb.append("| ").append(r.nome()).append(" | `").append(r.codigo()).append("` | ")
                        .append(r.hinos()).append(" | ").append(r.estrofes()).append(" | ")
                        .append(r.coros()).append(" | `").append(r.ficheiro()).append("` | ")
                        .append(r.erros() == 0 ? "OK" : "ERRO").append(" (").append(r.avisos()).append(" avisos) |\n");
            }
            sb.append("\n");
        }

        sb.append("## Integridade do texto\n\n");
        sb.append("- Caracteres na origem (títulos + estrofes exportadas): ").append(caracteresAntes).append("\n");
        sb.append("- Caracteres no esquema novo: ").append(caracteresDepois).append("\n");
        long diferenca = caracteresDepois - caracteresAntes;
        sb.append("- Diferença: ").append(diferenca);
        sb.append(diferenca == 0
                ? " — nenhum texto alterado pela migração\n"
                : " — ALTERAÇÃO DE TEXTO (a migração não pode alterar texto)\n");
        sb.append("- Estrofes fora de Unicode NFC na origem: ").append(estrofesNaoNfc).append("\n\n");

        sb.append("## Verificações efectuadas\n\n");
        sb.append("| Verificação | Encontrados |\n|---|---:|\n");
        for (SecaoRelatorio secao : secoes) {
            sb.append("| ").append(secao.titulo()).append(" | ").append(secao.encontrados()).append(" |\n");
        }
        sb.append("\n");

        for (SecaoRelatorio secao : secoes) {
            sb.append("## ").append(secao.titulo());
            if (secao.encontrados() > 0) {
                sb.append(" (").append(secao.encontrados()).append(")");
            }
            sb.append("\n\n");
            if (secao.linhas().isEmpty()) {
                sb.append("Nada a assinalar.\n\n");
            } else {
                for (String linha : secao.linhas()) {
                    sb.append(linha).append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("## Erros de validação\n\n");
        if (erros.isEmpty()) {
            sb.append("Nenhum. Todos os ficheiros gerados são válidos.\n");
        } else {
            for (String erro : erros) {
                sb.append("- ").append(erro).append("\n");
            }
        }

        return sb.toString();
    }
}
