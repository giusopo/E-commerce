# Copertura del codice

JaCoCo misura quali righe e quali rami del codice vengono eseguiti dai test; Codecov conserva i report
di ogni commit e li mostra nella dashboard e nelle pull request. La copertura dice cosa viene eseguito,
non se i test se ne accorgono quando il codice sbaglia: per questo c'è il mutation testing, in [mutation.md](mutation.md).

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
| Classi non testate: servlet `CartControl`, `ProductControl`, `StarControl`, `GetPicturesControl`, `IMGmodel`, `LocalServer`, `UserBean`, `OrdineBean`, `RecensioneBean` | escluse dal piano di test, con il motivo in [testing.md](testing.md) |
| `UserControl`, `AuthFilter` (package `control`) e `AdminControl` | sono testate, ma sono servlet: `control` contiene anche servlet non testate e `UserControl` ha codice morto (branch al 68%); di `AdminControl` si testa solo il controllo del ruolo |

**Pipeline** (`ci.yml`): `jacoco:report`, upload di `jacoco.xml` a Codecov, poi `jacoco:check@check`,
che applica le soglie ai dati dei test appena eseguiti senza rieseguirli. In locale:

```powershell
./mvnw clean verify    # test, report e soglie
start target/site/jacoco/index.html
```

## 2. Analisi

Suite di 211 casi, tutti superati; tutte le soglie sono rispettate.

| | Righe | Branch | Classi |
|---|---|---|---|
| **Perimetro** | **96,0%** (764/796) | **92,9%** (260/280) | 7/7 |
| `Model` (quattro DAO) | 96,3% | 90,9% | 4/4 |
| `Model.Bean` (`CartBean`, `ProductBean`) | 95,8% | 100% | 2/2 |
| `admin` (`AzioniAdminModel`) | 90,3% | **75,0%** | 1/1 |
| Progetto intero (report completo) | 66,9% | 63,6% | 13/19 |

Il divario tra perimetro e progetto viene dalle servlet non testate: `CartControl` e `ProductControl`
da sole hanno 243 righe mai eseguite.

**Classi con la copertura più bassa:** `AzioniAdminModel` (righe 90,3%, branch 75%, esattamente sulla
soglia), `RecensioneModel` (91,4% / 83,3%), `CartModel` (94,4% / 93,3%).

**Codice non coperto nel perimetro:**

| Dove | Cosa manca | Perché |
|---|---|---|
| blocco `static` di ogni DAO | lookup JNDI riuscito | nei test non c'è Tomcat, quindi il lookup fallisce sempre |
| `CartModel.annulla`, `UserModel.AddUser` | `addSuppressed` se fallisce anche il rollback | nessun test fa fallire il rollback |
| `CartModel.addProduct`, `deleteProduct` | `rs != null` vero | `rs` non viene mai assegnato: codice morto |
| `finally` di `ProductModel` e `UserModel.getOrdineById` | `pstmt` o `connection` nulli | statement non creato perché la connessione fallisce: provato solo su alcuni metodi |
| `ProductModel.doRetrieveByCat`, `doRetrieveBySubCat` | ordinamento vuoto | il caso è provato solo su `doRetrieveAll` (15b) |
| `UserModel.AddUser` | utente appena inserito non ritrovato | caso non provato |
| `RecensioneModel` | `connection == null`, 0 righe modificate | `getConnection()` non restituisce `null`; con 0 righe stampa solo un messaggio |
| `AzioniAdminModel.modificaSconto` | `rowsUpdated < 1` | il blocco dell'`if` è vuoto |
| `CartBean.setProducts`, `ProductBean.toString`, `StartoJSON` | metodi mai chiamati | dei bean si testano solo `equals`, `hashCode` e `isEmpty` |

## 3. Piano di miglioramento

1. **Margine sul package `admin`.** Il branch al 75% dipende dall'`if` vuoto di `modificaSconto`:
   gestire l'errore (o togliere il blocco) e testarlo darebbe margine sulla soglia.
2. **Rollback fallito.** Un test per `confermaOrdine` e uno per `AddUser` in cui fallisce anche il
   rollback: l'errore originale deve arrivare al chiamante con quello del rollback tra i soppressi.
3. **Casi mancanti.** Ordinamento vuoto in `doRetrieveByCat` e `doRetrieveBySubCat`; utente non
   ritrovato in `AddUser`; connessione fallita in `getOrdineById`.
4. **Codice morto.** Togliere `rs` da `CartModel` e `emptyUser`/`Validate` da `UserControl`. Poi
   `UserControl` e `AuthFilter` possono entrare nelle soglie, escludendo solo le servlet non testate.
5. **Servlet non testate.** `CartControl` è il prossimo candidato: gestisce il carrello in sessione ed è
   la classe con più righe non eseguite.
