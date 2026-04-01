# QuizMaker 🦕

App Spring Boot per creare e giocare quiz scolastici, con pannello insegnante protetto e pagina alunni con login tramite parola chiave.

## Avvio in sviluppo (H2)

```bash
# Il profilo dev è attivo di default
mvn spring-boot:run

# Oppure esplicitamente:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- App: http://localhost:8080
- Admin: http://localhost:8080/admin/login
- H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:quizmakerdb`
- Username: `sa` / Password: (vuota)

## Credenziali admin di default

| Username | Password   |
|----------|------------|
| `admin`  | `changeme` |

⚠️ **Cambia la password prima di andare in produzione!**

Per generare un nuovo hash bcrypt:
```bash
# Con Spring CLI o online su https://bcrypt-generator.com
# Poi aggiorna app.admin.password in application.yml
```

## Avvio in produzione (SQLite)

```bash
# (Opzionale) personalizza il percorso del file SQLite
export PROD_SQLITE_DB_URL=jdbc:sqlite:/opt/quizmaker/data/quizmaker-prod.db
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD="{bcrypt}\$2a\$10\$..."

# Build e avvio
mvn package -DskipTests
java -jar target/quizmaker-0.0.5.jar --spring.profiles.active=prod
```

### Backup schedulato database (solo SQLite/profilo prod)

Il job usa Spring Scheduler e crea copie di backup nella cartella `./backups` (configurabile).

```bash
# Abilita/disabilita backup (in prod è true di default)
export DB_BACKUP_ENABLED=true

# Cron Spring (default: ogni giorno alle 02:00)
export DB_BACKUP_CRON="0 0 2 * * *"

# Directory backup
export DB_BACKUP_DIRECTORY="./backups"

# Quanti file mantenere
export DB_BACKUP_RETENTION_COUNT=14
```

## API REST

| Metodo | URL                 | Auth | Descrizione         |
|--------|---------------------|------|---------------------|
| GET    | `/api/quizzes`      | No   | Lista tutti i quiz  |
| GET    | `/api/quizzes/{id}` | No   | Dettaglio qu        |
| POST   | `/api/quizzes`      | Sì   | Crea quiz           |
| PUT    | `/api/quizzes/{id}` | Sì   | Aggiorna quiz       |
| DELETE | `/api/quizzes/{id}` | Sì   | Elimina quiz        |

## Pagine Web

| URL                     | Accesso     | Descrizione      |
|-------------------------|-------------|------------------|
| `/`                     | Pubblico    | Pagina alunni    |
| `/admin/login`          | Pubblico    | Login insegnante |
| `/admin`                | Autenticato | Dashboard admin  |

## Liquibase

Le migration sono in `src/main/resources/db/changelog/`.
Ogni nuovo changeset va aggiunto in un file separato e incluso nel `db.changelog-master.xml`.

```xml
<!-- Esempio nuovo changeset -->
<changeSet id="003" author="quizmaker">
    <comment>Aggiungi colonna...</comment>
    ...
</changeSet>
```

## Test

```bash
mvn test
```


## Login studenti

Gli studenti sono salvati nella tabella `students` e accedono da `/` con una parola chiave di 4 caratteri (`login_keyword`).
Dopo la consegna, il quiz viene bloccato per quello studente; la maestra può sbloccarlo dalla dashboard admin nella tabella risultati.
La dashboard admin include anche la gestione studenti (creazione nome + generazione automatica codice univoco da 4 caratteri).
