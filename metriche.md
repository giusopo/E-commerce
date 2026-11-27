# Evoluzione di copertura e mutation score

Questo documento mostra come sono cambiate le metriche di qualità dei test nelle sette tappe del
lavoro, e quali modifiche hanno prodotto ogni variazione. Nelle prime tre cambia la suite di test;
nella quarta cambia il codice, senza nuovi test; nella quinta un test nuovo trova un difetto e la
correzione cambia il codice; nella sesta la stessa categoria di test, estesa agli altri DAO, trova altri
difetti; nella settima i difetti li trova la ricostruzione delle specifiche, e i test nuovi li fissano. Il
dettaglio dei singoli test è in [pianoTest.md](pianoTest.md).

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
e i mutanti scendono a 543 e poi a 497; nella tappa 6 cambia il codice degli altri DAO e scendono a 326;
nella tappa 7 cambiano `UserModel` e `AzioniAdminModel` e salgono a 330.

## Riepilogo

| | Tappa 1: 100 casi | Tappa 2: 179 casi | Tappa 3: 211 casi | Tappa 4: 211 casi, senza `rs` | Tappa 5: 267 casi, try-with-resources | Tappa 6: 307 casi, try-with-resources in tutti i DAO | Tappa 7: 314 casi, difetti dalle specifiche |
|---|---|---|---|---|---|---|---|
| JaCoCo `Model`: righe / branch | 69,5% / 47,1% | 96,3% / 90,4% | 96,3% / 90,9% | 96,6% / 91,7% | 96,4% / 91,1% | 96,3% / 94,2% | 96,3% / 94,2% |
| JaCoCo `Model.Bean`: righe / branch | 89,2% / 64,7% | 95,0% / 100% | 95,8% / 100% | 95,8% / 100% | 98,3% / 100% | 98,3% / 100% | 98,3% / 100% |
| JaCoCo `admin`: righe / branch | 90,3% / 75,0% | 90,3% / 75,0% | 90,3% / 75,0% | 90,3% / 75,0% | 90,3% / 75,0% | 90,3% / 75,0% | 89,7% / 100% |
| `./mvnw verify` (soglie JaCoCo) | fallisce | passa | passa | passa | passa | passa | passa |
| PIT: line coverage | 71% (616/865) | 94% (817/865) | 95% (822/865) | 95% (816/855) | 96% (792/827) | 97% (662/683) | 97% (662/683) |
| PIT: mutanti senza copertura | 248 | 123 | 102 | 76 | 52 | 4 | 4 |
| **PIT: mutation score** | **39%** (225/575) | **57%** (327/575) | **80%** (460/575) | **84%** (454/543) | **87%** (432/497) | **95%** (310/326) | **96%** (316/330) |
| PIT: test strength | 69% | 72% | 97% | 97% | 97% | 96% | 97% |

Mutation score per classe (`CartModel` ha 129 mutanti fino alla tappa 3, 97 nella tappa 4 e 51 dalla 5;
`ProductModel`, `UserModel` e `RecensioneModel` cambiano totale nella tappa 6; `UserModel` e
`AzioniAdminModel` nella tappa 7):

| Classe | Mutanti | Tappa 1 | Tappa 2 | Tappa 3 | Tappa 4 | Tappa 5 | Tappa 6 | Tappa 7 |
|---|---|---|---|---|---|---|---|---|
| `ProductModel` | 170 → 75 | 26% | 48% | 81% | 81% | 81% | **99%** | 99% |
| `UserModel` | 143 → 87 → 89 | 24% | 56% | 86% | 86% | 86% | **97%** | 97% |
| `CartModel` | 129 → 97 → 51 | 36% | 41% | 60% | 74% | **98%** | 98% | 98% |
| `RecensioneModel` | 53 → 33 | 58% | 74% | 89% | 89% | 89% | 82% | 82% |
| `ProductBean` | 45 | 80% | 91% | 93% | 93% | 93% | 93% | 93% |
| `AzioniAdminModel` | 18 → 20 | 89% | 89% | 89% | 89% | 89% | 89% | **100%** |
| `CartBean` | 17 | 100% | 100% | 100% | 100% | 100% | 100% | 100% |

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

## Tappa 6: chiusura dello statement fallita negli altri DAO (307 casi)

I 48 mutanti senza copertura rimasti nelle copie dei `finally` erano in `ProductModel` (32) e `UserModel`
(16). La scelta **chiusura dello statement fallita** della tappa 5 è stata estesa a tutti i metodi di
`ProductModel`, `UserModel` e `RecensioneModel` che chiudevano le risorse a mano (40 casi: 17e-17h,
33k-33n, 38g-38h). Come per `CartModel` si provano la query riuscita e la query fallita; nei metodi con
due query anche la chiusura dello statement della seconda (17g, 17h, 33m, 33n).

