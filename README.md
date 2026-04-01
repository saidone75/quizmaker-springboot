# QuizMaker 🦕

App Spring Boot per creare, pubblicare e somministrare quiz scolastici.
Include:
- area **studenti** con login tramite codice personale;
- area **admin** protetta con Spring Security;
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

Servizi utili:
- App: http://localhost:8080
- Login admin: http://localhost:8080/admin/login
- Console H2: http://localhost:8080/h2-console
- JDBC URL H2: `jdbc:h2:mem:quizmakerdb`
- Username H2: `sa`
- Password H2: *(vuota)*

## Credenziali admin di default

| Username | Password |
|----------|----------|
| `alice`  | `changeme` |

⚠️ **Cambia la password prima di andare in produzione.**

Le credenziali sono configurabili via variabili d'ambiente:

```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='{bcrypt}$2a$12$...'
```

## Avvio in produzione (profilo `prod`, SQLite)

```bash
# (Opzionale) personalizza il percorso del DB SQLite
export PROD_SQLITE_DB_URL=jdbc:sqlite:/opt/quizmaker/data/quizmaker-prod.db

# Credenziali admin
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='{bcrypt}$2a$12$...'

# Build e avvio
mvn package -DskipTests
java -jar target/quizmaker-0.0.10.jar --spring.profiles.active=prod
```

## Variabili d'ambiente principali

| Variabile | Default | Descrizione |
|---|---|---|
| `ADMIN_USERNAME` | `alice` | Username admin Spring Security |
| `ADMIN_PASSWORD` | hash bcrypt predefinito | Password admin (hash bcrypt) |
| `PROD_SQLITE_DB_URL` | `jdbc:sqlite:./data/quizmaker-prod.db` | URL DB SQLite in produzione |
| `OPENAI_API_KEY` | vuota | Chiave API OpenAI |
| `OPENAI_MODEL` | `gpt-5.4-mini` | Modello usato per generazione quiz |
| `OPENAI_MAX_ATTACHMENT_CHARS` | `60000` | Limite caratteri testo estratto da allegato |
| `DB_BACKUP_ENABLED` | `false` (`true` in prod) | Abilita backup DB schedulato |
| `DB_BACKUP_CRON` | `0 0 2 * * *` | Cron backup |
| `DB_BACKUP_DIRECTORY` | `./backups` | Directory backup |
| `DB_BACKUP_RETENTION_COUNT` | `14` | Numero backup mantenuti |

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

## Flusso studenti

1. Lo studente effettua login da `/` inserendo una parola chiave di **4 caratteri**.
2. Vede solo i quiz **pubblicati**.
3. Invia le risposte con endpoint dedicato.
4. Dopo la consegna, il quiz viene bloccato per quello studente.
5. L'admin può sbloccare singolo studente o tutti gli studenti per un quiz.

## API REST

### Quiz (`/api/quizzes`)

> Nota sicurezza:
> - `GET /api/quizzes` e `GET /api/quizzes/{id}` sono pubblici e ritornano solo quiz pubblicati.
> - `POST /api/quizzes/{id}/submit` è pubblico ma richiede sessione studente valida.
> - Endpoint di gestione quiz/sblocco richiedono ruolo `ADMIN`.

| Metodo | URL | Auth | Descrizione |
|---|---|---|---|
| GET | `/api/quizzes` | No | Lista quiz pubblicati |
| GET | `/api/quizzes/{id}` | No | Dettaglio quiz pubblicato |
| POST | `/api/quizzes/{id}/submit` | Sessione studente | Invia risposte quiz |
| POST | `/api/quizzes/{quizId}/unlock/{studentId}` | Admin | Sblocca quiz per uno studente |
| POST | `/api/quizzes/{quizId}/unlock-all` | Admin | Sblocca quiz per tutti gli studenti |
| POST | `/api/quizzes` | Admin | Crea nuovo quiz |
| PUT | `/api/quizzes/{id}` | Admin | Aggiorna quiz |
| PUT | `/api/quizzes/{id}/publication` | Admin | Pubblica/nasconde quiz |
| DELETE | `/api/quizzes/{id}` | Admin | Elimina quiz |
| POST | `/api/quizzes/generate` (`multipart/form-data`) | Admin | Genera bozza quiz con AI |

#### Payload principali

**Crea/Aggiorna quiz** (`POST/PUT /api/quizzes...`):
```json
{
  "title": "Verifica sui vulcani",
  "emoji": "🌋",
  "questions": [
    {
      "text": "Qual è il vulcano attivo più alto d'Europa?",
      "options": ["Etna", "Stromboli", "Vesuvio"],
      "correctAnswerIndex": 0
    }
  ]
}
```

**Invio risposte** (`POST /api/quizzes/{id}/submit`):
```json
{
  "answers": [0, 2, 1, 1]
}
```

**Pubblicazione quiz** (`PUT /api/quizzes/{id}/publication`):
```json
{
  "published": true
}
```

**Generazione AI** (`POST /api/quizzes/generate`, multipart):
- campi form: `topic`, `numberOfQuestions`, `difficulty`, `tone`
- allegato opzionale: `file` (es. PDF o DOCX, da cui viene estratto testo)

### Studenti (`/api/students`)

> Tutti gli endpoint studenti richiedono ruolo `ADMIN`.

| Metodo | URL | Auth | Descrizione |
|---|---|---|---|
| GET | `/api/students` | Admin | Lista studenti con keyword |
| POST | `/api/students` | Admin | Crea studente |
| DELETE | `/api/students/{id}` | Admin | Elimina studente |
| POST | `/api/students/{id}/regenerate-password` | Admin | Rigenera keyword singolo studente |
| POST | `/api/students/regenerate-passwords` | Admin | Rigenera keyword per tutti |

## Pagine Web

| URL | Accesso | Descrizione |
|---|---|---|
| `/` | Pubblico / sessione studente | Login studente + pagina quiz |
| `/admin/login` | Pubblico | Login insegnante |
| `/admin` | Admin | Dashboard quiz |
| `/admin/students` | Admin | Gestione studenti |
| `/admin/results` | Admin | Risultati quiz e sblocchi |
| `/admin/quiz/new` | Admin | Editor nuovo quiz |
| `/admin/quiz/{id}/edit` | Admin | Editor modifica quiz |
| `/about` | Admin | Info build/runtime |

## Liquibase

Le migration sono in `src/main/resources/db/changelog/` e vengono caricate dal `db.changelog-master.xml`.

Regola pratica: aggiungi ogni modifica schema in un nuovo file XML e includilo nel master changelog.

## Test

```bash
mvn test
```
