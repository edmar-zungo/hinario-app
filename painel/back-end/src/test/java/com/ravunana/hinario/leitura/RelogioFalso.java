package com.ravunana.hinario.leitura;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Relógio controlável pelos testes (pausas de quota, reinício diário). */
final class RelogioFalso extends Clock {

    private volatile Instant agora = Instant.parse("2026-10-28T10:00:00Z");

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }

    void avancarSegundos(long segundos) {
        agora = agora.plusSeconds(segundos);
    }
}