**Difetti trovati.** Sul codice di allora 31 dei 40 casi fallivano (commit `6a8b649`), per due difetti:

- in `ProductModel` e nella seconda query di `getOrdineById`, che chiudevano con `finally` annidati, lo
  stesso difetto di `CartModel`: se dopo la query falliva anche la chiusura, l'errore della query andava
  perso (17f, 17h, 33n);
- negli altri metodi di `UserModel` e in `RecensioneModel` statement e connessione si chiudevano nello
  stesso `finally`: se `pstmt.close()` falliva, la connessione restava aperta (33k, 33l, 38g, 38h).
  Ripetuto nel tempo esaurisce il pool di connessioni, come il difetto del test 36.

Passavano solo i casi con la query riuscita nei metodi con `finally` annidati (17e, 17g, 33m).

**Correzione** (commit `4b9d40b`). I tre DAO usano try-with-resources per connessione, statement e
`ResultSet`, che diventano variabili locali. Prima connessione e statement erano campi di oggetti condivisi
da tutte le richieste, quindi due richieste contemporanee potevano sovrascriversi le risorse: nessun test lo
verifica, ma con la correzione il problema non può più presentarsi. Inoltre la query delle votazioni,
copiata in tre metodi di `ProductModel`, è in un metodo a parte; in `RecensioneModel` spariscono i
controlli `connection == null`, sempre falsi; in `getIdCart` e `getIdCartByUserId` il `return` è dopo il
blocco, perché con un `return` all'interno il compilatore genera branch che JaCoCo non esclude.

**Effetto.**

| | Tappa 5 | Tappa 6 |
|---|---|---|
| Mutanti | 497 | 326 |
| Uccisi / senza copertura / sopravvissuti | 432 / 52 / 13 | 310 / 4 / 12 |
| Mutation score | 87% | 95% |
| JaCoCo `Model`: righe / branch | 96,4% / 91,1% | 96,3% / 94,2% |

- Il totale scende di 171 mutanti: spariscono i 48 senza copertura, il sopravvissuto su `rs.close()` in
  `getOrdineById` e, al netto, 122 uccisi, che erano sulle chiusure scritte a mano, sui controlli
  `connection == null` e sulle copie della query delle votazioni. Come nella tappa 5, lo score sale
  soprattutto perché cambia il totale, ma la causa sono difetti trovati dai test.
- Il margine sulla soglia passa da 36 a 49 mutanti uccisi.
- `RecensioneModel` diventa la classe più bassa (82%): i suoi 6 sopravvissuti equivalenti pesano di più
  su 33 mutanti che su 53.
- In JaCoCo i branch di `Model` salgono dal 91,1% al 94,2%, perché spariscono i controlli `!= null` dei
  `finally`. Le righe del progetto intero scendono dal 76,3% al 74,3%: le chiusure tolte erano righe
  coperte, quindi le classi fuori dal perimetro pesano di più.

## Tappa 7: difetti trovati ricostruendo le specifiche (314 casi)

Il progetto non aveva documentazione, e le specifiche sono state ricostruite dal codice, dallo schema del
database, dalle pagine e dal piano dei test ([specifiche.md](specifiche.md)). Il confronto tra le
specifiche e i commenti del codice (sezione 12 di quel documento) ha fatto emergere difetti che nessun test
cercava:

- `getUserTramiteID` e `getUtenti` non leggevano la data di nascita: nell'elenco dell'amministratore tutti
  gli utenti risultavano nati l'1/1/1970. I test 33b e 33d passavano perché non controllavano la data.
- `modificaSconto` aveva un `if` vuoto con il commento "deve portare ad una pagina di errore": uno sconto
  non salvato, per un prodotto inesistente o per un errore del database, veniva mostrato come salvato.
- Per un'azione mancante o sconosciuta `AdminControl` reindirizzava a `/error.jsp`, una pagina che non
  esiste.

**Test.** I test 33b e 33d controllano anche la data di nascita; sono nuovi il 35b (valori limite 1 e 0
righe modificate), il 69b e il 69c (sconto non salvato) e il 69d (azione mancante, vuota o sconosciuta):
7 casi in più. Sul codice di prima falliscono il 33b, il 33d, il 69b, il 69c e i tre casi del 69d; il 35b
non compila nemmeno, perché `modificaSconto` non restituiva niente.

