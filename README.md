# QuizMaker 🦕

Applicazione per creare, pubblicare e somministrare quiz scolastici.
Include:
- area **studenti** con login tramite codice personale;
- area **admin** protetta da password;
- API REST per quiz, invio risultati e gestione studenti;
- generazione quiz con AI (opzionale) anche da allegati.

## Requisiti

- Java **21**
- Maven **>= 3.6.3**

## Avvio in sviluppo (profilo `dev`, H2 in-memory)

```bash
# Il profilo dev è attivo di default
mvn spring-boot:run

# Oppure esplicitamente:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Link utili:
- Applicazione: http://localhost:8080
- Login admin: http://localhost:8080/admin/login
- Console H2: http://localhost:8080/h2-console
- JDBC URL H2: `jdbc:h2:mem:quizmakerdb`
- Username H2: `sa`
- Password H2: *(vuota)*

## Credenziali admin di default

| Username | Password   |
|----------|------------|
| `admin`  | `changeme` |

⚠️ **Cambia la password prima di andare in produzione.**

Le credenziali sono configurabili via variabili d'ambiente:

```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='{bcrypt}$2a$12$...'
```

## Avvio in produzione (profilo `prod`, SQLite)

```bash
# (Opzionale) personalizza il percorso del DB SQLite
export PROD_SQLITE_DB_URL=jdbc:sqlite:/opt/quizmaker/data/quizmaker.db

# Credenziali admin
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='{bcrypt}$2a$12$...'

# Avvio
java -jar target/quizmaker-0.0.10.jar --spring.profiles.active=prod
```

## Variabili d'ambiente principali

| Variabile                     | Default                           | Descrizione                                |
|-------------------------------|-----------------------------------|--------------------------------------------|
| `ADMIN_USERNAME`              | `admin`                           | Username admin                             |
| `ADMIN_PASSWORD`              | hash bcrypt predefinito           | Password admin (hash bcrypt)               |
| `PROD_SQLITE_DB_URL`          | `jdbc:sqlite:./data/quizmaker.db` | URL DB SQLite in produzione                |
| `OPENAI_API_KEY`              | vuota                             | Chiave API OpenAI                          |
| `OPENAI_MODEL`                | `gpt-5.4-mini`                    | Modello usato per generazione quiz         |
| `OPENAI_MAX_ATTACHMENT_CHARS` | `60000`                           | Limite caratteri testo estratto da allegato|
| `DB_BACKUP_ENABLED`           | `false` (`true` in prod)          | Abilita backup DB schedulato               |
| `DB_BACKUP_CRON`              | `0 0 2 * * *`                     | Cron backup                                |
| `DB_BACKUP_DIRECTORY`         | `./backups`                       | Directory backup                           |
| `DB_BACKUP_RETENTION_COUNT`   | `30`                              | Numero backup mantenuti                    |

## Backup schedulato database (SQLite/profilo `prod`)

```bash
# Abilita/disabilita backup
export DB_BACKUP_ENABLED=true

# Ogni giorno alle 02:00
export DB_BACKUP_CRON="0 0 2 * * *"

# Directory backup
export DB_BACKUP_DIRECTORY="./backups"

# Quanti file mantenere
export DB_BACKUP_RETENTION_COUNT=14
```

## Pagine Web

| URL                     | Accesso                      | Descrizione                  |
|-------------------------|------------------------------|------------------------------|
| `/`                     | Pubblico / sessione studente | Login studente + pagina quiz |
| `/admin/login`          | Pubblico                     | Login insegnante             |
| `/admin`                | Admin                        | Dashboard quiz               |
| `/admin/students`       | Admin                        | Gestione studenti            |
| `/admin/results`        | Admin                        | Risultati quiz e sblocchi    |
| `/admin/quiz/new`       | Admin                        | Editor nuovo quiz            |
| `/admin/quiz/{id}/edit` | Admin                        | Editor modifica quiz         |
| `/about`                | Admin                        | Info build/runtime           |

## Liquibase

Le migration sono in `src/main/resources/db/changelog/` e vengono caricate dal `db.changelog-master.xml`.

Regola pratica: aggiungi ogni modifica schema in un nuovo file XML e includilo nel master changelog.

## Test

```bash
mvn test
```
