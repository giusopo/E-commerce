# Strategia di test

I test di unità verificano la logica dell'applicazione senza server e senza database, così girano in
pochi secondi in locale e nella pipeline. L'elenco completo dei test,
numerati da 1 a 38, e delle classi escluse con il motivo è in [pianoTest.md](pianoTest.md).

## Strumenti

- **JUnit 6.1.3** (`junit-jupiter`) e **Mockito 5.24.0** (`mockito-junit-jupiter`), solo in scope `test`:
  non entrano nel WAR.
- I test sono compilati per Java 21, perché JUnit 6 richiede almeno Java 17; il codice dell'app resta per Java 11.
- Surefire 3.5.4 carica Mockito come agente Java, così Java 21 non stampa avvisi, e scrive nei report
  il `@DisplayName` di ogni test, che inizia con il suo numero nel piano.

## Cosa testiamo

| Classe di test | Test | Oggetto |
|---|---|---|
| `CartBeanTest`, `ProductBeanTest` | 1-10 | carrello dell'utente non registrato, contratto `equals`/`hashCode` |
| `ProductModelTest` | 11-19 | catalogo: lettura, ricerca, eliminazione |
| `CartModelTest` | 20-26 | carrello salvato e conferma dell'ordine |
| `UserModelTest` | 27-33 | registrazione, login, ordini |
| `AzioniAdminModelTest` | 34-36 | inserimento prodotti e sconti |
| `RecensioneModelTest` | 37-38 | recensioni |

Le priorità sono i percorsi critici (ordine, registrazione, login), i casi limite (carrello vuoto,
sconto 0 e 100%, nessun risultato) e le condizioni di errore (eccezioni SQL, chiusura delle risorse,
transazioni annullate). Le servlet e le JSP non sono coperte da test di unità: il motivo è in `pianoTest.md`.

## Isolamento dal database

I DAO ricevono il database da Tomcat via JNDI. Nei test `DataSource`, `Connection`, `PreparedStatement`
e `ResultSet` sono mock di Mockito:

- `testutil.JdbcMocks.sostituisciDataSource` inserisce il mock nel campo `ds` del DAO; `@AfterEach`
  rimette il valore originale;
- `JdbcMocks.resultSet` simula le righe lette, con le stesse conversioni di un driver JDBC;
- i test verificano query, parametri, `commit`/`rollback` e chiusura delle risorse.

## Principi FIRST

- **Fast**: nessun accesso a rete o database; i 48 casi girano in circa 2 secondi.
- **Independent**: ogni test crea i propri mock e i propri dati in `@BeforeEach`, e il `DataSource`
  viene ripristinato dopo ogni test.
- **Repeatable**: dati fissi e inventati; la data di una recensione è verificata in un intervallo,
  non contro l'orologio.
- **Self-validating**: asserzioni e verifiche Mockito con messaggi che spiegano l'errore,
  per esempio *"I prezzi già scritti devono essere annullati con rollback()"*.
- **Thorough**: test parametrizzati per più scenari dello stesso metodo (sconti, esito presente o assente).

## Integrazione nella pipeline

`ci.yml` esegue `./mvnw clean test` a ogni push e pull request verso `main`. Poi, anche se i test falliscono:

1. carica `target/surefire-reports/` come artefatto `test-results`;
2. scrive nella pagina della run una tabella dei test, ordinata per numero del piano, con i test non superati e il loro messaggio.

Se un test fallisce il WAR non viene prodotto, e quindi l'immagine Docker non viene pubblicata.

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

## Uso di strumenti AI

Parte dei test è stata scritta con un assistente AI. Ogni test è stato rivisto: nomi descrittivi,
casi limite aggiunti, rispetto dei principi FIRST. La revisione ha trovato anche un errore nei test
generati: una verifica Mockito non supportata nei test 26 e 29, corretta prima delle correzioni del codice.

## Esecuzione

```powershell
./mvnw test
```

I risultati sono in `target/surefire-reports/`.
