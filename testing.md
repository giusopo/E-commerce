# Strategia di test

## Come sono scelti i casi di test

I casi derivano da tecniche black-box:

- **Classi di equivalenza, criterio debole.** Le classi valide sono raccolte nel minor numero di casi;
  ogni caso contiene al massimo una classe non valida, così un errore non ne nasconde un altro
  (es. test 47: un solo campo vuoto per caso).
- **Valori limite.** Si provano l'ultimo valore accettato e il primo rifiutato: password di 8 e 20
  caratteri (41b) contro 7 e 21 (41), date di oggi e di domani (45b, 45), 0 e 1 righe cancellate (19).
- **Partizione per categorie.** Per ogni parametro si individuano categorie e scelte: per esempio
  0, 1 o più risultati, oppure la categoria `all` contro una categoria specifica. La connessione al
  database fallita è la scelta di errore: si prova una volta per ogni metodo dei DAO che chiude le
  risorse a mano (17, 17b, 26b, 33h, 38e). I metodi con try-with-resources sono esclusi, perché lì
  l'eccezione arriva al chiamante senza codice di chiusura che possa sbagliare.
  Per la chiusura delle risorse ci sono altre due scelte, aggiunte dopo il mutation testing:
  **chiamata riuscita** (14b, 26c, 33i, 38f: ogni statement e ogni connessione aperti vengono chiusi) e
  **query fallita** (14, 17c, 17d, 26d, 33j: la query lancia l'errore quando le risorse sono già aperte,
  l'errore arriva al chiamante invariato e le risorse vengono chiuse). La query fallita si prova solo dove
  le chiusure sono in `finally` annidati: negli altri metodi quel percorso è già coperto dalla connessione fallita.
- **Tabella di decisione.** Nei controlli di accesso (`AuthFilter`, `AdminControl`) le condizioni
  "area admin", "utente loggato" e "ruolo admin" stabiliscono se la richiesta prosegue o se l'utente
  viene reindirizzato al login. In `AdminControl` il ruolo viene controllato prima di leggere l'azione
  richiesta, che quindi è indifferente: per l'admin basta provare una sola azione.

## Classi escluse dai test

| Classe | Motivo |
|--------|--------|
| Servlet: `StarControl`, `GetPicturesControl` e le pagine del catalogo di `ProductControl` | Hanno poca logica propria: leggono i parametri, chiamano un DAO e passano la richiesta a una JSP. |
| Bean: `UserBean`, `OrdineBean`, `RecensioneBean` e i getter/setter di `ProductBean` | Contengono solo getter e setter, quindi i test sarebbero poco utili. `CartBean` invece viene testato perché contiene la logica del carrello; di `ProductBean` si testano `equals`, `hashCode` e `isEmpty` (test 10-10d). |
| `IMGmodel` | È interamente statico: per testarlo servirebbero mock statici, complessi da configurare e con poco beneficio. |
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
| `CartBeanTest`, `ProductBeanTest` | 1-10 | carrello ospite, `equals`/`hashCode`, `isEmpty` |
| `ProductModelTest` | 11-19 | catalogo: lettura, ricerca, eliminazione |
| `CartModelTest` | 20-26 | carrello salvato e conferma dell'ordine |
| `UserModelTest` | 27-33 | registrazione, login, utenti, ordini |
| `AzioniAdminModelTest` | 34-36 | inserimento prodotti e sconti |
| `RecensioneModelTest` | 37-38 | recensioni |
| `UserControlTest` | 39-58 | registrazione, login, dati utente, logout |
| `AuthFilterTest` | 59-67 | accesso all'area `/admin` e alla servlet `/Admin` |
| `AdminControlTest` | 68-69 | controllo del ruolo nelle azioni admin |
| `ProductControlTest` | 70-71 | inserimento e modifica delle recensioni |
| `CartControlTest` | 72-77 | azioni del carrello, conferma e storico degli ordini |

I casi sono ricavati con le classi di equivalenza (una classe non valida per caso), i valori limite
(es. password di 7, 8, 20 e 21 caratteri), la partizione per categorie e le tabelle di decisione.

## Isolamento da database e server

Nei test dei DAO il database è sostituito da mock di Mockito (`testutil.JdbcMocks`); i test verificano
query, parametri, `commit`/`rollback` e chiusura delle risorse. Per servlet e filtro,
`testutil.ServletMocks` crea richiesta, risposta e sessione finte che imitano Tomcat.

## Principi FIRST

- **Fast**: niente rete né database; i 261 casi girano in pochi secondi.
- **Independent**: mock e dati nuovi in `@BeforeEach`; `DataSource` e DAO ripristinati dopo ogni test.
- **Repeatable**: dati fissi e inventati; le date limite (45 e 45b) sono calcolate rispetto a oggi.
- **Self-validating**: ogni controllo ha un messaggio; i controlli indipendenti sono raccolti in
  `assertAll` e le guardie li precedono.
- **Thorough**: ogni test verifica un solo comportamento; i test parametrizzati coprono più classi.

## Copertura e adeguatezza

Ogni classe di equivalenza ha almeno un caso, e `./mvnw verify` fallisce se un package
testato scende sotto l'80% delle righe o il 75% dei branch. Copertura prima (100 casi) e dopo (261 casi):

| Package | Righe | Branch |
|---|---|---|
| `Model` | 69% → 96% | 47% → 91% |
| `Model.Bean` | 89% → 98% | 65% → 100% |
| `admin` | 90% | 75% |

Abbiamo inserito a mano 5 difetti (es. password di 8 caratteri rifiutata): ognuno è stato scoperto
solo dai casi nuovi o modificati.

## Integrazione nella pipeline

`ci.yml` esegue `./mvnw clean test` a ogni push e pull request verso `main`, salva i report e scrive
nella run la tabella dei test. Se un test fallisce, WAR e immagine Docker non vengono prodotti.


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