**Correzione.** `UserModel` legge `DATA_NASCITA`; `modificaSconto` restituisce `true` solo se ha modificato
una riga, come `inserisciProdotto`, e `AdminControl` mostra la pagina di errore quando lo sconto non è
salvato; i redirect puntano a `Error.jsp`. Nella stessa tappa sono cambiati, senza effetti sulle metriche,
alcuni commenti superati, i nomi di alcuni parametri, lo schema (username `UNIQUE`) e il JavaScript delle
recensioni.

**Effetto.**

| | Tappa 6 | Tappa 7 |
|---|---|---|
| Mutanti | 326 | 330 |
| Uccisi / senza copertura / sopravvissuti | 310 / 4 / 12 | 316 / 4 / 10 |
| Mutation score | 95% | 96% |
| JaCoCo `admin`: righe / branch | 90,3% / 75,0% | 89,7% / 100% |

- In `UserModel` si aggiungono 2 mutanti, le letture della data, uccisi dai test 33b e 33d. In
  `modificaSconto` i 2 mutanti della condizione, prima equivalenti perché il blocco era vuoto, ora vengono
  uccisi, e se ne aggiungono 2 sul valore restituito: `AzioniAdminModel` passa dall'89% al 100%.
- Questa volta lo score sale perché aumentano i mutanti uccisi (da 310 a 316), non perché diminuisce il
  totale. Il margine sulla soglia passa da 49 a 52 mutanti uccisi.
- In JaCoCo il branch di `admin`, che era esattamente sulla soglia del 75%, arriva al 100%; le righe
  scendono di poco perché le righe tolte erano coperte.

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
  non esercitavano. Nella tappa 6 la stessa categoria ha trovato difetti in tutti gli altri DAO.
- **Un difetto di omissione non si vede né con la copertura né con il mutation testing.** Nella tappa 7
  mancava una riga (la lettura della data di nascita) e un comportamento (la pagina di errore dopo
  l'`if` vuoto): dove il codice non c'è, non c'è niente da eseguire né da mutare. I due mutanti dell'`if`
  sembravano equivalenti, e lo erano rispetto al codice, non rispetto a ciò che il codice doveva fare. Li
  ha fatti emergere solo il confronto con una specifica scritta.

## Mutanti non uccisi (14: 10 sopravvissuti e 4 senza copertura)

| Gruppo | Stato | Mutanti | Motivo |
|---|---|---|---|
| Equivalenti | sopravvissuti | 10 | Non cambiano il comportamento osservabile: condizioni che proteggono solo una stampa (7), `setCartID(-1)` uguale al valore di default (2), `hashCode()` che restituisce sempre 0, che rispetta comunque il contratto con `equals()` (1). |
| Rollback fallito | senza copertura | 2 | `addSuppressed` in `CartModel.annulla` e in `AddUser`: servirebbe un test in cui fallisce anche il rollback. |
| `toString` e `StartoJSON` di `ProductBean` | senza copertura | 2 | Non testati: dei bean si testano solo `equals`, `hashCode` e `isEmpty`. |

I mutanti non uccisi erano 115 nella tappa 3, 89 nella 4 e 65 nella 5: gli stessi gruppi, più i 26
irraggiungibili di `CartModel` tolti nella tappa 4, le sue 24 copie dei `finally` sparite nella tappa 5 e,
nella tappa 6, le 48 copie dei `finally` di `ProductModel` e `UserModel` e il sopravvissuto su `rs.close()`
in `getOrdineById`. Nella tappa 7 spariscono i 2 equivalenti dell'`if` vuoto di `modificaSconto`.

## Limiti e possibili passi successivi

- **Margine sulla soglia.** Con 316 mutanti uccisi su 330, PIT fallirebbe solo sotto i 264, cioè perdendo
  più di 52 mutanti uccisi. Il margine si ridurrebbe se nuovo codice entrasse nel perimetro senza test.
- **Equivalenti.** Togliere i controlli che proteggono solo una stampa (6 in `RecensioneModel`, ora la
  classe più bassa) eliminerebbe mutanti che nessun test può uccidere.
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
`ea4bad7`, quelle della tappa 5 sul commit `d0bb2d7`, quelle della tappa 6 sul commit `4b9d40b` e quelle
della tappa 7 il 27/11/2026, sul codice con le correzioni descritte in [specifiche.md](specifiche.md).

Per analizzare una sola classe, per esempio `CartModel`, senza far fallire la build:

```powershell
.\mvnw.cmd test-compile org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=Model.CartModel" "-DtargetTests=Model.CartModelTest" "-Dpit.mutationThreshold=0"
```
