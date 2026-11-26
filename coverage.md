# Copertura del codice

## 1. Configurazione

**JaCoCo 0.8.16** (`jacoco-maven-plugin` nel `pom.xml`), con tre esecuzioni:

| Esecuzione | Fase | Cosa fa |
|---|---|---|
| `prepare-agent` | inizio build | aggancia l'agente di JaCoCo alla JVM dei test (Surefire usa `@{argLine}`) |
| `report` | `test` | report HTML, XML e CSV in `target/site/jacoco/` |
| `check` | `verify` | soglie per package: righe ≥ 80%, branch ≥ 75%, classi ≥ 100%; sotto soglia la build fallisce |

**Perimetro.** Il report comprende tutte le classi. Soglie e Codecov guardano lo stesso sottoinsieme:
le `<excludes>` di `check` e le voci `ignore` di [codecov.yml](codecov.yml) sono le stesse.

| Fuori dal perimetro | Motivo |
|---|---|
| Classi non testate: servlet `StarControl`, `GetPicturesControl`, `IMGmodel`, `LocalServer`, `UserBean`, `OrdineBean`, `RecensioneBean` | escluse dal piano di test, con il motivo in [testing.md](testing.md) |
| `UserControl`, `AuthFilter`, `CartControl`, `ProductControl` (package `control`) e `AdminControl` | sono testate, ma sono servlet: `control` contiene anche `StarControl`, non testata, `UserControl` ha codice morto (branch al 68%) e di `ProductControl` si testano solo le recensioni; di `AdminControl` si testa solo il controllo del ruolo |

**Pipeline** (`ci.yml`): `jacoco:report`, upload di `jacoco.xml` a Codecov, poi `jacoco:check@check`,
che applica le soglie ai dati dei test appena eseguiti senza rieseguirli.

**Segnale e blocco.** Codecov segnala i peggioramenti con i controlli `codecov/project` (copertura
scesa rispetto al commit di base) e `codecov/patch` (righe nuove o modificate coperte meno dell'80%),
senza impedire l'unione. Il blocco lo dà la soglia di JaCoCo: sotto soglia la CI fallisce. Dettagli
in [CICD.md](CICD.md#segnale-e-blocco-sulla-copertura). In locale:

```powershell
./mvnw clean verify    # test, report e soglie
start target/site/jacoco/index.html
```

## 2. Analisi

Suite di 261 casi, tutti superati; tutte le soglie sono rispettate.

| | Righe | Branch | Classi |
|---|---|---|---|
| **Perimetro** | **96,6%** (763/790) | **93,5%** (258/276) | 7/7 |
| `Model` (quattro DAO) | 96,6% | 91,7% | 4/4 |
| `Model.Bean` (`CartBean`, `ProductBean`) | 98,3% | 100% | 2/2 |
| `admin` (`AzioniAdminModel`) | 90,3% | **75,0%** | 1/1 |
| Progetto intero (report completo) | 76,6% | 73,5% | 15/19 |

Prima di togliere da `CartModel` la variabile `rs`, mai usata, il perimetro era al 96,4% delle righe e al
92,9% dei branch: i due branch `rs != null` di `addProduct` e `deleteProduct` non si potevano coprire.

Il divario tra perimetro e progetto viene dalle classi fuori dal perimetro: le più scoperte sono
`ProductControl` (pagine del catalogo, 81 righe mai eseguite), `AdminControl` (69) e `LocalServer` (54).

**Classi con la copertura più bassa:** `AzioniAdminModel` (righe 90,3%, branch 75%, esattamente sulla
soglia), `RecensioneModel` (91,4% / 83,3%), e per i branch `ProductModel` (98,6% / 91,1%). `CartModel`
è al 95,8% delle righe e al 100% dei branch.

**Codice non coperto nel perimetro:**

| Dove | Cosa manca | Perché |
|---|---|---|
| blocco `static` di ogni DAO | lookup JNDI riuscito | nei test non c'è Tomcat, quindi il lookup fallisce sempre |
| `CartModel.annulla`, `UserModel.AddUser` | `addSuppressed` se fallisce anche il rollback | nessun test fa fallire il rollback |
| `finally` di `ProductModel` e `UserModel.getOrdineById` | `pstmt` o `connection` nulli | statement non creato perché la connessione fallisce: provato solo su alcuni metodi |
| `ProductModel.doRetrieveByCat`, `doRetrieveBySubCat` | ordinamento vuoto | il caso è provato solo su `doRetrieveAll` (15b) |
| `UserModel.AddUser` | utente appena inserito non ritrovato | caso non provato |
| `RecensioneModel` | `connection == null`, 0 righe modificate | `getConnection()` non restituisce `null`; con 0 righe stampa solo un messaggio |
| `AzioniAdminModel.modificaSconto` | `rowsUpdated < 1` | il blocco dell'`if` è vuoto |
| `ProductBean.StartoJSON` | metodo mai chiamato | lo usa solo `StarControl`, non testata |

## 3. Piano di miglioramento suggerito dall'agente (claudeCode Opus5.5)

1. **Margine sul package `admin`.** Il branch al 75% dipende dall'`if` vuoto di `modificaSconto`:
   gestire l'errore (o togliere il blocco) e testarlo darebbe margine sulla soglia.
2. **Rollback fallito.** Un test per `confermaOrdine` e uno per `AddUser` in cui fallisce anche il
   rollback: l'errore originale deve arrivare al chiamante con quello del rollback tra i soppressi.
3. **Casi mancanti.** Ordinamento vuoto in `doRetrieveByCat` e `doRetrieveBySubCat`; utente non
   ritrovato in `AddUser`; connessione fallita in `getOrdineById`.
4. **Codice morto.** Togliere `emptyUser`/`Validate` da `UserControl`, come già fatto con `rs` in
   `CartModel`. Poi le servlet testate possono entrare nelle soglie, escludendo solo quelle non testate.
5. **Servlet non testate.** `StarControl`, che con zero prodotti risponde `]` (JSON non valido), e le
   pagine del catalogo di `ProductControl`, la classe con più righe non eseguite.
