# Changelog

Tutte le modifiche rilevanti a questo progetto saranno documentate in questo file.

Il formato è basato su [Keep a Changelog](https://keepachangelog.com/it-IT/1.1.0/),
e questo progetto aderisce al [Semantic Versioning](https://semver.org/lang/it/).

## [Non rilasciato]

## [1.0.0] - 2026-05-07

### Aggiunto
- Prima release pubblica di **Alice's Simple Quiz Maker**.
- Area insegnante con autenticazione, dashboard e gestione del profilo.
- Editor quiz con gestione di domande e opzioni di risposta.
- Area studente per accesso e compilazione dei quiz.
- Gestione studenti, tracciamento risultati e revisione tentativi quiz.
- API REST per quiz, studenti e sottomissioni.
- Generazione quiz assistita dall'AI tramite integrazione OpenAI (opzionale).
- Estrazione testo da documenti per supportare la generazione assistita dei quiz.
- Supporto alla ricerca immagini Wikimedia (semplice o semantica).
- Supporto al caricamento e alla gestione immagini delle domande.
- Protezioni di login (rate limiting e mitigazione brute-force).
- Supporto opzionale a Cloudflare Turnstile CAPTCHA.
- Migrazioni database e versionamento schema con Liquibase.
- Profili di esecuzione per ambienti `dev`, `docker` e `prod`.
- Backup schedulato di SQLite per l'ambiente di produzione.

### Stack tecnologico
- Java 21, Spring Boot 4, Spring Security e Thymeleaf.
- JPA/Hibernate e Liquibase.
- Database H2 per `dev`/`docker`, SQLite per `prod`.
- Runtime embedded Jetty.
