# Copertura del codice

## 1. Configurazione

**JaCoCo 0.8.16** (`jacoco-maven-plugin` nel `pom.xml`), con tre esecuzioni:

| Esecuzione | Fase | Cosa fa |
|---|---|---|
| `prepare-agent` | inizio build | aggancia l'agente di JaCoCo alla JVM dei test |
| `report` | `test` | report HTML, XML e CSV di **tutte** le classi in `target/site/jacoco/` |
| `check` | `verify` | soglie per package, solo sul perimetro: righe ≥ 80%, branch ≥ 75%, classi ≥ 100% |

### Perimetro

Il **perimetro** è l'insieme delle classi su cui si calcolano le soglie e la copertura mostrata da
Codecov. Le classi escluse non entrano nel calcolo: le loro righe non contano né come coperte né come
da coprire, quindi non abbassano la percentuale. La copertura del perimetro misura solo il codice che
abbiamo scelto di testare; quella del progetto intero, con tutte le classi, è nella sezione 2.

Nel perimetro ci sono 7 classi: i DAO `CartModel`, `ProductModel`, `UserModel` e `RecensioneModel`,
`CartBean`, `ProductBean` e `AzioniAdminModel`. Le esclusioni sono le `<excludes>` di `check`, ripetute
nelle voci `ignore` di [codecov.yml](codecov.yml):

| Escluse | Perché |
|---|---|
| Non testate: `StarControl`, `GetPicturesControl`, `IMGmodel`, `LocalServer`, `UserBean`, `OrdineBean`, `RecensioneBean` | scelta del piano di test, motivata in [testing.md](testing.md) |
| Testate: `UserControl`, `AuthFilter`, `CartControl`, `ProductControl`, `AdminControl` | `control` è escluso per intero perché contiene `StarControl`; `UserControl` ha codice morto (branch 68%); di `ProductControl` si testano solo le recensioni, di `AdminControl` solo il ruolo |

**Pipeline** (`ci.yml`): `jacoco:report`, upload di `jacoco.xml` a Codecov, poi `jacoco:check@check`,
senza rieseguire i test. Il **blocco** è la soglia di JaCoCo: sotto soglia la CI fallisce.
Codecov dà solo un **segnale**, senza impedire l'unione: `codecov/project` se la copertura scende
rispetto al commit di base, `codecov/patch` se le righe nuove o modificate sono coperte meno dell'80%
([CICD.md](CICD.md#segnale-e-blocco-sulla-copertura)). In locale:

```powershell
./mvnw clean verify    # test, report e soglie
start target/site/jacoco/index.html
```

## 2. Analisi

Suite di 307 casi, tutti superati: le soglie sono rispettate.

| | Righe | Branch | Classi |
|---|---|---|---|
| **Perimetro** | **96,4%** (610/633) | **96,2%** (152/158) | 7/7 |
| `Model` (quattro DAO) | 96,3% | 94,2% | 4/4 |
| `Model.Bean` (`CartBean`, `ProductBean`) | 98,3% | 100% | 2/2 |
| `admin` (`AzioniAdminModel`) | 90,3% | **75,0%** | 1/1 |
| Progetto intero (tutte le classi) | 74,3% (1056/1421) | 69,6% (351/504) | 15/19 |

Il progetto intero è più basso perché include le classi escluse; le più scoperte sono `ProductControl`
(pagine del catalogo, 81 righe mai eseguite), `AdminControl` (69) e `LocalServer` (54).

**Codice non coperto nel perimetro:**

| Dove | Cosa manca | Perché |
|---|---|---|
| blocco `static` dei DAO | lookup JNDI riuscito | nei test non c'è Tomcat |
| `CartModel.annulla`, `UserModel.AddUser` | `addSuppressed` del rollback | nessun test fa fallire il rollback |
| `doRetrieveByCat`, `doRetrieveBySubCat` | ordinamento vuoto | provato solo su `doRetrieveAll` (15b) |
| `UserModel.AddUser` | utente inserito non ritrovato | caso non provato |
| `RecensioneModel` | 0 righe modificate | c'è solo una stampa |
| `AzioniAdminModel.modificaSconto` | `rowsUpdated < 1` | il blocco dell'`if` è vuoto |
| `ProductBean.StartoJSON` | metodo mai chiamato | lo usa solo `StarControl` |

## 3. Piano di miglioramento suggerito dall'agente (claudeCode Opus5.5)

1. **Margine sul package `admin`.** Il branch al 75% dipende dall'`if` vuoto di `modificaSconto`:
   gestirlo e testarlo dà margine sulla soglia.
2. **Rollback fallito.** Test per `confermaOrdine` e `AddUser`: l'errore originale deve arrivare al
   chiamante, quello del rollback tra i soppressi.
3. **Casi mancanti.** Ordinamento vuoto in `doRetrieveByCat` e `doRetrieveBySubCat`, utente non
   ritrovato in `AddUser`.
4. **Codice morto.** Togliere `emptyUser`/`Validate` da `UserControl`; poi le servlet testate possono
   entrare nel perimetro.
5. **Servlet non testate.** `StarControl` (con zero prodotti risponde `]`, JSON non valido) e le pagine
   del catalogo di `ProductControl`.
