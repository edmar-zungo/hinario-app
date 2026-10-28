package com.ravunana.hinario.migracao;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Resultado corrido da migração (usado pelo log, por {@code POST /migracao}
 * e pelo relatório).
 *
 * @param ficheirosGerados nomes dos {@code hinario-*.json} escritos
 * @param erros             erros que impediram a escrita de algum ficheiro
 * @param caracteresAntes   caracteres de texto (títulos + estrofes) na origem
 * @param caracteresDepois  os mesmos caracteres depois de exportados
 * @param estrofesOrfas     estrofes cujo hino não existe em {@code hinario.json}
 * @param avisos            avisos de validação em todos os ficheiros gerados
 */
public record ResultadoMigracao(
        Instant geradoEm,
        Path relatorio,
        List<String> ficheirosGerados,
        List<String> erros,
        long caracteresAntes,
        long caracteresDepois,
        int estrofesOrfas,
        int avisos) {

    public boolean textoInalterado() {
        return caracteresAntes == caracteresDepois;
    }
}
