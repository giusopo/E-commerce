# Mutation testing

PIT inserisce nel codice piccoli errori (mutanti), come una condizione negata o una chiamata tolta, e
riesegue i test su ogni versione. Un mutante è **ucciso** se almeno un test fallisce,
**sopravvissuto** se tutti passano. La copertura ([coverage.md](coverage.md)) dice quale codice i test
eseguono; il mutation score dice se se ne accorgono quando quel codice sbaglia.

## 1. Configurazione

**PIT 1.30.2** (`pitest-maven` nel `pom.xml`) con `pitest-junit5-plugin` 1.2.3 per eseguire i test JUnit 6.

| Parametro | Valore |
|---|---|
| Classi mutate (`targetClasses`) | `Model.*`, `admin.AzioniAdminModel`; esclusi `UserBean`, `OrdineBean`, `RecensioneBean` |
| Test eseguiti (`targetTests`) | `Model.*`, `admin.*` |
| Soglie | `mutationThreshold` 80%, `coverageThreshold` 80%: sotto soglia la build fallisce |
| Mutatori | insieme predefinito di PIT: negazione e limiti delle condizioni, rimozione di chiamate `void`, operatori matematici, valori restituiti |
| Righe non mutate | stampe su `System.out` (`avoidCallsTo`): toglierle non cambia il comportamento |
| Timeout | `timeoutConstant` 5000 ms, `timeoutFactor` 1,5 |
| Report | HTML e XML in `target/pit-reports/` |

Il perimetro è quello delle soglie di JaCoCo (DAO, `CartBean`, `ProductBean`), descritto in
[coverage.md](coverage.md). Classi, test e soglie sono proprietà Maven, modificabili con `-D`.

```powershell
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
start target/pit-reports/index.html
```

Nella pipeline PIT gira a ogni push e pull request con le stesse soglie; il report è caricato come
artefatto `pit-report` anche se la soglia non è raggiunta.

## 2. Analisi

Suite di 211 casi: **575 mutanti**, **460 uccisi** (459 da un test fallito, 1 per timeout),
**13 sopravvissuti**, **102 senza copertura**.

| Metrica | Valore |
|---|---|
| **Mutation score** | **80%** (460/575), esattamente sulla soglia |
| Test strength (uccisi tra i mutanti su codice eseguito) | 97% (460/473) |
| Copertura delle righe delle classi mutate | 95% (822/865) |

| Classe | Mutanti | Score |
|---|---|---|
| `CartBean` | 17 | 100% |
| `ProductBean` | 45 | 93% |
| `RecensioneModel` | 53 | 89% |
| `AzioniAdminModel` | 18 | 89% |
| `UserModel` | 143 | 86% |
| `ProductModel` | 170 | 81% |
| **`CartModel`** | 129 | **60%** |

**Mutanti senza copertura (102).** Quasi tutti (98) sono nei `finally` annidati dei DAO che chiudono le
risorse a mano: `close()` e controlli `!= null` nelle copie dei `finally` che il compilatore esegue solo
se fallisce anche una chiusura precedente, e `rs != null` di `CartModel`, sempre falso perché `rs` non
viene mai assegnato. `CartModel` ne ha 50, per questo ha lo score più basso. Gli altri 4: `addSuppressed`
quando fallisce anche il rollback (2), `toString` e `StartoJSON` di `ProductBean` (2).

**Mutanti sopravvissuti (13):**

| Mutanti | Dove | Esito dell'analisi |
|---|---|---|
| 7 | condizioni in `ProductModel` e `RecensioneModel` che proteggono solo una stampa | equivalenti |
| 2 | `if (rowsUpdated < 1)` di `AzioniAdminModel.modificaSconto`, con il blocco vuoto | equivalenti |
| 2 | `setCartID(-1)` in `UserModel`, uguale al valore di default | equivalenti |
| 1 | `hashCode()` di `ProductBean` che restituisce 0: rispetta ancora il contratto con `equals` | equivalente |
| 1 | `rs.close()` rimosso in `UserModel.getOrdineById` | **lacuna dei test**: il 33i verifica statement e connessioni, non il `ResultSet` |

**Schemi ricorrenti.** I mutanti non uccisi si concentrano sulla gestione delle risorse (chiusure,
controlli `null`, rollback), non sulla logica: tutti i mutanti su calcoli, prezzi, parametri delle
query e campi letti dal database vengono uccisi. I due mutatori più frequenti tra i non uccisi sono la
rimozione di chiamate `void` (55) e la negazione delle condizioni (54).

## 3. Piano di miglioramento

1. **Chiusura del `ResultSet` in `getOrdineById`.** Aggiungere al test 33i la verifica di `rs.close()`:
   uccide l'unico mutante non equivalente sopravvissuto e dà un po' di margine sulla soglia.
2. **Chiusura fallita.** Una categoria di test in cui `close()` lancia un'eccezione raggiungerebbe le
   copie dei `finally`. In `UserModel` e `RecensioneModel`, dove statement e connessione si chiudono nello
   stesso `finally`, un errore in `pstmt.close()` lascerebbe aperta la connessione: da verificare con
   un test prima di correggere il codice.
3. **Rollback fallito** in `confermaOrdine` e `AddUser`: l'errore originale deve arrivare al chiamante
   con quello del rollback tra i soppressi.
4. **Codice morto e equivalenti.** Togliere `rs` da `CartModel` e i controlli che proteggono solo una
   stampa: si eliminano mutanti che nessun test può uccidere.
5. **Estendere il perimetro** a `UserControl` e `AuthFilter`, che contengono la logica di login e il
   controllo di accesso, dopo aver tolto il codice morto di `UserControl`.

L'andamento delle metriche da 100 a 211 casi è in [metriche.md](metriche.md).
