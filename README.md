# QuizMaker 🦕

Applicazione Spring Boot per creare, pubblicare e somministrare quiz scolastici divertenti, con interfaccia web e API REST.

## Caratteristiche principali

- Accesso **insegnante** con registrazione self-service e dashboard dedicata (`/teacher/...`).
- Gestione **multi-insegnante** con ruoli admin/non-admin, abilitazione account, reset password e cancellazione completa account (solo admin).
- Generazione quiz con **AI OpenAI** (opzionale) e supporto allegati (`.pdf`, `.docx`, testo).
- Condivisione quiz verso più insegnanti.
- Gestione risultati con analytics e sblocco tentativi singolo studente o in blocco.
- Controlli di sicurezza includono:
  - protezione brute-force login teacher e login studente;
  - rate limit registrazione teacher;
  - supporto **Cloudflare Turnstile** (opzionale) in fase di registrazione.
- Backup schedulato database SQLite in produzione con retention configurabile.
- Semplice dispiegamento in cloud o on premise.

## Stack tecnologico

- Java **21**
- Spring Boot **4**
- Spring Security
- Thymeleaf
- JPA/Hibernate + Liquibase
- H2 (dev/docker) e SQLite (prod)

## Avvio rapido in sviluppo (profilo `dev`)

```bash
mvn spring-boot:run
```

Oppure con profilo esplicito:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Link utili in locale:

- App: http://localhost:8080
- Login teacher: http://localhost:8080/teacher/login
- Registrazione teacher: http://localhost:8080/teacher/register
- Console H2 (dev): http://localhost:8080/h2-console
- JDBC URL H2: `jdbc:h2:mem:quizmakerdb`
- User H2: `sa`
- Password H2: *(vuota)*

## Credenziali iniziali

L'app crea un utente admin di default via configurazione:

| Username | Password   |
|----------|------------|
| `admin`  | `changeme` |

⚠️ Cambia subito password in ambienti non di sviluppo.

Variabili d'ambiente:

```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='$2a$12$...'
```

> `ADMIN_PASSWORD` può essere una password in chiaro o un hash bcrypt (anche con prefisso `{bcrypt}`).

## Profili runtime

### `dev`

- DB H2 in-memory
- H2 console attiva
- Turnstile attivo per default con chiavi di test

### `prod`

- DB SQLite (`jdbc:sqlite:./data/quizmaker.db` di default)
- cookie sessione `Secure` e `SameSite=Strict`
- backup DB abilitato di default

Esempio avvio produzione:

```bash
export PROD_SQLITE_DB_URL=jdbc:sqlite:/opt/quizmaker/data/quizmaker.db
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='$2a$12$...'

java -jar target/quizmaker-*.jar --spring.profiles.active=prod
```

### `docker`

Il profilo docker usa configurazione equivalente al dev (H2 in-memory).

```bash
docker compose -f docker/docker-compose.yml up --build
```

## Variabili d'ambiente principali

| Variabile                     | Default                           | Descrizione                        |
|-------------------------------|-----------------------------------|------------------------------------|
| `ADMIN_USERNAME`              | `admin`                           | Username teacher/admin bootstrap   |
| `ADMIN_PASSWORD`              | `changeme`                        | Password teacher/admin bootstrap   |
| `PROD_SQLITE_DB_URL`          | `jdbc:sqlite:./data/quizmaker.db` | Path DB SQLite in produzione       |
| `OPENAI_API_KEY`              | vuota                             | API key OpenAI                     |
| `OPENAI_MODEL`                | `gpt-5.4-mini`                    | Modello per generazione quiz       |
| `OPENAI_MAX_ATTACHMENT_CHARS` | `60000`                           | Max caratteri estratti da allegato |
| `TURNSTILE_ENABLED`           | `false` (`true` in dev)           | Abilita verifica CAPTCHA Turnstile |
| `TURNSTILE_SITE_KEY`          | vuota (o test key in dev)         | Site key Turnstile                 |
| `TURNSTILE_SECRET_KEY`        | vuota (o test key in dev)         | Secret key Turnstile               |
| `TURNSTILE_VERIFY_URL`        | endpoint Cloudflare               | URL verifica Turnstile             |
| `DB_BACKUP_ENABLED`           | `false` (`true` in prod)          | Abilita job backup SQLite          |
| `DB_BACKUP_CRON`              | `0 0 2 * * *`                     | Pianificazione backup              |
| `DB_BACKUP_DIRECTORY`         | `./backups`                       | Directory output backup            |
| `DB_BACKUP_RETENTION_COUNT`   | `30`                              | Numero backup mantenuti            |
| `SESSION_COOKIE_SECURE`       | `true` (prod)                     | Cookie di sessione solo HTTPS      |

