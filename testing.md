# Strategia di test

I test di unità verificano la logica dell'app senza server né database. L'elenco dei test (1-69)
e delle classi escluse, con il motivo, è in [pianoTest.md](pianoTest.md).

## Strumenti

- **JUnit 6.1.3** e **Mockito 5.24.0** (`mockito-junit-jupiter`), in scope `test`: non entrano nel WAR.
- Test compilati per Java 21 (JUnit 6 richiede almeno Java 17); l'app resta compilata per Java 11.
- Surefire 3.5.4 carica Mockito come agente Java e scrive nei report il `@DisplayName` (numero del
  test nel piano e descrizione).

## Cosa testiamo

| Classe di test | Test | Oggetto |
|---|---|---|
| `CartBeanTest`, `ProductBeanTest` | 1-10 | carrello ospite, contratto `equals`/`hashCode` |
| `ProductModelTest` | 11-19 | catalogo: lettura, ricerca, eliminazione |
| `CartModelTest` | 20-26 | carrello salvato e conferma dell'ordine |
| `UserModelTest` | 27-33 | registrazione, login, ordini |
| `AzioniAdminModelTest` | 34-36 | inserimento prodotti e sconti |
| `RecensioneModelTest` | 37-38 | recensioni |
| `UserControlTest` | 39-58 | registrazione, login, dati utente, logout |
| `AuthFilterTest` | 59-67 | accesso all'area `/admin` e alla servlet `/Admin` |
| `AdminControlTest` | 68-69 | controllo del ruolo nelle azioni admin |

Le priorità sono percorsi critici (ordine, registrazione, login), casi limite (carrello vuoto, sconto
0 e 100%, nessun risultato) ed errori (eccezioni SQL, risorse da chiudere, transazioni annullate).
Le altre servlet e le JSP non sono testate (motivo in `pianoTest.md`).

## Isolamento da database e server

Nei test dei DAO `DataSource`, `Connection`, `PreparedStatement` e `ResultSet` sono mock di Mockito:

- `testutil.JdbcMocks.sostituisciDataSource` inserisce il mock nel campo `ds` del DAO, ripristinato in `@AfterEach`;
- `JdbcMocks.resultSet` simula le righe lette come un driver JDBC;
- i test verificano query, parametri, `commit`/`rollback` e chiusura delle risorse.

Per servlet e filtro i DAO sono mock nei campi statici e `testutil.ServletMocks` crea richiesta,
risposta e sessione finte che imitano Tomcat.

## Principi FIRST

- **Fast**: niente rete né database; i 100 casi girano in pochi secondi.
- **Independent**: mock e dati nuovi in `@BeforeEach`; `DataSource` e DAO della servlet ripristinati
  dopo ogni test.
- **Repeatable**: dati fissi e inventati; nessuna dipendenza dall'orologio o dall'ordine dei test.
- **Self-validating**: ogni controllo ha un messaggio che spiega l'errore. I controlli indipendenti
  sono raccolti in `assertAll`, così un fallimento non nasconde gli altri; le guardie (controlli senza
  i quali i successivi non hanno senso) vengono prima, segnate con `// Guardia`.
- **Thorough**: un test verifica un solo comportamento (es. 32a/32b);
  i test parametrizzati coprono più scenari dello stesso metodo (sconti, password deboli, date).

## Integrazione nella pipeline

`ci.yml` esegue `./mvnw clean test` a ogni push e pull request verso `main`. Anche con test falliti
carica i report come artefatto `test-results` e scrive nella run la tabella dei test, ordinata per
numero del piano. Se un test fallisce, WAR e immagine Docker non vengono prodotti.

## Difetti trovati dai test

| Test | Difetto | Correzione |
|---|---|---|
| 7-9b | Incremento e decremento confrontavano i prodotti con `equals()` | confronto per ID |
| 10 | `hashCode()` assente | aggiunto, coerente con `equals()` |
| 13 | Prezzo e grammatura letti con `getInt`, decimali persi | `getDouble` |
| 17 | `NullPointerException` al posto della `SQLException` | chiusura solo se la connessione esiste |
| 18 | SQL injection su categoria e sottocategoria | parametri del `PreparedStatement` |
| 26 | Ordine confermato a metà se un passo falliva | transazione con `rollback` |
| 29 | Utente registrato senza carrello | transazione con `rollback` |
| 36 | Connessioni mai chiuse, pool esaurito | try-with-resources |
| 38 | `RecensioneBean.isEmpty()` sempre falso | confronto con i valori di default |
| 41-45 | Il server accettava password deboli, email, telefoni e date non validi | validazione anche sul server |
| 46 | Data in formato errato: errore 500 | risposta `val 0` |
| 48 | Errore del database segnalato come registrazione riuscita | risposta `val 0` |
| 52, 56 | `NullPointerException` senza parametri di login o senza login | controllo dei valori `null` |
| 53 | Secondo login: dati scritti nella sessione appena invalidata | uso della nuova sessione |
| 64-68 | Azioni admin (`/Admin`) eseguibili senza login | filtro anche su `/Admin`, ruolo verificato nella servlet |

## Uso di strumenti AI

Parte dei test è stata scritta con un assistente AI e poi rivista: nomi descrittivi, casi limite,
principi FIRST. La revisione ha corretto una verifica Mockito non supportata (test 26 e 29), test con
più comportamenti e controlli indipendenti in sequenza.

## Esecuzione

```powershell
./mvnw test
```

I risultati sono in `target/surefire-reports/`.
