# Evoluzione di copertura e mutation score

Questo documento mostra come sono cambiate le metriche di qualità dei test nelle cinque tappe del
lavoro, e quali modifiche hanno prodotto ogni variazione. Nelle prime tre cambia la suite di test;
nella quarta cambia il codice, senza nuovi test; nella quinta un test nuovo trova un difetto e la
correzione cambia il codice. Il dettaglio dei singoli test è in [pianoTest.md](pianoTest.md).

## Cosa si misura

| Metrica | Strumento | Domanda | Soglia |
|---|---|---|---|
| Copertura di righe e branch | JaCoCo 0.8.16 | Quanto codice viene **eseguito** dai test? | 80% righe, 75% branch, 100% classi per package |
| Mutation score | PIT 1.30.2 | Quanti errori inseriti apposta (mutanti) vengono **scoperti** dai test? | 80% |
| Test strength | PIT 1.30.2 | Dei mutanti su codice eseguito, quanti vengono scoperti? | nessuna |

Il perimetro è lo stesso per i due strumenti: i DAO (`Model.*Model`, `AzioniAdminModel`), `CartBean` e
`ProductBean`. Servlet, bean con soli getter e setter, `IMGmodel` e `LocalServer` sono esclusi, come
spiegato in `pianoTest.md`. Nelle prime tre tappe il codice non cambia e PIT genera sempre gli stessi 575
mutanti, quindi i numeri sono confrontabili tra loro. Nelle tappe 4 e 5 cambia il codice di `CartModel`
e i mutanti scendono a 543 e poi a 497.

## Riepilogo

| | Tappa 1: 100 casi | Tappa 2: 179 casi | Tappa 3: 211 casi | Tappa 4: 211 casi, senza `rs` | Tappa 5: 267 casi, try-with-resources |
|---|---|---|---|---|---|
| JaCoCo `Model`: righe / branch | 69,5% / 47,1% | 96,3% / 90,4% | 96,3% / 90,9% | 96,6% / 91,7% | 96,4% / 91,1% |
| JaCoCo `Model.Bean`: righe / branch | 89,2% / 64,7% | 95,0% / 100% | 95,8% / 100% | 95,8% / 100% | 98,3% / 100% |
| JaCoCo `admin`: righe / branch | 90,3% / 75,0% | 90,3% / 75,0% | 90,3% / 75,0% | 90,3% / 75,0% | 90,3% / 75,0% |
| `./mvnw verify` (soglie JaCoCo) | fallisce | passa | passa | passa | passa |
| PIT: line coverage | 71% (616/865) | 94% (817/865) | 95% (822/865) | 95% (816/855) | 96% (792/827) |
| PIT: mutanti senza copertura | 248 | 123 | 102 | 76 | 52 |
| **PIT: mutation score** | **39%** (225/575) | **57%** (327/575) | **80%** (460/575) | **84%** (454/543) | **87%** (432/497) |
| PIT: test strength | 69% | 72% | 97% | 97% | 97% |

Mutation score per classe (`CartModel` ha 129 mutanti fino alla tappa 3, 97 nella tappa 4 e 51 nella 5):

| Classe | Mutanti | Tappa 1 | Tappa 2 | Tappa 3 | Tappa 4 | Tappa 5 |
|---|---|---|---|---|---|---|
| `ProductModel` | 170 | 26% | 48% | 81% | 81% | 81% |
| `UserModel` | 143 | 24% | 56% | 86% | 86% | 86% |
| `CartModel` | 129 → 97 → 51 | 36% | 41% | 60% | 74% | **98%** |
| `RecensioneModel` | 53 | 58% | 74% | 89% | 89% | 89% |
| `ProductBean` | 45 | 80% | 91% | 93% | 93% | 93% |
| `AzioniAdminModel` | 18 | 89% | 89% | 89% | 89% | 89% |
| `CartBean` | 17 | 100% | 100% | 100% | 100% | 100% |

## Tappa 1: la suite di partenza (100 casi)

È la suite del Lab 3 dopo la revisione con `assertAll` (commit `94e1cc2`): test dei DAO, di `CartBean`,
`ProductBean`, `UserControl`, `AuthFilter` e `AdminControl`. Le soglie di JaCoCo non erano rispettate nei
package `Model` e `Model.Bean`. Il mutation score era del 39%: interi metodi non avevano test
(`getOrdineById`, `getUtenti`, `doRetrieveBySubCat`) e 248 mutanti cadevano su codice mai eseguito.

## Tappa 2: tecniche black-box (179 casi)

I casi sono stati ricavati sistematicamente con le tecniche del corso:

- classi di equivalenza e valori limite su `CartBean` e `ProductBean` (2a, 9c, 9d, 10b-10d);
- partizione per categorie nei DAO: 0, 1 o più righe, categoria `all` o specifica, ordinamento indicato o
  assente (15b, 18b-18d, 23b, 33b-33g, 38a-38e);
