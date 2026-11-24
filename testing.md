# Strategia di test

I test di unità verificano la logica dell'applicazione senza server né database. L'elenco dei test
(1-63) e delle classi escluse, con il motivo, è in [pianoTest.md](pianoTest.md).

## Strumenti

- **JUnit 6.1.3** (`junit-jupiter`) e **Mockito 5.24.0** (`mockito-junit-jupiter`), solo in scope `test`:
  non entrano nel WAR.
- I test sono compilati per Java 21, perché JUnit 6 richiede almeno Java 17; il codice dell'app resta per Java 11.
- Surefire 3.5.4 carica Mockito come agente Java (niente avvisi su Java 21) e mette nei report
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
| `UserControlTest` | 39-58 | registrazione, login, dati utente, logout |
| `AuthFilterTest` | 59-63 | accesso all'area `/admin` |

Le priorità sono i percorsi critici (ordine, registrazione, login), i casi limite (carrello vuoto,
sconto 0 e 100%, nessun risultato) e le condizioni di errore (eccezioni SQL, chiusura delle risorse,
transazioni annullate). Delle servlet sono testate solo `UserControl` e `AuthFilter`; le altre e le JSP
no (motivo in `pianoTest.md`).

## Isolamento da database e server

I DAO ricevono il database da Tomcat via JNDI. Nei test `DataSource`, `Connection`, `PreparedStatement`
e `ResultSet` sono mock di Mockito:

- `testutil.JdbcMocks.sostituisciDataSource` inserisce il mock nel campo `ds` del DAO; `@AfterEach`
  rimette il valore originale;
- `JdbcMocks.resultSet` simula le righe lette, con le stesse conversioni di un driver JDBC;
- i test verificano query, parametri, `commit`/`rollback` e chiusura delle risorse.

Per `UserControl` e `AuthFilter` i DAO sono mock nei campi statici della servlet e `testutil.ServletMocks`
crea richiesta, risposta e sessione finte (una sessione invalidata lancia `IllegalStateException`, come in Tomcat).

## Principi FIRST

- **Fast**: nessun accesso a rete o database; gli 81 casi girano in circa 4 secondi.
- **Independent**: ogni test crea mock e dati in `@BeforeEach`; `DataSource` e DAO della servlet
  vengono ripristinati dopo ogni test.
- **Repeatable**: dati fissi e inventati; la data di una recensione è verificata in un intervallo,
  non contro l'orologio.
- **Self-validating**: asserzioni e verifiche Mockito con messaggi che spiegano l'errore,
  per esempio *"I prezzi già scritti devono essere annullati con rollback()"*.
- **Thorough**: test parametrizzati per più scenari dello stesso metodo (sconti, password deboli, date).

## Integrazione nella pipeline

`ci.yml` esegue `./mvnw clean test` a ogni push e pull request verso `main`. Anche se i test falliscono,
carica `target/surefire-reports/` come artefatto `test-results` e scrive nella pagina della run la
tabella dei test, ordinata per numero del piano, con i messaggi dei test non superati.
Se un test fallisce, il WAR e l'immagine Docker non vengono prodotti.

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
| 41-45 | Il server accettava password deboli o vuote, email, telefoni e date non validi | controlli del form ripetuti sul server |
| 46 | Data in formato errato: errore 500 | risposta `val 0` |
| 48 | Errore del database segnalato come registrazione riuscita | risposta `val 0` |
| 52, 56 | `NullPointerException` senza parametri di login o senza login | controllo dei valori `null` |
| 53 | Secondo login: dati scritti nella sessione appena invalidata | uso della nuova sessione |

## Uso di strumenti AI

Parte dei test è stata scritta con un assistente AI e poi rivista: nomi descrittivi, casi limite,
principi FIRST. La revisione ha corretto anche una verifica Mockito non supportata nei test 26 e 29.

## Esecuzione

```powershell
./mvnw test
```

I risultati sono in `target/surefire-reports/`.
