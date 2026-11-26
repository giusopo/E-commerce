## 1. Configurazione Mutation testing

**PIT 1.30.2** (`pitest-maven` nel `pom.xml`) con `pitest-junit5-plugin` 1.2.3 per eseguire i test JUnit 6.


Classi mutate (`targetClasses`): `Model.*`, `admin.AzioniAdminModel`; 
esclusi `UserBean`, `OrdineBean`, `RecensioneBean`;

Soglie: `mutationThreshold` 80%, `coverageThreshold` 80%: sotto soglia la build fallisce.
   sono state scelte queste soglie sotto consiglio del tutorial(lab4).

Mutatori allplicati di PIT: negazione e limiti delle condizioni, rimozione di chiamate `void`, operatori matematici, valori restituiti;

Righe non mutate: stampe su `System.out` (`avoidCallsTo`): toglierle non cambia il comportamento.
Report : HTML e XML in `target/pit-reports/` 

Il perimetro è quello delle soglie di JaCoCo (DAO, `CartBean`, `ProductBean`), descritto in
[coverage.md](coverage.md).

```powershell
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
start target/pit-reports/index.html
```

Su GitHub PIT non gira a ogni push: come suggerisce il tutorial, per non consumare la quota di Actions
ha un workflow a parte, [mutation.yml](.github/workflows/mutation.yml), che si avvia a mano (scheda
*Actions* → *Mutation Testing* → *Run workflow*).

Se si compilano i due campi
del form, analizza solo le classi e i test indicati. Lo score compare nella pagina della run e il report
nell'artefatto `pit-report`, anche se la soglia non è raggiunta.

La soglia vale per l'insieme delle classi analizzate in quella esecuzione, non per ogni classe. Con il
perimetro completo lo score è dell'84%, ma un'analisi del solo `CartModel` si ferma al 74% e fallisce.
Per leggere il report di una sola classe senza far fallire la build, in locale si può aggiungere
`"-Dpit.mutationThreshold=0"`: il report viene scritto comunque.

## 2. Analisi

Suite di 211 casi: **543 mutanti**, **454 uccisi** (453 da un test fallito, 1 per timeout),
**13 sopravvissuti**, **76 senza copertura**.

| Metrica | Valore |
|---|---|
| **Mutation score** | **84%** (454/543): prima di scendere sotto la soglia si possono perdere 22 mutanti uccisi |
| Test strength (uccisi tra i mutanti su codice eseguito) | 97% (454/467) |
| Copertura delle righe delle classi mutate | 95% (816/855) |

Prima di togliere da `CartModel` la variabile `rs`, mai usata, i mutanti erano 575 e lo score era
esattamente l'80% (460/575), senza margine. Il dettaglio è in [metriche.md](metriche.md).

| Classe | Mutanti | Score |
|---|---|---|
| `CartBean` | 17 | 100% |
| `ProductBean` | 45 | 93% |
| `RecensioneModel` | 53 | 89% |
| `AzioniAdminModel` | 18 | 89% |
| `UserModel` | 143 | 86% |
| `ProductModel` | 170 | 81% |
| **`CartModel`** | 97 | **74%** |

**Mutanti senza copertura (76).** Quasi tutti (72) sono nei `finally` annidati dei DAO che chiudono le
risorse a mano: `close()` e controlli `!= null` nelle copie dei `finally` che il compilatore esegue solo
se fallisce anche una chiusura precedente. Sono 32 in `ProductModel`, 24 in `CartModel` e 16 in
`UserModel`. `CartModel` ha lo score più basso perché questi mutanti sono un quarto dei suoi. Gli altri 4:
`addSuppressed` quando fallisce anche il rollback (2, in `CartModel.annulla` e `UserModel.AddUser`),
`toString` e `StartoJSON` di `ProductBean` (2).

**Mutanti sopravvissuti (13):**

| Mutanti | Dove | Impatto nel comportamento del programma |
|---|---|---|
| 7 | condizioni in `ProductModel` e `RecensioneModel` alternano delle stampe nella console |  Secondario |
| 2 | `if (rowsUpdated < 1)` di `AzioniAdminModel.modificaSconto`, con il blocco vuoto | Secondario |
| 2 | `setCartID(-1)` in `UserModel`, uguale al valore di default | Secondario |
| 1 | `hashCode()` di `ProductBean` che restituisce 0: rispetta ancora le condizioni di `equals` | Secondario |
| 1 | `rs.close()` rimosso in `UserModel.getOrdineById` | **lacuna dei test**: il 33i verifica statement e connessioni, non il `ResultSet` |

**Analisi dei sopravvissuti** I mutanti non uccisi si concentrano sulla gestione delle risorse (chiusure,
controlli `null`, rollback), non sulla logica. 

I due mutatori più frequenti tra gli 89 non uccisi sono la
negazione delle condizioni (42) e la rimozione di chiamate `void` (41).

## 3. Piano di miglioramento suggerito dall'agente (claudeCode Opus5.5)

1. **Chiusura del `ResultSet` in `getOrdineById`.** Aggiungere al test 33i la verifica di `rs.close()`:
   uccide l'unico mutante non equivalente sopravvissuto.
2. **Chiusura fallita.** Una categoria di test in cui `close()` lancia un'eccezione raggiungerebbe le
   copie dei `finally`. In `CartModel` bastano 4 test, uno con la query riuscita e uno con la query
   fallita per `addProduct` e per `deleteProduct`, in cui `pstmt.close()` fallisce: la connessione deve
   essere chiusa lo stesso. La classe passerebbe da 72 a 80 mutanti uccisi su 97 (82%) e supererebbe la
   soglia anche da sola. In `UserModel` e `RecensioneModel`, dove statement e connessione si chiudono
   nello stesso `finally`, un errore in `pstmt.close()` lascerebbe aperta la connessione: da verificare
   con un test prima di correggere il codice.
3. **Rollback fallito** in `confermaOrdine` e `AddUser`: l'errore originale deve arrivare al chiamante
   con quello del rollback tra i soppressi.
4. **Equivalenti.** Togliere i controlli che proteggono solo una stampa e l'`if` vuoto di
   `modificaSconto`: si eliminano mutanti che nessun test può uccidere. La stessa operazione è già stata
   fatta in `CartModel`, togliendo la variabile `rs` mai usata: lo score complessivo è salito dall'80%
   all'84% senza aggiungere test.
5. **Estendere il perimetro** a `UserControl` e `AuthFilter`, che contengono la logica di login e il
   controllo di accesso, dopo aver tolto il codice morto di `UserControl`.

L'andamento delle metriche da 100 a 211 casi, e dopo la rimozione di `rs`, è in [metriche.md](metriche.md).