- connessione al database fallita, una volta per ogni metodo che chiude le risorse a mano (17b, 26b, 33h, 38e);
- valori limite nella registrazione (41b, 44, 45, 45b, 47b) e tabelle di decisione per l'accesso admin (63b, 68a-69).

**Effetto.** La copertura di `Model` è salita dal 69,5% al 96,3% delle righe e dal 47,1% al 90,4% dei
branch, e `verify` ha cominciato a passare. Il mutation score è salito solo dal 39% al 57%. Il guadagno
viene quasi tutto dal codice prima non eseguito: i mutanti senza copertura sono scesi da 248 a 123. La test
strength invece è rimasta quasi ferma (dal 69% al 72%): il codice nuovo veniva eseguito, ma le asserzioni
non ne controllavano tutti gli effetti.

## Tappa 3: test guidati dal mutation testing (211 casi)

L'analisi del report di PIT della tappa 2 ha diviso i 242 mutanti non scoperti di `Model` in tre gruppi:
185 sulla chiusura delle risorse (`close()` e `if (connection != null)`), 46 su campi e parametri non
verificati, 9 equivalenti. Le aggiunte rispondono a quei gruppi:

1. **Verifica completa di campi e parametri.** I test di lettura controllano tutti i campi copiati dal
   `ResultSet`, non solo l'id e il nome (15, 18b, 18d, 30, 33b, 33d, 33f). Il test 24 controlla anche
   l'ordine e il prodotto di ogni prezzo, l'IVA e la data di inoltro; il 23 verifica che il `ResultSet`
   venga chiuso.
2. **Transazioni confermate.** Prima nessun test si sarebbe accorto della mancanza di `commit()` in
   `confermaOrdine` e `AddUser`: veniva provato solo il rollback. I test 24b e 28b verificano il commit dopo
   l'ultimo passo e il ripristino dell'autocommit.
3. **Categoria "chiamata riuscita"** (14b, 26c, 33i, 38f): dopo una chiamata senza errori, ogni
   statement e ogni connessione aperti vengono chiusi. È lo stesso tipo di difetto del test 36, che negli
   altri DAO sarebbe passato inosservato.
4. **Categoria "query fallita"** (17c, 17d, 26d, 33j): la query lancia l'errore quando le risorse sono già
   aperte. L'errore deve arrivare al chiamante invariato e le risorse vanno chiuse. Il compilatore duplica i
   blocchi `finally`, e queste copie vengono eseguite solo in caso di errore: JaCoCo le nasconde, PIT no.

**Effetto.** Il mutation score è salito dal 57% all'80% e la test strength dal 72% al 97%, mentre la
copertura di JaCoCo è rimasta praticamente uguale (+0,5 punti di branch in `Model`). I nuovi test non
eseguono codice nuovo: controllano meglio quello che era già eseguito.

## Tappa 4: codice morto tolto da `CartModel` (211 casi)

L'analisi di PIT limitata a `CartModel` dava 129 mutanti: 78 uccisi, nessun sopravvissuto e 51 senza
copertura, cioè uno score del 60% con una test strength del 100%. I test uccidevano tutti i mutanti che
eseguivano, e il problema era il codice che nessun test eseguiva. Di quei 51 mutanti, 26 non si potevano
raggiungere in nessun modo. In `addProduct` e `deleteProduct` la variabile `rs` veniva dichiarata ma mai
assegnata, perché i due metodi eseguono solo `executeUpdate`. Quindi `rs.close()` non veniva mai eseguito
(2 mutanti), e lo stesso valeva per le copie dei `finally` che il compilatore genera per il caso in cui
`rs.close()` fallisce (24 mutanti).

**Modifica.** Da `addProduct` e `deleteProduct` sono stati tolti la variabile `rs` e il livello di
`finally` che la chiudeva (commit `ea4bad7`). I test non sono cambiati: i 211 casi passano.

**Effetto.**

| `CartModel` | Tappa 3 | Tappa 4 |
|---|---|---|
| Mutanti | 129 | 97 |
| Uccisi / senza copertura / sopravvissuti | 78 / 51 / 0 | 72 / 25 / 0 |
| Mutation score | 60% | 74% |
| JaCoCo: righe / branch | 94,4% / 93,3% | 95,8% / 100% |

- Spariscono 32 mutanti: i 26 irraggiungibili e 6 che i test uccidevano. Di questi 6, 2 erano sul controllo
  `rs != null`. Gli altri 4 c'erano perché PIT contava separatamente le due copie del controllo su `pstmt`.
  Ora quel controllo è nel `finally` più esterno, PIT riconosce le due copie come uguali e le conta una
  volta sola.
