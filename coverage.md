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
| Non testate: `StarControl`, `GetPicturesControl`, `LocalServer`, `UserBean`, `OrdineBean` | scelta del piano di test, motivata in [testing.md](testing.md) |
| Testate: `UserControl`, `AuthFilter`, `CartControl`, `ProductControl`, `Html`, `AdminControl`, `IMGmodel`, `RecensioneBean` | `control` e `GestioneIMG` sono esclusi per intero perché contengono `StarControl` e `GetPicturesControl`; di `ProductControl` si testano recensioni, scheda e parametri della home, di `AdminControl` il ruolo, lo sconto, i controlli del form e la pagina di errore; di `RecensioneBean`, escluso come gli altri bean di soli dati, `equals` e `hashCode` |

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

Suite di 383 casi, tutti superati: le soglie sono rispettate.

| | Righe | Branch | Classi |
|---|---|---|---|
| **Perimetro** | **96,3%** (626/650) | **97,2%** (171/176) | 7/7 |
| `Model` (quattro DAO) | 96,3% | 94,8% | 4/4 |
| `Model.Bean` (`CartBean`, `ProductBean`) | 97,6% | 100% | 2/2 |
| `admin` (`AzioniAdminModel`) | 92,1% | 100% | 1/1 |
| Progetto intero (tutte le classi) | 84,4% (1225/1452) | 83,6% (443/530) | 17/20 |

Il progetto intero è più basso perché include le classi escluse; le più scoperte sono `LocalServer`
(54 righe mai eseguite), `ProductControl` (pagine per categoria, 40) e `AdminControl` (salvataggio delle
immagini e caricamento di prodotti e utenti, 34).

**Codice non coperto nel perimetro:**

| Dove | Cosa manca | Perché |
|---|---|---|
| blocco `static` dei DAO | lookup JNDI riuscito | nei test non c'è Tomcat |
| `CartModel.annulla`, `UserModel.AddUser` | `addSuppressed` del rollback | nessun test fa fallire il rollback |
| `doRetrieveByCat`, `doRetrieveBySubCat` | ordinamento vuoto | provato solo su `doRetrieveAll` (15b) |
| `UserModel.AddUser` | utente inserito non ritrovato | caso non provato |
| `RecensioneModel` | 0 righe modificate | c'è solo una stampa |
| `ProductBean.StartoJSON` | metodo mai chiamato | lo usa solo `StarControl` |
| `ProductBean.toString` | metodo mai chiamato | serve solo per le stampe di debug |

Il branch di `admin` è al 100%: l'`if` vuoto di `modificaSconto` è stato sostituito dall'esito del
metodo (test 35b e 69b-69d), e i rami nuovi di `inserisciProdotto`, sottocategoria vuota e id generato
mancante, sono provati dai test 34d e 34e.

## 3. Piano di miglioramento suggerito dall'agente (claudeCode Opus5.5)

1. **Rollback fallito.** Test per `confermaOrdine` e `AddUser`: l'errore originale deve arrivare al
   chiamante, quello del rollback tra i soppressi.
2. **Casi mancanti.** Ordinamento vuoto in `doRetrieveByCat` e `doRetrieveBySubCat`, utente non
   ritrovato in `AddUser`.
3. **Perimetro.** Il codice morto di `UserControl` è stato tolto (branch dal 68% al 96%), quindi le
   servlet testate potrebbero entrare nel perimetro; servirebbe però escludere per nome `StarControl` e
   `GetPicturesControl` invece dei package `control` e `GestioneIMG`.
4. **Servlet non testate.** `StarControl` e `GetPicturesControl`, verificate solo con l'applicazione
   avviata ([pianoTest.md](pianoTest.md)), e le pagine per categoria di `ProductControl`.
