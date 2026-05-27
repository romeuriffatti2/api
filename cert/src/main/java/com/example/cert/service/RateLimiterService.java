package com.example.cert.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter simples por IP usando sliding window em memória.
 * Limite: 5 requisições por IP a cada 10 minutos.
 * Adequado para endpoints públicos de baixo volume onde a JVM
 * roda em instância única.
 */
@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MILLIS = 10 * 60 * 1000L; // 10 minutos

    /** Armazena [IP → {timestamp da 1ª req na janela, contador}] */
    private final Map<String, long[]> store = new ConcurrentHashMap<>();

    /**
     * Verifica se o IP está dentro do limite.
     *
     * @param ip endereço IP do requisitante
     * @return true se a requisição é permitida; false se o limite foi atingido
     */
    public synchronized boolean isAllowed(String ip) {
        long now = Instant.now().toEpochMilli();
        long[] state = store.get(ip);

        if (state == null || (now - state[0]) >= WINDOW_MILLIS) {
            // Primeira requisição ou janela expirada: inicia nova janela
            store.put(ip, new long[]{now, 1L});
            return true;
        }

        if (state[1] < MAX_REQUESTS) {
            state[1]++;
            return true;
        }

        // Limite atingido dentro da janela
        return false;
    }
}
