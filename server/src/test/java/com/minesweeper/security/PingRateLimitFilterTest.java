package com.minesweeper.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test del rate-limit + bypass su GET /api/ping.
 * Mira a verificare:
 *  - rate limit classico: N max request per finestra, poi 429
 *  - bypass tramite header X-Ping-Token: salta sempre il rate limit
 *  - bypass tramite query ?token= (fallback): salta sempre il rate limit
 *  - bypass disabilitato se property vuota
 *  - token vuoto / non corrispondente: NON bypassa
 *  - il bypass NON incrementa il bucket (call successiva senza token puo' ancora passare)
 */
class PingRateLimitFilterTest {

    private static final int MAX = 3;
    private static final int WINDOW = 60;

    private PingRateLimitFilter filterWithToken;
    private PingRateLimitFilter filterWithoutToken;

    @BeforeEach
    void setUp() {
        filterWithToken = new PingRateLimitFilter(MAX, WINDOW, false, "secret-xyz-123");
        filterWithoutToken = new PingRateLimitFilter(MAX, WINDOW, false, "");
    }

    private MockHttpServletRequest pingRequest(String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/ping");
        req.setRemoteAddr(remoteAddr);
        return req;
    }

    private int sendN(PingRateLimitFilter filter, MockHttpServletRequest req, int n) throws Exception {
        int allowed = 0;
        for (int i = 0; i < n; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            // ricreo la request perché la catena può consumarla — ma qui non c'è body read
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(req, res, chain);
            if (res.getStatus() == 200) allowed++;
        }
        return allowed;
    }

    @Test
    void rateLimitBlocksAfterMaxRequestsWithoutToken() throws Exception {
        MockHttpServletRequest req = pingRequest("10.0.0.1");
        int allowed = sendN(filterWithToken, req, MAX + 5);
        assertEquals(MAX, allowed, "Devono passare solo le prime MAX richieste");
        // L'ultima risposta deve essere 429
        MockHttpServletResponse last = new MockHttpServletResponse();
        filterWithToken.doFilter(req, last, new MockFilterChain());
        assertEquals(429, last.getStatus());
        assertTrue(last.getContentAsString().contains("rate_limited"));
    }

    @Test
    void validXHeaderBypassesRateLimit() throws Exception {
        MockHttpServletRequest req = pingRequest("10.0.0.2");
        req.addHeader("X-Ping-Token", "secret-xyz-123");
        int allowed = sendN(filterWithToken, req, MAX * 5);
        assertEquals(MAX * 5, allowed,
                "Con header valido TUTTE le richieste devono passare, anche sopra il limite");
    }

    @Test
    void validQueryParamFallbackBypassesRateLimit() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/ping");
        req.setRemoteAddr("10.0.0.3");
        req.setParameter("token", "secret-xyz-123");
        int allowed = sendN(filterWithToken, req, MAX * 5);
        assertEquals(MAX * 5, allowed,
                "Con ?token= valido TUTTE le richieste devono passare");
    }

    @Test
    void invalidHeaderDoesNotBypass() throws Exception {
        MockHttpServletRequest req = pingRequest("10.0.0.4");
        req.addHeader("X-Ping-Token", "wrong-value");
        int allowed = sendN(filterWithToken, req, MAX + 5);
        assertEquals(MAX, allowed, "Header con valore errato NON deve bypassare");
    }

    @Test
    void wrongQueryParamDoesNotBypass() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/ping");
        req.setRemoteAddr("10.0.0.5");
        req.setParameter("token", "wrong-value");
        int allowed = sendN(filterWithToken, req, MAX + 5);
        assertEquals(MAX, allowed, "?token= errato NON deve bypassare");
    }

    @Test
    void blankTokenDoesNotBypass() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/ping");
        req.setRemoteAddr("10.0.0.6");
        req.setParameter("token", ""); // vuoto esplicito
        int allowed = sendN(filterWithToken, req, MAX + 5);
        assertEquals(MAX, allowed, "Token vuoto NON deve bypassare");
    }

    @Test
    void blankHeaderDoesNotBypass() throws Exception {
        MockHttpServletRequest req = pingRequest("10.0.0.7");
        req.addHeader("X-Ping-Token", "   "); // solo spazi
        int allowed = sendN(filterWithToken, req, MAX + 5);
        assertEquals(MAX, allowed, "Header blank NON deve bypassare");
    }

    @Test
    void bypassDisabledWhenPropertyBlank() throws Exception {
        MockHttpServletRequest req = pingRequest("10.0.0.8");
        req.addHeader("X-Ping-Token", "secret-xyz-123"); // valore corretto, ma filter SENZA segreto
        int allowed = sendN(filterWithoutToken, req, MAX + 5);
        assertEquals(MAX, allowed,
                "Se property vuota, anche l'header valido NON bypassa (fail-safe)");
    }

    @Test
    void bypassDoesNotConsumeBudget() throws Exception {
        // Prima saturiamo il bucket con chiamate SENZA token
        MockHttpServletRequest noTok = pingRequest("10.0.0.9");
        sendN(filterWithToken, noTok, MAX);

        // Poi una richiesta con token valido: deve passare (200) e NON incrementare
        MockHttpServletRequest tokReq = pingRequest("10.0.0.9");
        tokReq.addHeader("X-Ping-Token", "secret-xyz-123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filterWithToken.doFilter(tokReq, res, new MockFilterChain());
        assertEquals(200, res.getStatus());

        // Le successive senza token devono ancora essere 429 (bucket pieno).
        // Il bypass NON deve "resettare" il conteggio per IP.
        MockHttpServletResponse after = new MockHttpServletResponse();
        filterWithToken.doFilter(noTok, after, new MockFilterChain());
        assertEquals(429, after.getStatus());
    }

    @Test
    void doesNotFilterNonPingRequests() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/genera");
        req.setRemoteAddr("10.0.0.10");

        int allowed = sendN(filterWithToken, req, MAX * 10);
        assertEquals(MAX * 10, allowed,
                "Il filtro non deve toccare endpoint diversi da GET /api/ping");
    }
}
