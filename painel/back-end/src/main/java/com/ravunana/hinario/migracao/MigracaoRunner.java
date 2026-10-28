package com.ravunana.hinario.migracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Corre a migração no arranque quando {@code hinario.migracao.auto=true}.
 * O mesmo resultado está disponível em {@code POST /migracao}.
 */
@Component
@ConditionalOnProperty(name = "hinario.migracao.auto", havingValue = "true")
public class MigracaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoRunner.class);

    private final MigracaoService migracao;

    public MigracaoRunner(MigracaoService migracao) {
        this.migracao = migracao;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ResultadoMigracao resultado = migracao.migrar();
            if (!resultado.erros().isEmpty()) {
                throw new IllegalStateException("Migração terminou com erros: " + resultado.erros());
            }
            log.info("Migração concluída: {} ficheiros gerados, relatório em {}",
                    resultado.ficheirosGerados().size(), resultado.relatorio());
        } catch (IOException e) {
            throw new IllegalStateException("Migração falhou: " + e.getMessage(), e);
        }
    }
}