- Lo score complessivo sale dall'80% (460/575) all'84% (454/543). Prima bastava un solo mutante ucciso in
  meno per far fallire PIT; ora se ne possono perdere 22.
- In JaCoCo spariscono i 2 branch `rs != null`, che non si potevano coprire: `CartModel` arriva al 100%
  dei branch e il package `Model` passa dal 90,9% al 91,7%.
- `CartModel` restava comunque sotto l'80% se analizzato da solo: il workflow *Mutation Testing*
  avviato su GitHub con la sola `CartModel` falliva con 72 mutanti uccisi su 97. I 25 mutanti rimasti si
  raggiungono solo con test in cui una chiusura o il rollback falliscono.

## Tappa 5: chiusura dello statement fallita (267 casi)

In questa tappa la suite comprende anche i test dei controller `ProductControl` e `CartControl` (70-77b,
50 casi), descritti in [pianoTest.md](pianoTest.md). PIT non li esegue, perché analizza solo i test dei
package `Model` e `admin` (160 casi), quindi non cambiano il mutation score. In JaCoCo invece esercitano
anche `CartBean` e `ProductBean`: il package `Model.Bean` sale dal 95,8% al 98,3% delle righe.

Dei 25 mutanti senza copertura di `CartModel`, 24 erano nelle copie dei `finally` che si eseguono solo se
fallisce `pstmt.close()`. Per raggiungerle è stata aggiunta alla partizione per categorie una nuova scelta,
**chiusura dello statement fallita**, sugli stessi tre metodi dei test 26b-26d (6 casi):

- **26e**: la query riesce e poi fallisce la chiusura dello statement. Quell'errore deve arrivare al
  chiamante e la connessione deve essere chiusa comunque.
- **26f**: falliscono la query e poi la chiusura dello statement. Deve arrivare l'errore della query, con
  quello della chiusura tra i soppressi, e la connessione deve essere chiusa.

**Difetto trovato.** Il 26e passava, mentre il 26f falliva su tutti e tre i metodi: un'eccezione lanciata
nel `finally` sostituisce quella del blocco `try`, quindi l'errore della query andava perso e al chiamante
arrivava solo quello della chiusura (commit `a851e59`). La connessione invece veniva chiusa. La correzione
(commit `d0bb2d7`) sostituisce in `addProduct`, `deleteProduct` e `getProducts` le chiusure scritte a mano
con try-with-resources, come in `confermaOrdine`: le risorse vengono chiuse comunque e l'errore della
chiusura finisce tra i soppressi.

**Effetto.**

| `CartModel` | Tappa 4 | Tappa 5 |
|---|---|---|
| Mutanti | 97 | 51 |
| Uccisi / senza copertura / sopravvissuti | 72 / 25 / 0 | 50 / 1 / 0 |
| Mutation score | 74% | 98% |
| JaCoCo: righe / branch | 95,8% / 100% | 94,8% / 100% |

- Spariscono 46 mutanti: i 24 senza copertura e 22 uccisi, tutti sulle chiusure scritte a mano. PIT e
  JaCoCo non misurano il codice di chiusura che il compilatore genera per try-with-resources, perché non
  l'ha scritto il programmatore. Resta senza copertura solo `addSuppressed` in `annulla`, che richiede un
  rollback fallito.
