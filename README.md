# Minesweeper

![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![Vite](https://img.shields.io/badge/Vite-6-646CFF?logo=vite)
![Java](https://img.shields.io/badge/Java-21-437291?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot)

Monorepo con **UI React (Vite)** e **API Spring Boot (Java)**: gioco classico, sessioni partita in memoria, utenti e classifiche su **PostgreSQL** (es. Supabase), autenticazione **JWT** lato applicazione.

**Se atterri oggi nel repo**, in ordine logico:

1. Questa pagina (stack, struttura, env, avvio locale)
2. `client/src/` per il flusso utente (Home, Registrazione, Account)
3. `server/src/main/java/com/` per controller, filtri di sicurezza, sessioni di gioco

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

Avvia in parallelo `server` (Spring Boot) e `client` (Vite) tramite `concurrently` (`devDependencies` a livello monorepo). In alternativa, due terminali: `mvnw` in `server` e `npm run dev` in `client` come in precedenza.

---

## Test e qualità (alto impatto)

- **Unit test** sulla logica `Minesweeper` (seed fissi, bordi, vittoria, sconfitta) in `server/src/test/java/com/MinesweeperTest.java`.
- **Client**: `client/src/api/client.js` (interceptor su `401` autenticato) e `devLog.js` (messaggi in console **solo in sviluppo**, niente log rumorosi in produzione).
- **MinesweeperApplicationTest**: verifica l’avvio del contesto Spring oltre ai test su `MinesweeperTest`.
- **Sessioni partita**: ogni partita in memoria ha un TTL (`app.game.session-ttl-seconds`, default 1h) e un job schedulato che rimuove le sessioni inattive (parametri `app.game.session-cleanup-*.` in `application.properties`).
- **OpenAPI (Springdoc)**: documentazione JSON su `/v3/api-docs`, UI su **http://localhost:8080/swagger-ui.html** (redirect verso l’indice; path abilitato in `SecurityConfig`).

---

## Stack (alto livello)

| Layer | Tecnologia |
|--------|------------|
| Web UI | React 19 + Vite 6 + React Router 7 |
| Chiamate HTTP | Axios (base URL da `VITE_APP_BACKEND_URL`) |
| API | Spring Boot 3.4, REST (`/api`, `/auth`, `/score`) |
| Persistenza | PostgreSQL via Spring Data JPA (prod / Supabase) |
| Auth | JWT (HMAC) + BCrypt (password) |
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

1. **Spring Boot** – REST, JWT, JPA, sessioni partita in memoria per `sessionId`, rate limit su login/registrazione
2. **Vite** – SPA, routing (`/`, `/register`, `/account`), token in `localStorage` (`token`, `loggedUser`)

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
      api/client.js      # Axios condiviso, gestione 401
      devLog.js          # log solo in sviluppo (Vite: import.meta.env.DEV)
      pages/
        Home.jsx         # partita, classifica, salvataggio punteggi
        Register.jsx     # login / registrazione, token
        Account.jsx      # profilo, password, logout
      App.css, index.css
    public/

  server/
    pom.xml
    Dockerfile
    render.yaml
    mvnw, mvnw.cmd
    src/
      main/
        java/com/
          MinesweeperApplication.java
          Minesweeper.java           # logica griglia, primo click safe
          MinesweeperController.java # /api/genera, /api/reveal, ping
          GameSessionService.java
          AuthController.java
          ScoreController.java
          SecurityConfig.java
          JwtService.java
          JwtAuthenticationFilter.java
          AuthRateLimitFilter.java
          OpenApiConfig.java
          …
        resources/
          application.properties
      test/
        java/com/
          MinesweeperTest.java       # test unit su logica di gioco
        resources/
          application.properties     # H2, JWT di test, CORS
```

---

## Flussi principali (entrypoint “da seguire”)

### Autenticazione (JWT)

- **Login / Registrazione** → `AuthController` (`/auth/login`, `/auth/register`)
  - risposta: `{ token, user }` (password mai esposta nel JSON: serializzazione controllata su `User`)
- **Filtro JWT** → `JwtAuthenticationFilter` popola `SecurityContext` per le route protette
- **Regole** → `SecurityConfig`: ad es. `GET /auth/user`, `PUT /auth/change-password`, `GET|POST` sui punteggi sensibili richiedono `Authorization: Bearer …`

### Partita (nessuna autenticazione obbligatoria)

- **Nuova partita** → `POST /api/genera` con `sessionId` (UUID lato client)
- **Mossa** → `POST /api/reveal` (stato in `GameSessionService`, `ConcurrentHashMap` per `sessionId`)

Nota: le sessioni di gioco sono **in memoria** sul singolo nodo: riavvio server = partite perse; non adatto a più repliche senza store condiviso.

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
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Connessione PostgreSQL |
| `JWT_SECRET` (→ `app.jwt.secret`, min. 32 caratteri) | Firma HMAC del JWT |
| `JWT_ISSUER` / `JWT_AUDIENCE` (opzionali) | `iss` / `aud` sui token |
| `FRONT` (o mapping verso `app.cors.allowed-origins`) | Origine/i consentiti CORS (es. `http://localhost:5173` o URL produzione) |
| `PORT` | Porta server (default 8080) |
| `app.security.auth-rate-max-per-minute` (opz.) | Soglia rate limit login/reg (default sensato) |
| `app.game.session-ttl-seconds` (opz., default 3600) | Dopo quanto togliere in RAM una partita inattiva |
| `app.game.session-cleanup-initial-delay-ms` (opz.) | Ritardo (ms) prima del primo giro di pulizia sessioni |
| `app.game.session-cleanup-interval-ms` (opz.) | Ogni quanto (ms) ripetere la pulizia |

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