## Backup schedulato database (SQLite)

```bash
export DB_BACKUP_ENABLED=true
export DB_BACKUP_CRON="0 0 2 * * *"
export DB_BACKUP_DIRECTORY="./backups"
export DB_BACKUP_RETENTION_COUNT=14
```

## Funzionalità web

| URL                        | Accesso                      | Descrizione                     |
|----------------------------|------------------------------|---------------------------------|
| `/`                        | Pubblico / sessione studente | Login studente + pagina quiz    |
| `/teacher/login`           | Pubblico                     | Login teacher                   |
| `/teacher/register`        | Pubblico                     | Registrazione teacher           |
| `/teacher`                 | Teacher                      | Dashboard quiz                  |
| `/teacher/students`        | Teacher                      | Gestione studenti               |
| `/teacher/results`         | Teacher                      | Risultati e sblocchi quiz       |
| `/teacher/logs`            | Teacher                      | Visualizzazione log applicativi |
| `/teacher/profile`         | Teacher                      | Cambio password personale       |
| `/teacher/quiz/new`        | Teacher                      | Editor nuovo quiz               |
| `/teacher/quiz/{id}/edit`  | Teacher                      | Editor modifica quiz            |
| `/teacher/system`          | Admin                        | Pannello sistema                |
| `/teacher/system/teachers` | Admin                        | Gestione insegnanti             |
| `/teacher/about`           | Admin                        | Info build/runtime              |

## API principali

### Quiz (`/api/quizzes`)

- `GET /api/quizzes` elenco quiz pubblicati per studente autenticato.
- `GET /api/quizzes/{id}` dettaglio quiz pubblicato.
- `POST /api/quizzes/{id}/submit` invio risposte studente.
- `POST /api/quizzes` creazione quiz (teacher).
- `PUT /api/quizzes/{id}` modifica quiz (teacher).
- `DELETE /api/quizzes/{id}` eliminazione quiz (teacher).
- `PUT /api/quizzes/{id}/publication` pubblicazione/depubblicazione.
- `POST /api/quizzes/{id}/share` condivisione quiz a più teacher.
- `POST /api/quizzes/{quizId}/unlock/{studentId}` sblocco tentativo singolo.
- `POST /api/quizzes/{quizId}/unlock-all` sblocco massivo tentativi.
- `POST /api/quizzes/generate` generazione quiz via AI (multipart, opzionale allegato).

### Studenti (`/api/students`)

- `GET /api/students` elenco studenti del teacher corrente.
- `POST /api/students` creazione studente.
- `DELETE /api/students/{id}` eliminazione studente.
- `POST /api/students/{id}/regenerate-password` rigenera parola chiave singolo studente.
- `POST /api/students/regenerate-passwords` rigenera parole chiave in massa.

### Log (`/api/teacher/logs`)

- `GET /api/teacher/logs/tail?lines=200` ultime righe log (max 1000).

## Sicurezza

- CSRF con cookie token (eccetto H2 console).
- Login teacher con blocco temporaneo dopo troppi tentativi falliti.
- Login studente protetto per IP + keyword.
- Registrazione teacher rate-limited e integrata con CAPTCHA Turnstile (se abilitato).
- Ruoli applicativi: `ROLE_TEACHER`, `ROLE_ADMIN`.

## Database e migration

Le migration Liquibase sono in `src/main/resources/db/changelog/`.
Aggiungi ogni modifica schema in un nuovo file XML e includilo in `db.changelog-master.xml`.

## Test

```bash
mvn test
```

## Licenza

Copyright (c) 2026 Saidone

Distributed under the GNU General Public License v3.0
