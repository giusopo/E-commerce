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

Dei 267 casi, PIT esegue i 160 dei package `Model` e `admin`: **497 mutanti**, **432 uccisi** (1 per
timeout), **13 sopravvissuti**, **52 senza copertura**.

| Metrica | Valore |
|---|---|
| **Mutation score** | **87%** (432/497): 36 mutanti uccisi di margine sulla soglia |
| Test strength (uccisi tra i mutanti su codice eseguito) | 97% (432/445) |
| Copertura delle righe delle classi mutate | 96% (792/827) |

I mutanti sono scesi da 575 (80%) a 543 (84%) togliendo la variabile `rs` mai usata da `CartModel`, poi
a 497 con il passaggio a try-with-resources dopo il difetto trovato dal 26f ([metriche.md](metriche.md)).

| Classe | Mutanti | Score |
|---|---|---|
| `CartBean` | 17 | 100% |
| `CartModel` | 51 | 98% |
| `ProductBean` | 45 | 93% |
| `RecensioneModel` | 53 | 89% |
| `AzioniAdminModel` | 18 | 89% |
| `UserModel` | 143 | 86% |
| **`ProductModel`** | 170 | **81%** |

**Senza copertura (52).** 48 sono nelle copie dei `finally` annidati, eseguite solo se fallisce anche una
chiusura precedente: 32 in `ProductModel`, che per questo ha lo score più basso, e 16 in `UserModel`.
`CartModel` non ne ha più, perché PIT non muta il codice di chiusura generato per try-with-resources. Gli
altri 4: `addSuppressed` del rollback fallito (`CartModel.annulla`, `UserModel.AddUser`) e `toString` e
`StartoJSON` di `ProductBean`.

**Sopravvissuti (13):**

| Mutanti | Dove | Impatto sul comportamento |
|---|---|---|
| 7 | condizioni di `ProductModel` e `RecensioneModel` che scelgono solo quale stampa fare | secondario |
| 2 | `if (rowsUpdated < 1)` con il blocco vuoto in `AzioniAdminModel.modificaSconto` | secondario |
| 2 | `setCartID(-1)` in `UserModel`, uguale al valore di default | secondario |
| 1 | `hashCode()` di `ProductBean` che restituisce 0: resta coerente con `equals` | secondario |
| 1 | `rs.close()` rimosso in `UserModel.getOrdineById` | **lacuna dei test**: il 33i non verifica il `ResultSet` |

I 65 non uccisi riguardano la gestione delle risorse, non la logica; i mutatori più frequenti sono la
negazione delle condizioni (30) e la rimozione di chiamate `void` (29).

## 3. Piano di miglioramento suggerito dall'agente (claudeCode Opus5.5)

1. **`ResultSet` in `getOrdineById`.** Verificare `rs.close()` nel test 33i: uccide l'unico
   sopravvissuto non equivalente.
2. **Chiusura fallita negli altri DAO.** I test 26e e 26f, estesi a `ProductModel` e `UserModel` che
   chiudono ancora a mano, raggiungerebbero i 48 mutanti senza copertura e verificherebbero se c'è lo
   stesso difetto di `CartModel` (errore della query perso se fallisce anche `pstmt.close()`).
3. **Rollback fallito** in `confermaOrdine` e `AddUser`: l'errore originale deve arrivare al chiamante,
   quello del rollback tra i soppressi.
4. **Equivalenti.** Togliere i controlli che proteggono solo una stampa e l'`if` vuoto di
   `modificaSconto`, come già fatto con `rs` in `CartModel` (score dall'80% all'84% senza nuovi test).
5. **Estendere il perimetro** a `UserControl` e `AuthFilter` (login e controllo di accesso), dopo aver
   tolto il codice morto di `UserControl`.
