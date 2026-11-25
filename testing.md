# Strategia di test

I test di unità verificano la logica dell'app senza server né database. L'elenco dei test, le tecniche,
le classi escluse e i rami non coperti sono in [pianoTest.md](pianoTest.md).

## Strumenti

- **JUnit 6.1.3** e **Mockito 5.24.0**, in scope `test`: non entrano nel WAR.
- Test compilati per Java 21 (JUnit 6 richiede almeno Java 17); l'app resta compilata per Java 11.
- Surefire 3.5.4 carica Mockito come agente Java e scrive nei report il `@DisplayName`.
- **JaCoCo 0.8.16** misura la copertura; **PIT 1.30.2** (mutation testing) si avvia a mano.

## Cosa testiamo

| Classe di test | Test | Oggetto |
|---|---|---|
| `CartBeanTest`, `ProductBeanTest` | 1-10 | carrello ospite, `equals`/`hashCode`, `isEmpty` |
| `ProductModelTest` | 11-19 | catalogo: lettura, ricerca, eliminazione |
| `CartModelTest` | 20-26 | carrello salvato e conferma dell'ordine |
| `UserModelTest` | 27-33 | registrazione, login, utenti, ordini |
| `AzioniAdminModelTest` | 34-36 | inserimento prodotti e sconti |
| `RecensioneModelTest` | 37-38 | recensioni |
| `UserControlTest` | 39-58 | registrazione, login, dati utente, logout |
| `AuthFilterTest` | 59-67 | accesso all'area `/admin` e alla servlet `/Admin` |
| `AdminControlTest` | 68-69 | controllo del ruolo nelle azioni admin |

I casi sono ricavati con le classi di equivalenza (una classe non valida per caso), i valori limite
(es. password di 7, 8, 20 e 21 caratteri), la partizione per categorie e le tabelle di decisione.

## Isolamento da database e server

Nei test dei DAO il database è sostituito da mock di Mockito (`testutil.JdbcMocks`); i test verificano
query, parametri, `commit`/`rollback` e chiusura delle risorse. Per servlet e filtro,
`testutil.ServletMocks` crea richiesta, risposta e sessione finte che imitano Tomcat.

## Principi FIRST

- **Fast**: niente rete né database; i 179 casi girano in pochi secondi.
- **Independent**: mock e dati nuovi in `@BeforeEach`; `DataSource` e DAO ripristinati dopo ogni test.
- **Repeatable**: dati fissi e inventati; le date limite (45 e 45b) sono calcolate rispetto a oggi.
- **Self-validating**: ogni controllo ha un messaggio; i controlli indipendenti sono raccolti in
  `assertAll` e le guardie li precedono (`// Guardia`).
- **Thorough**: ogni test verifica un solo comportamento; i test parametrizzati coprono più classi.

## Copertura e adeguatezza

Ogni classe di equivalenza ha almeno un caso, e `./mvnw verify` fallisce se un package
testato scende sotto l'80% delle righe o il 75% dei branch. Copertura prima (100 casi) e dopo (179 casi):

| Package | Righe | Branch |
|---|---|---|
| `Model` | 69% → 96% | 47% → 90% |
| `Model.Bean` | 89% → 95% | 65% → 100% |
| `admin` | 90% | 75% |

Abbiamo inserito a mano 5 difetti (es. password di 8 caratteri rifiutata): ognuno è stato scoperto
solo dai casi nuovi o modificati.

## Integrazione nella pipeline

`ci.yml` esegue `./mvnw clean test` a ogni push e pull request verso `main`, salva i report e scrive
nella run la tabella dei test. Se un test fallisce, WAR e immagine Docker non vengono prodotti.

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
| 64-68 | Azioni admin (`/Admin`) eseguibili senza login | filtro su `/Admin` e ruolo verificato nella servlet |

## Uso di strumenti AI

Parte dei test è stata scritta con un assistente AI e poi rivista (nomi, casi limite, FIRST, tecniche
black-box); la revisione ha corretto una verifica Mockito non supportata (test 26 e 29) e test con più
comportamenti o con controlli indipendenti in sequenza.

## Esecuzione

```powershell
./mvnw test     # test e report di copertura
./mvnw verify   # in più, controllo delle soglie di copertura
```

I risultati sono in `target/surefire-reports/`, la copertura in `target/site/jacoco/index.html`.
