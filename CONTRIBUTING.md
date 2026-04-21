# Contribuire ad Alice's Simple Quiz Maker

Grazie per l'interesse nel progetto! 🎉

Questo documento raccoglie le linee guida minime per mantenere il codice coerente, leggibile e facile da revisionare.

## Flusso consigliato

1. Crea un branch dedicato alla modifica.
2. Mantieni ogni commit piccolo e con un obiettivo chiaro.
3. Aggiorna test e documentazione nella stessa PR quando introduci nuove funzionalità.

## Standard di sviluppo

- Segui il layering applicativo già presente (`controller -> service -> repository/entity`).
- Evita accoppiamenti trasversali e logica di business nei controller.
- Per cambi schema database usa sempre una nuova migration Liquibase (mai riscrivere migration già applicate).
- Se aggiungi configurazioni nuove, documentale nel `README.md`.

## Qualità e verifica

Prima di aprire una PR, esegui almeno i controlli essenziali disponibili nel tuo ambiente:

```bash
mvn test
```

Se non puoi eseguire i test completi (es. limiti di rete o dipendenze esterne), indica chiaramente nel testo della PR cosa non è stato validato e perché.

## Pull Request

Nel testo PR cerca di includere:

- contesto/motivazione della modifica;
- elenco puntuale dei cambiamenti;
- test/comandi eseguiti e relativo esito.
