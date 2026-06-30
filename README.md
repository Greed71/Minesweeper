# Minesweeper

![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![Vite](https://img.shields.io/badge/Vite-6-646CFF?logo=vite)
![Java](https://img.shields.io/badge/Java-21-437291?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot)

Monorepo con **UI React (Vite)** e **API Spring Boot (Java)**: gioco classico, sessioni partita in memoria, utenti e classifiche su **PostgreSQL** (es. Supabase), autenticazione **JWT** lato applicazione. **Interfaccia multilingua** (IT, FR, ES, DE) via `i18next`.

**Se atterri oggi nel repo**, in ordine logico:

1. Questa pagina (stack, struttura, env, avvio locale)
2. `client/src/` per il flusso utente (Home, Registrazione, Account)
3. `server/src/main/java/com/minesweeper/` per controller, filtri di sicurezza, sessioni di gioco (organizzato per feature: `auth/`, `game/`, `score/`, `config/`, `security/`, `exception/`)

Non esiste (ancora) una cartella `docs/` separata: l’intento del `README` è allineare **perimetro prodotto** (cosa fa l’app) e **perimetro tecnico** (dove intervenire in codice).

---

## A cosa serve questa documentazione

L’obiettivo è rispondere in fretta a:

- **Dove** vivono le feature (client vs server, quale pacchetto / cartella)
- **Qual è il flusso** (auth → partita via API stateless → salvataggio punteggio con token)
- **Quali variabili d’ambiente** servono per far girare frontend e backend insieme

L’esperienza completa richiede **Vite e Spring** attivi insieme, con `VITE_APP_BACKEND_URL` e CORS (es. `FRONT`) allineati. Dalla **radice del monorepo** puoi usare un solo comando (dopo `npm install` in radice, vedi sotto «Avvio unificato»).

---

## Avvio unificato (opzionale)

Dalla **radice** del repository:

```bash
npm install
npm run dev
```

Avvia in parallelo `server` (Spring Boot) e `client` (Vite) tramite `concurrently` (`devDependencies` a livello monorepo). Se invece lanci `npm run dev` **solo** da `client/`, parte **unicamente** Vite: le API su `http://localhost:8080` non esistono finché non avvii Spring (dalla root con il comando qui sopra, oppure `mvnw spring-boot:run` in `server/` in un secondo terminale). In alternativa, due terminali: `mvnw` in `server` e `npm run dev` in `client` come in precedenza.

- **Unit test** sulla logica `Minesweeper` (seed fissi, bordi, vittoria, sconfitta) in `server/src/test/java/com/minesweeper/game/engine/MinesweeperTest.java`.
- **Test di sicurezza automatizzati** (21 test totali): timing attack login, revoca refresh token, race condition sessioni, anti-enumerazione utenti, validazione input — `AuthServiceSecurityTest`, `AuthControllerSecurityTest`, `InMemoryGameSessionRepositorySecurityTest`.
- **Client**: `client/src/api/client.js` (interceptor su `401` con auto-refresh token) e `hooks/useMinesweeper.js` (logica di gioco estratta in custom hook), `devLog.js` (messaggi in console **solo in sviluppo**).
- **i18n / multilingua**: `client/src/i18n/` con 4 lingue (IT, FR, ES, DE), selettore lingua nell'header, persistenza in `localStorage`. Libreria `i18next` + `react-i18next`.
- **MinesweeperApplicationTest**: verifica l'avvio del contesto Spring.
- **Rate limiting**: `POST /auth/*`, `/api/genera`, `/api/reveal`, `/api/clic` via `AuthRateLimitFilter`; `GET /api/ping` via `PingRateLimitFilter` (finestra dedicata per keep-alive, 120 req/min).
- **OpenAPI (Springdoc)**: documentazione JSON su `/v3/api-docs`, UI su **http://localhost:8080/swagger-ui.html**.
- **OpenAPI (Springdoc)**: documentazione JSON su `/v3/api-docs`, UI su **http://localhost:8080/swagger-ui.html** (redirect verso l’indice; path abilitato in `SecurityConfig`).

---

## Stack (alto livello)

| Layer | Tecnologia |
|--------|------------|
| Web UI | React 19 + Vite 6 + React Router 7 |
| Chiamate HTTP | Axios (base URL da `VITE_APP_BACKEND_URL`) |
| API | Spring Boot 3.4, REST (`/api`, `/auth`, `/score`) |
| Persistenza | PostgreSQL via Spring Data JPA (prod / Supabase) |
| Auth | JWT (HMAC, access token 15min) + Refresh Token (256bit, 7gg, rotazione) + BCrypt (password) |
| i18n | `i18next` + `react-i18next` — 4 lingue: IT, FR, ES, DE |
| H2 in memoria | Solo test backend (`src/test/resources`) |
| OpenAPI | Springdoc (`/v3/api-docs`, Swagger UI) |
| Contenitore | `server/Dockerfile`; opzione deploy in `server/render.yaml` |

### Tooling sviluppo (client)

- **ESLint** 9 (config in `client/eslint.config.js`) per il linting del frontend

---

## Getting started (sviluppo)

**Prerequisiti:** Node.js + npm, JDK 21, Maven (o uso di `server/mvnw`), istanza PostgreSQL o stringa di connessione (es. Supabase).

### Backend (`server/`)

Imposta almeno le variabili per il DB, JWT, CORS e (in produzione) niente segreti in chiaro: vedi sotto *Variabili d’ambiente*.

```bash
cd server
# Windows: .\mvnw.cmd spring-boot:run
./mvnw spring-boot:run
```

Porta predefinita: **8080** (override con `PORT`).

Test:

```bash
cd server
./mvnw test
```

### Frontend (`client/`)

Crea (o adatta) un file `client/.env` con `VITE_APP_BACKEND_URL` uguale all’URL del backend, es.:

```env
VITE_APP_BACKEND_URL=http://localhost:8080
```

Il frontend salva in `localStorage` tre chiavi: `token` (access JWT), `refreshToken` (per rinnovo automatico), `loggedUser` (profilo). L'interceptor in `client.js` gestisce il refresh automatico su `401`.

```bash
cd client
npm install
npm run dev
```

Porta predefinita Vite: **http://localhost:5173** (CORS: variabile `FRONT` o default `app.cors.allowed-origins` lato server).

Build produzione client:

```bash
cd client
npm run build
```

Build JAR server:

```bash
cd server
./mvnw -DskipTests package
```

---

## Architettura: come gira in locale

In sviluppo l’esperienza completa richiede **due processi**:

1. **Spring Boot** – REST, JWT, JPA, sessioni partita in memoria per `sessionId`, rate limit su login/registrazione/refresh/gioco, refresh token con rotazione
2. **Vite** – SPA, routing (`/`, `/register`, `/account`), token in `localStorage` (`token`, `refreshToken`, `loggedUser`)

Non c’è un processo unico: il README serve proprio a far combaciare **origine** (frontend) e **allowed-origins** (backend).

---

## Project structure (mappa per orientarsi)

```
Minesweeper/
  README.md
  package.json         # opz.: npm run dev a radice (client + server)
  .gitignore

  client/
    package.json
    vite.config.js
    .env                 # (locale) VITE_APP_BACKEND_URL, non in git
    src/
      main.jsx
      Router.jsx
      config.js
      i18n/
        i18n.js            # init i18next, detection lingua, persistenza localStorage
        locales/
          it.json, fr.json, es.json, de.json
      api/client.js      # Axios condiviso, interceptor 401 + auto-refresh
      hooks/
        useMinesweeper.js # custom hook: logica gioco (timer, bandiere, API)
      components/
        Board.jsx          # griglia presentational
        Cell.jsx           # cella presentational
      pages/
        Home.jsx           # partita, classifica, salvataggio punteggi
        Register.jsx       # login / registrazione, token
        Account.jsx        # profilo, password, logout
      devLog.js            # log solo in sviluppo (Vite: import.meta.env.DEV)
      App.css, index.css
    public/

  server/
    pom.xml
    Dockerfile
    render.yaml
    mvnw, mvnw.cmd
    src/
      main/
        java/com/minesweeper/
          MinesweeperApplication.java
          auth/
            controller/AuthController.java    # /auth/*
            service/AuthService.java          # logica auth + refresh token
            filter/AuthRateLimitFilter.java   # rate limit IP
            filter/JwtAuthenticationFilter.java # estrazione JWT
            dto/LoginRequest, RegisterRequest, PasswordChangeRequest, RefreshTokenRequest, AuthResponseBody
            model/User.java, RefreshToken.java
            repository/UserRepo.java, RefreshTokenRepo.java
          game/
            controller/MinesweeperController.java # /api/*
            service/GameSessionService.java
            engine/Minesweeper.java               # logica pura griglia
            repository/GameSessionRepository.java # interfaccia
            repository/InMemoryGameSessionRepository.java
            model/GameSessionEntry.java
            dto/GameConfig, ClickPosition, GameStateResponse
          score/
            controller/ScoreController.java   # /score/*
            service/ScoreService.java
            model/Score.java
            repository/ScoreRepo.java
            dto/ScoreRequest.java
          config/
            SecurityConfig.java   # CORS, filtri, header sicurezza
            OpenApiConfig.java
          security/
            JwtService.java
            IpRequestThrottle.java
            PingRateLimitFilter.java         # rate limit dedicato GET /api/ping
            JsonAccessDeniedHandler.java
            JsonAuthenticationEntryPoint.java
          exception/
            ApiExceptionHandler.java
            GameSessionNotFoundException.java
        resources/
          application.properties
      test/
        java/com/minesweeper/
          MinesweeperApplicationTest.java
          game/engine/MinesweeperTest.java
          auth/service/AuthServiceSecurityTest.java
          auth/controller/AuthControllerSecurityTest.java
          game/repository/InMemoryGameSessionRepositorySecurityTest.java
        resources/
          application.properties              # H2, JWT di test, CORS
```

---

## Flussi principali (entrypoint “da seguire”)

### Autenticazione (JWT + Refresh Token)

- **Login / Registrazione** → `AuthController` (`/auth/login`, `/auth/register`)
  - risposta: `{ token, refreshToken, user }` (password mai esposta nel JSON: `@JsonIgnore` su `User.getPassword()`)
- **Refresh** → `POST /auth/refresh` con `{ refreshToken }` — consuma il vecchio refresh token e ne emette uno nuovo (rotazione). Access token: 15 minuti. Refresh token: 7 giorni.
- **Logout** → `POST /auth/logout` (autenticato) — revoca tutti i refresh token dell'utente nel DB.
- **Filtro JWT** → `JwtAuthenticationFilter` popola `SecurityContext` per le route protette
- **Regole** → `SecurityConfig`: ad es. `GET /auth/user`, `PUT /auth/change-password`, `GET|POST` sui punteggi sensibili richiedono `Authorization: Bearer …`

### Partita (nessuna autenticazione obbligatoria)

- **Nuova partita** → `POST /api/genera` con `sessionId` (UUID lato client)
- **Mossa** → `POST /api/reveal` (stato in `GameSessionService` → `InMemoryGameSessionRepository`)

Nota: le sessioni di gioco sono **in memoria** sul singolo nodo con limite massimo configurabile (`app.game.max-sessions`): riavvio server = partite perse; non adatto a più repliche senza store condiviso (Redis, DB). La griglia ha validazione `mines < rows × cols` e un flood-fill iterativo per evitare `StackOverflow` su griglie grandi.

### Classifiche e punteggi

- **Leaderboard** → `GET /score/leaderboard?difficulty=…` (pubblico)
- **Salvataggio punteggio** → `POST /score/save` con **body solo `points` e `difficulty`**, utente desunto dal **JWT** (niente `username` nel body, anti-spoofing)
- **Punteggi utente** → `GET /score/user` (autenticato, allineato al token)

---

## Variabili d’ambiente (panoramica)

I segreti **non** vanno committati. L’esempio in `application.properties` è solo per sviluppo; in produzione usa env reali.

### Backend (indicative)

| Variabile | Ruolo |
|-----------|--------|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Connessione PostgreSQL in produzione; se **non le imposti**, in locale il backend parte con H2 in memoria (vedi `application.properties`) |
| `JWT_SECRET` (→ `app.jwt.secret`, min. 32 caratteri) | Firma HMAC del JWT; se lasciato al default, il server logga un WARNING all'avvio |
| `JWT_TTL_SECONDS` (→ `app.jwt.ttl-seconds`, default 900 = 15min) | Durata access token |
| `JWT_REFRESH_TTL_SECONDS` (→ `app.jwt.refresh-ttl-seconds`, default 604800 = 7gg) | Durata refresh token |
| `JWT_ISSUER` / `JWT_AUDIENCE` (opzionali) | `iss` / `aud` sui token |
| `FRONT` (o mapping verso `app.cors.allowed-origins`) | Origine/i consentiti CORS (es. `http://localhost:5173` o URL produzione) |
| `PORT` | Porta server (default 8080) |
| `app.security.auth-rate-max-per-minute` (opz.) | Soglia rate limit login/reg/refresh/gioco (default 30 richieste/min) |
| `app.security.ping-rate-max-per-minute` (opz.) | Soglia rate limit GET /api/ping keep-alive (default 120 richieste/min) |
| `app.security.trust-x-forwarded-for` (opz., default false) | Se `true`, usa header `X-Forwarded-For` per IP reale (solo dietro proxy fidato) |
| `app.game.session-ttl-seconds` (opz., default 3600) | Dopo quanto togliere in RAM una partita inattiva |
| `app.game.max-sessions` (opz., default 10000) | Massimo partite attive contemporaneamente |
| `app.game.session-cleanup-initial-delay-ms` (opz.) | Ritardo (ms) prima del primo giro di pulizia sessioni |
| `app.game.session-cleanup-interval-ms` (opz.) | Ogni quanto (ms) ripetere la pulizia |

### Lingue supportate (frontend)

Il selettore lingua nell'header sceglie tra IT, FR, ES, DE. La preferenza viene salvata in `localStorage` (`minesweeper-lang`) e rispettata ai successivi accessi.

### Frontend

| Variabile | Ruolo |
|-----------|--------|
| `VITE_APP_BACKEND_URL` | Base URL assoluta dell’API (es. `http://localhost:8080`) |

---

## Docker

Il backend può essere costruito e avviato come JAR in container: vedi `server/Dockerfile` (build Maven multi-stage, `java -jar`).

---

## Idee per evolvere la documentazione

- Aggiungere `docs/` con una **visione prodotto** (regole del gioco, difficoltà) separata da note tecniche
- Arricchire annotazioni/ gruppi in Springdoc o esportare lo schema per il client, se servono contratti ufficiali
- Se si introduce una **cache o Redis** per le sessioni, aggiornare la sezione *Partita* e l’architettura in locale
