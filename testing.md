# Strategia di test

## Come sono scelti i casi di test

I casi derivano da tecniche black-box:

- **Classi di equivalenza (criterio debole).** Le classi valide stanno nel minor numero di casi; ogni
  caso ha al massimo una classe non valida, così un errore non ne nasconde un altro (47: un campo vuoto per caso).
- **Valori limite.** L'ultimo valore accettato e il primo rifiutato: password di 8 e 20 caratteri (41b)
  contro 7 e 21 (41), nascita oggi e domani (45b, 45), 0 e 1 righe cancellate (19), sconto 0 e 100
  contro -1 e 101 (69g, 69e), quantità nel carrello uguale alla giacenza (73e, 74f, 26g).
- **Partizione per categorie.** Per ogni parametro, categorie e scelte (es. 0, 1 o più risultati;
  categoria `all` o specifica). Per le risorse dei DAO le scelte sono: connessione fallita (17, 17b, 26b,
  33h, 38e), chiamata riuscita (14b, 26c, 33i, 38f: tutto chiuso), query fallita (14, 17c, 17d, 26d, 33j:
  errore invariato, risorse chiuse) e chiusura dello statement fallita (17e-17h, 26e-26f, 33k-33n,
  38g-38h: connessione chiusa, errore della query conservato). Le ultime tre scelte vengono dal mutation
  testing e mirano alle chiusure scritte a mano (la query fallita dove c'erano `finally` annidati). La
  chiusura fallita ha trovato difetti in tutti i DAO, ora con try-with-resources: i test restano come regressione.
- **Tabella di decisione.** In `AuthFilter` e `AdminControl` "area admin", "utente loggato" e "ruolo
  admin" decidono se la richiesta prosegue o va al login. `AdminControl` controlla il ruolo prima di
  leggere l'azione, quindi per l'admin basta provarne una.

## Classi escluse dai test

| Classe | Motivo |
|--------|--------|
| Servlet: `StarControl`, `GetPicturesControl` e le pagine per categoria di `ProductControl` | Hanno poca logica propria: leggono i parametri, chiamano un DAO e passano la richiesta a una JSP. Della home e della scheda del prodotto si testano invece i parametri (71c-71e). |
| Bean: `UserBean`, `OrdineBean`, i getter e i setter di `ProductBean` e `RecensioneBean` | Contengono solo getter e setter, quindi i test sarebbero poco utili. Di `RecensioneBean` si testano `equals` e `hashCode` (10e, 10f). |
| Interfacce `*_intf` | Non contengono codice da eseguire. |
| `dev.LocalServer` | Serve solo ad avviare l'app in locale ed è escluso dal WAR, quindi non è codice di produzione. |
| JSP (es. `Cart.jsp`) | Non si possono testare con unit test Java. La formula del prezzo scontato che contengono è comunque verificata dal test 24 su `CartModel`. |

## Strumenti

- **JUnit 6.1.3** e **Mockito 5.24.0**, in scope `test`: non entrano nel WAR.
- Test compilati per Java 21 (JUnit 6 richiede almeno Java 17); l'app resta compilata per Java 11.
- Surefire 3.5.4 carica Mockito come agente Java e scrive nei report il `@DisplayName`.
- **JaCoCo 0.8.16** (copertura, in CI) e **PIT 1.30.2** (mutazioni, a mano): [coverage.md](coverage.md), [mutation.md](mutation.md).

## Cosa testiamo

| Classe di test | Test | Oggetto |
|---|---|---|
| `CartBeanTest`, `ProductBeanTest`, `RecensioneBeanTest` | 1-10f | carrello ospite, `equals`/`hashCode`, `isEmpty` |
| `ProductModelTest` | 11-19 | catalogo: lettura, ricerca, eliminazione |
| `CartModelTest` | 20-26h | carrello salvato, conferma dell'ordine e giacenza |
| `UserModelTest` | 27-33 | registrazione, login, utenti, ordini |
| `AzioniAdminModelTest` | 34-36 | inserimento prodotti con id generato e sconti |
| `RecensioneModelTest` | 37-38 | recensioni |
| `UserControlTest` | 39-58 | registrazione, login con unione del carrello dell'ospite, dati utente, logout |
| `AuthFilterTest` | 59-67 | accesso all'area `/admin` e alla servlet `/Admin` |
| `AdminControlTest` | 68-69g | ruolo, sconto, controlli del form di inserimento e azione sconosciuta |
| `ProductControlTest` | 70-71e | recensioni, scheda del prodotto e parametri della home |
| `CartControlTest` | 72-77d | azioni del carrello, giacenza, conferma e storico degli ordini |
| `IMGmodelTest` | 78a-78d | lettura e salvataggio delle immagini |
| `HtmlTest` | 79 | escape HTML del testo mostrato nelle pagine |

## Isolamento da database e server

Nei test dei DAO e di `IMGmodel` il database è sostituito da mock di Mockito (`testutil.JdbcMocks`); i test verificano
query, parametri, `commit`/`rollback` e chiusura delle risorse. Per servlet e filtro,
`testutil.ServletMocks` crea richiesta, risposta e sessione finte che imitano Tomcat.

## Principi FIRST

- **Fast**: niente rete né database; i 383 casi girano in pochi secondi.
- **Independent**: mock e dati nuovi in `@BeforeEach`; `DataSource` e DAO ripristinati dopo ogni test.
- **Repeatable**: dati fissi e inventati; le date limite (45 e 45b) sono calcolate rispetto a oggi.
- **Self-validating**: ogni controllo ha un messaggio; i controlli indipendenti sono raccolti in
  `assertAll` e le guardie li precedono.
- **Thorough**: ogni test verifica un solo comportamento; i test parametrizzati coprono più classi.

## Copertura e adeguatezza

Ogni classe di equivalenza ha almeno un caso, e `./mvnw verify` fallisce se un package
testato scende sotto l'80% delle righe o il 75% dei branch. Copertura prima (100 casi) e dopo (383 casi):

| Package | Righe | Branch |
|---|---|---|
| `Model` | 69% → 96% | 47% → 95% |
| `Model.Bean` | 89% → 98% | 65% → 100% |
| `admin` | 90% → 92% | 75% → 100% |

Abbiamo inserito a mano 5 difetti (es. password di 8 caratteri rifiutata): ognuno è stato scoperto
solo dai casi nuovi o modificati.

## Integrazione nella pipeline

`ci.yml` esegue `./mvnw clean test` a ogni push e pull request verso `main`, salva i report e scrive
nella run la tabella dei test. Se un test fallisce, WAR e immagine Docker non vengono prodotti.


## Uso di strumenti AI

Parte dei test è stata scritta con un assistente AI, revisioni successive hanno fatto emergere una verifica Mockito non supportata (test 26 e 29) e test con più comportamenti o con controlli indipendenti in sequenza, che sono stati corretti.

## Esecuzione

```powershell
./mvnw test     # test e report di copertura
./mvnw verify   # in più, controllo delle soglie di copertura
```

I risultati sono in `target/surefire-reports/`, la copertura in `target/site/jacoco/index.html`.