- La previsione fatta prima di scrivere i test (4 test su `addProduct` e `deleteProduct`, `CartModel`
  all'82%) non si è realizzata: i test hanno trovato un difetto, e la correzione ha cambiato il codice
  misurato.
- Lo score complessivo sale dall'84% (454/543) all'87% (432/497) e il margine sulla soglia passa da 22 a
  36 mutanti. Anche qui i mutanti uccisi scendono (da 454 a 432), perché diminuisce il totale. A
  differenza della tappa 4, però, la causa è un difetto trovato da un test.
- Ora ogni classe supera la soglia anche analizzata da sola. La più bassa è `ProductModel` (81%).
- I test 26e e 26f restano come test di regressione: se qualcuno tornasse alle chiusure scritte a mano,
  il 26f lo segnalerebbe.

## Cosa mostrano i numeri

- **La copertura non misura l'efficacia dei test.** Nella tappa 2 la copertura di `Model` ha superato il
  96% con un mutation score del 51%. Nella tappa 3 il mutation score è cresciuto di 23 punti senza che la
  copertura si muovesse.
- **Le due tecniche si completano.** Le tecniche black-box hanno portato i test su tutto il codice; il
  mutation testing ha mostrato quali comportamenti di quel codice non venivano verificati.
- **Il report di PIT orienta il lavoro.** Raggruppare i mutanti sopravvissuti per causa ha indicato pochi
  comportamenti da verificare, ognuno dei quali uccide molti mutanti: 32 casi in più hanno scoperto 133
  mutanti in più.
- **Il mutation score dipende anche dal codice.** Nella tappa 4 lo score è salito di 4 punti senza nessun
  test nuovo, perché sono spariti mutanti che nessun test poteva raggiungere. Il confronto con la tappa 3
  va letto come un cambio del totale, non come test più efficaci: i mutanti uccisi sono scesi da 460 a 454.
- **La soglia vale per l'insieme delle classi analizzate.** Nella tappa 4 PIT passava sul perimetro
  completo, ma un'analisi del solo `CartModel` (74%) falliva, perché le altre classi non compensavano più.
- **I mutanti senza copertura indicano dove cercare.** Nella tappa 5 lo score è salito soprattutto
  perché la correzione ha tolto codice, non per i mutanti uccisi dai test nuovi. Però i test scritti per
  raggiungere le copie dei `finally` hanno trovato un difetto vero, su un percorso che le altre categorie
  non esercitavano.

## Mutanti non uccisi (65: 13 sopravvissuti e 52 senza copertura)

| Gruppo | Stato | Mutanti | Motivo |
|---|---|---|---|
| Copie dei `finally` eseguite solo se fallisce anche la chiusura | senza copertura | 48 | Si raggiungono solo se `close()` lancia un'eccezione: 32 in `ProductModel`, 16 in `UserModel`. |
| Equivalenti | sopravvissuti | 12 | Non cambiano il comportamento osservabile: condizioni che proteggono solo una stampa (7) o un blocco vuoto (2), `setCartID(-1)` uguale al valore di default (2), `hashCode()` che restituisce sempre 0, che rispetta comunque il contratto con `equals()` (1). |
| `rs.close()` in `UserModel.getOrdineById` | sopravvissuto | 1 | Lacuna dei test: il 33i verifica la chiusura di statement e connessioni, non quella del `ResultSet`. |
| Rollback fallito | senza copertura | 2 | `addSuppressed` in `CartModel.annulla` e in `AddUser`: servirebbe un test in cui fallisce anche il rollback. |
| `toString` e `StartoJSON` di `ProductBean` | senza copertura | 2 | Non testati: dei bean si testano solo `equals`, `hashCode` e `isEmpty`. |

I mutanti non uccisi erano 115 nella tappa 3 e 89 nella tappa 4: gli stessi gruppi, più i 26
irraggiungibili di `CartModel` tolti nella tappa 4 e le sue 24 copie dei `finally` sparite nella tappa 5.

## Limiti e possibili passi successivi

- **Margine sulla soglia.** Con 432 mutanti uccisi su 497, PIT fallirebbe solo sotto i 396, cioè perdendo
  più di 36 mutanti uccisi. Il margine si ridurrebbe se nuovo codice entrasse nel perimetro senza test.
- **`rs.close()` in `getOrdineById`.** Aggiungere al test 33i la verifica della chiusura del `ResultSet`
  uccide l'unico mutante non equivalente sopravvissuto.
- **Chiusura dello statement fallita negli altri DAO.** `ProductModel` e `UserModel` chiudono ancora le
  risorse con `finally` annidati scritti a mano, quindi probabilmente hanno lo stesso difetto trovato in
  `CartModel`. I test 26e e 26f, estesi a quei metodi, raggiungerebbero i 48 mutanti del primo gruppo.
  Dalla lettura del codice, nei metodi che chiudono statement e connessione nello stesso `finally`
  (`UserModel`, `RecensioneModel`), un'eccezione in `pstmt.close()` impedirebbe anche la chiusura della
  connessione. Va verificato con un test di questa categoria prima di correggere il codice.
- **Rollback fallito.** Due test (uno per `confermaOrdine`, uno per `AddUser`) verificherebbero che
  l'errore originale arrivi al chiamante con quello del rollback tra i soppressi.

## Come riprodurre le misure

```powershell
.\mvnw.cmd clean verify                                            # test e soglie JaCoCo
start target\site\jacoco\index.html                                # report di copertura
.\mvnw.cmd test-compile org.pitest:pitest-maven:mutationCoverage   # mutation testing (soglia 80%)
start target\pit-reports\index.html                                # report di PIT
```

Su GitHub il mutation testing si avvia a mano dal workflow *Mutation Testing* (`mutation.yml`), che
pubblica il report nell'artefatto `pit-report`. Le misure delle tappe 1 e 2 sono state ottenute con gli
stessi comandi sui commit `94e1cc2` (100 casi) e `74b8558` (179 casi), quelle della tappa 4 sul commit
`ea4bad7` e quelle della tappa 5 sul commit `d0bb2d7`.

Per analizzare una sola classe, per esempio `CartModel`, senza far fallire la build:

```powershell
.\mvnw.cmd test-compile org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=Model.CartModel" "-DtargetTests=Model.CartModelTest" "-Dpit.mutationThreshold=0"
```
