## 1. Configurazione Mutation testing

**PIT 1.30.2** (`pitest-maven` nel `pom.xml`), con `pitest-junit5-plugin` 1.2.3 per eseguire i test JUnit 6.

| Parametro | Valore |
|---|---|
| Classi mutate | `Model.*` e `admin.AzioniAdminModel`, esclusi `UserBean`, `OrdineBean`, `RecensioneBean` |
| Soglie | `mutationThreshold` e `coverageThreshold` all'80%, come suggerisce la lab 4 |
| Mutatori | predefiniti: condizioni, chiamate `void`, operatori matematici, valori restituiti |
| Non mutate | stampe su `System.out` (`avoidCallsTo`) |
| Report | HTML e XML in `target/pit-reports/` |

Le classi mutate sono il perimetro delle soglie di JaCoCo, descritto in [coverage.md](coverage.md).

```powershell
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
start target/pit-reports/index.html
```

Su GitHub PIT non gira a ogni push, per non consumare la quota di Actions: si avvia a mano con
[mutation.yml](.github/workflows/mutation.yml) (*Actions* → *Mutation Testing* → *Run workflow*), anche
solo su alcune classi. Score nella pagina della run, report nell'artefatto `pit-report`. Sotto soglia la
build fallisce; la soglia vale per l'insieme delle classi analizzate, e ora ogni classe la supera anche da
sola.

## 2. Analisi

Dei 314 casi, PIT esegue i 207 dei package `Model` e `admin`: **330 mutanti**, **316 uccisi**,
**10 sopravvissuti**, **4 senza copertura**.

| Metrica | Valore |
|---|---|
| **Mutation score** | **96%** (316/330): 52 mutanti uccisi di margine sulla soglia |
| Test strength (uccisi tra i mutanti su codice eseguito) | 97% (316/326) |
| Copertura delle righe delle classi mutate | 97% (662/683) |

I mutanti sono scesi da 575 (80%) a 543 (84%) togliendo la variabile `rs` mai usata da `CartModel`, a 497
(87%) con try-with-resources in `CartModel`, poi a 326 (95%) con try-with-resources negli altri DAO, dopo
i difetti trovati dai test sulla chiusura fallita ([metriche.md](metriche.md)). PIT non muta il codice di
chiusura generato dal compilatore: sono spariti i 48 mutanti senza copertura dei `finally` annidati.
Sono saliti a 330 (96%) con le correzioni emerse ricostruendo le specifiche ([specifiche.md](specifiche.md)):
`UserModel` legge la data di nascita e `modificaSconto` restituisce l'esito al posto di un `if` vuoto.

| Classe | Mutanti | Score |
|---|---|---|
| `CartBean` | 17 | 100% |
| `AzioniAdminModel` | 20 | 100% |
| `ProductModel` | 75 | 99% |
| `CartModel` | 51 | 98% |
| `UserModel` | 89 | 97% |
| `ProductBean` | 45 | 93% |
| **`RecensioneModel`** | 33 | **82%** |

**Senza copertura (4):** `addSuppressed` del rollback fallito (`CartModel.annulla`, `UserModel.AddUser`)
e `toString` e `StartoJSON` di `ProductBean`, che i test di `Model` e `admin` non chiamano.

**Sopravvissuti (10):**

| Mutanti | Dove | Impatto sul comportamento |
|---|---|---|
| 7 | condizioni di `ProductModel` e `RecensioneModel` che scelgono solo quale stampa fare | secondario |
| 2 | `setCartID(-1)` in `UserModel`, uguale al valore di default | secondario |
| 1 | `hashCode()` di `ProductBean` che restituisce 0: resta coerente con `equals` | secondario |

Il sopravvissuto su `rs.close()` in `getOrdineById`, l'unica lacuna dei test, non c'è più: la chiusura del
`ResultSet` è garantita da try-with-resources. Nemmeno i 2 equivalenti dell'`if` vuoto di `modificaSconto`
ci sono più. Nessuno dei 14 non uccisi riguarda la logica.

## 3. Piano di miglioramento suggerito dall'agente (claudeCode Opus5.5)

1. **Rollback fallito** in `confermaOrdine` e `AddUser`: l'errore originale deve arrivare al chiamante,
   quello del rollback tra i soppressi. Uccide 2 dei 4 mutanti senza copertura.
2. **Equivalenti.** Togliere i controlli che proteggono solo una stampa (6 in `RecensioneModel`), come già
   fatto con `rs` in `CartModel` e con l'`if` vuoto di `modificaSconto`.
3. **Estendere il perimetro** a `UserControl` e `AuthFilter` (login e controllo di accesso), dopo aver
   tolto il codice morto di `UserControl`.
