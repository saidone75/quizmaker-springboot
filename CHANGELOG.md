# Changelog

Tutte le modifiche rilevanti del progetto saranno documentate in questo file.

## [1.0.0] - 2026-05-07

### 🎉 Prima release
- Pubblicazione iniziale di **Alice's Simple Quiz Maker**.
- Applicazione web completa per creazione, pubblicazione e somministrazione quiz didattici.

### ✨ Funzionalità principali incluse
- Area insegnante con autenticazione, dashboard e gestione profilo.
- Gestione studenti, risultati e tentativi quiz.
- Editor quiz con creazione domande e opzioni.
- Area studente per accesso e compilazione quiz.
- API REST per gestione quiz, studenti e sottomissioni.

### 🤖 AI e contenuti multimediali
- Generazione quiz opzionale tramite OpenAI.
- Import/estrazione testo da allegati per generazione assistita.
- Ricerca immagini Wikimedia (modalità simple/advanced).
- Supporto upload immagini per domande.

### 🔒 Sicurezza e operatività
- Protezioni login (rate limiting e brute-force protection).
- Supporto CAPTCHA Turnstile configurabile.
- Migrazioni database con Liquibase.
- Profili runtime `dev`, `docker`, `prod`.
- Backup schedulato database SQLite in produzione.

### 🧱 Stack tecnico
- Java 21, Spring Boot 4, Spring Security, Thymeleaf.
- JPA/Hibernate, Liquibase, H2 (dev/docker), SQLite (prod).
- Jetty embedded.
