# Design di BeautyBlind

Design del sistema ricostruito dagli artefatti del repository, come base per le specifiche JML del
[Lab 5](https://github.com/dardin88/swd-labs/blob/main/Lab5_webapps_jml.md). Descrive il codice dopo le
correzioni della tappa 8 di [metriche.md](metriche.md) (27/11/2026).

## 1. Scopo, metodo e fonti

### 1.1 Scopo

BeautyBlind non aveva una documentazione di progetto: l'unica specifica era quella indiretta dei test
([pianoTest.md](pianoTest.md)). Questo documento descrive architettura, dati, stato e contratti dei
componenti come risultano dagli artefatti, per ricavarne precondizioni, postcondizioni e invarianti.

Ricostruendo il design sono emerse divergenze tra codice, schema, pagine e test. Sono state corrette, con un
test per ogni correzione raggiungibile dai test di unità: l'elenco è nella tabella "Difetti trovati dai
test" di [pianoTest.md](pianoTest.md) e nella tappa 8 di [metriche.md](metriche.md). Le sezioni seguenti
descrivono il sistema corretto; la sezione 10 elenca i limiti che restano.

### 1.2 Metodo

- **Design recovery**: astrazioni di progetto ricavate dal codice e dalla documentazione esistente
  [S1, slide 38]. Il codice è l'unica rappresentazione affidabile del sistema, quindi è la fonte
  principale [S2, §4.2.2].
- Analisi **in-the-small** (responsabilità, interfaccia e dipendenze di ogni classe) e **in-the-large**
  (flussi delle funzionalità, dalla pagina al database) [S2, Annex B.3].
- **Data reverse engineering** dello schema e del codice che lo usa [S1, slide 64–70].
- Analisi **statica** manuale di Java, JSP, JavaScript, configurazione e schema; analisi **dinamica** con
  la suite di test (`./mvnw verify`: 383 casi, tutti superati) e con l'applicazione avviata su un database
  MariaDB 11.4.13 di prova, inizializzato con `ecommerce.sql`.
- Gli identificativi [Sx] sono le fonti del corso elencate in
  [reverse_engineering_fonti_ISTA.md](reverse_engineering_fonti_ISTA.md).

### 1.3 Fonti e convenzioni

| Artefatto | Dove | Cosa dice |
|---|---|---|
| Codice Java: 20 classi e 5 interfacce | `src/main/java` | cosa fa il sistema |
| Pagine e script: 14 JSP, 2 frammenti HTML, 5 script | `src/main/webapp` | come le pagine chiamano le servlet; controlli nel browser |
| Configurazione | [web.xml](src/main/webapp/WEB-INF/web.xml), [context.xml](src/main/webapp/META-INF/context.xml) | URL, filtro, pagine di errore, pool di connessioni |
| Schema e dati iniziali | [ecommerce.sql](src/main/db/ecommerce.sql), [aggiornamento-schema.sql](src/main/db/aggiornamento-schema.sql) | vincoli garantiti dal database |
| Test: 383 casi | `src/test/java`, [pianoTest.md](pianoTest.md) | comportamento atteso e verificato |
| Documenti | [README](README.md), [testing](testing.md), [coverage](coverage.md), [mutation](mutation.md), [metriche](metriche.md), [CICD](CICD.md), [Docker](Docker.md), [esecuzioneLocaleVS](esecuzioneLocaleVS.md) | ambiente, scelte di test, difetti corretti |

- **Artefatti in disaccordo.** Il codice dice cosa il sistema fa, i test cosa è stato verificato come
  atteso, lo schema quali vincoli garantisce il database, le pagine come il sistema viene usato. Una
  regola è descritta come contratto quando codice e test concordano.
- **Colonna "Verifica".** Riporta i numeri del piano dei test che verificano la regola; *codice* indica
  una regola letta solo nel codice, senza test di unità; *schema* un vincolo del database; *prova* un
  comportamento verificato con l'applicazione avviata.
- **Identificativi.** R regole di dominio (sezione 6), I invarianti (sezioni 4 e 5), C contratti comuni
  dei DAO (sezione 7.3), L limiti noti (sezione 10).
- **Riferimenti al codice.** Classi e metodi sono citati per nome e non per numero di riga: le
  annotazioni JML sposteranno le righe.

## 2. Il sistema

BeautyBlind è un e-commerce di cosmetici. Funzionalità:

- catalogo dei prodotti con immagini: paginato, completo o filtrato per categoria e sottocategoria;
- ricerca per nome, con suggerimenti mentre si scrive;
- scheda del prodotto con le recensioni;
- carrello: nella sessione per l'ospite, nel database per l'utente registrato;
- registrazione, login, area utente e logout;
- conferma dell'ordine con controllo della giacenza, fattura e storico degli ordini;
- area dell'amministratore: inserimento di un prodotto con due immagini, sconto su un prodotto, elenco
  degli utenti;
- pagina dei contatti, che apre il client di posta del visitatore, senza logica sul server.

| Attore | Come viene riconosciuto | Cosa può fare |
|---|---|---|
| Ospite | sessione senza `userID` | catalogo, ricerca, scheda, carrello in sessione, registrazione, login |
| Utente registrato | `userID` e `username` in sessione | come l'ospite, più carrello nel database, ordini e storico, recensioni, area utente |
| Amministratore | `adminRoles` uguale a `Boolean.TRUE` in sessione, cioè `utente.permessi_admin` = 1 | come l'utente registrato, più le pagine `/admin/*` e la servlet `/Admin` |

## 3. Architettura

### 3.1 Stile e decomponibilità

Applicazione web Java nello stile **Model 2** (MVC a livelli): servlet come controller, JSP come viste,
DAO JDBC e bean come modello, database relazionale raggiunto con un `DataSource` JNDI gestito da Tomcat.

Rispetto alla decomponibilità [S13, slide 7–8], interfaccia (JSP e JavaScript), controllo (servlet) e
accesso ai dati (DAO) sono componenti separati, ma la logica applicativa non ha un livello proprio:

- nelle servlet ci sono la validazione dei dati, la gestione del carrello e il controllo dei ruoli;
- nei DAO il calcolo del prezzo fatturato e del totale, il controllo della giacenza e le transazioni;
- nelle JSP la formula del prezzo scontato, ripetuta in tre pagine (R01).

Non esistono classi di servizio. La sezione 11 mostra cosa comporta per il Lab 5.

### 3.2 Package

| Package | Classi | Ruolo | Righe |
|---|---|---|---|
| `control` | `ProductControl`, `CartControl`, `UserControl`, `StarControl`, `AuthFilter`, `Html` | servlet di catalogo e recensioni, carrello e ordini, utenti, votazioni in JSON; filtro dell'area admin; escape HTML | 970 |
| `admin` | `AdminControl`, `AzioniAdminModel`, `AzioniAdminModel_intf` | servlet delle azioni dell'admin e relativo DAO | 327 |
| `GestioneIMG` | `GetPicturesControl`, `IMGmodel` | servlet delle immagini e DAO statico della tabella `immagine` | 142 |
| `Model` | `CartModel`, `ProductModel`, `UserModel`, `RecensioneModel` | DAO di carrello e ordini, catalogo, utenti, recensioni | 1078 |
| `Model.Interface` | `CartModel_intf`, `ProductModel_intf`, `UserModel_intf`, `RecensioneModel_intf` | interfacce dei DAO | 85 |
| `Model.Bean` | `CartBean`, `ProductBean`, `UserBean`, `OrdineBean`, `RecensioneBean` | oggetti del dominio; `CartBean` è il carrello in sessione | 628 |
| `dev` | `LocalServer` | Tomcat embedded per lo sviluppo, escluso dal WAR | 110 |

Le righe sono quelle dei file (`wc -l`), commenti compresi. I package seguono due criteri: `control` e
`Model` raggruppano per livello, `admin` e `GestioneIMG` per funzionalità, e contengono sia la servlet
sia il DAO.

### 3.3 Dipendenze

| Componente | Usa |
|---|---|
| `ProductControl` | `ProductModel_intf`, `RecensioneModel_intf`, `Html` |
| `CartControl` | `ProductModel_intf`, `CartModel_intf`, `UserModel_intf`, `CartBean` |
| `UserControl` | `UserModel` e `CartModel` come classi concrete (usa anche `getUserTramiteID`, che non è in `UserModel_intf`), `CartBean` |
| `AdminControl` | `AzioniAdminModel_intf`, `UserModel_intf`, `ProductModel_intf`, `IMGmodel` |
| `StarControl` | `ProductModel` come classe concreta |
| `GetPicturesControl` | `IMGmodel` |
| `AuthFilter` | solo la sessione HTTP |
| JSP che mostrano testo di utenti o prodotti | `Html` |
| DAO di `Model`, `AzioniAdminModel` e `IMGmodel` | `DataSource` JNDI `java:comp/env/jdbc/ecommerce`; i DAO di `Model` e `AzioniAdminModel` anche i bean |

Le servlet creano i loro DAO in campi statici (`StarControl` in un campo d'istanza); `IMGmodel` ha solo
metodi statici. I DAO sono condivisi da tutte le richieste e non hanno stato: connessione, statement e
`ResultSet` sono variabili locali dei try-with-resources. I test sostituiscono i campi delle servlet con
dei mock e il `DataSource` dei DAO con la riflessione (`testutil.JdbcMocks`).

### 3.4 URL

| URL | Componente | Metodi HTTP | Configurato in |
|---|---|---|---|
| `/` | `ProductControl` (servlet `Home`, url-pattern vuoto) | GET: catalogo, scheda, ricerca; POST: recensioni | `web.xml` |
| `/User` | `UserControl` | GET e POST, stesse azioni | `web.xml` |
| `/Cart` | `CartControl` | GET e POST, stesse azioni | `web.xml` |
| `/Admin` | `AdminControl` (accetta form multipart) | GET e POST, stesse azioni | `web.xml` |
| `/StarControl` | `StarControl` | GET e POST | `@WebServlet` |
| `/getPicture` | `GetPicturesControl` | GET e POST | `@WebServlet` |
| `/admin/*`, `/Admin` | `AuthFilter` | tutti | `web.xml` |
| pagine di errore | eccezioni e pagina predefinita: `Error.jsp`; 404: `Error404.jsp`; 500: `Error500.jsp` | | `web.xml` |

### 3.5 Ambiente

| Aspetto | Valore | Fonte |
|---|---|---|
| Container | Tomcat 9 (`javax.servlet`): 9.0.123 embedded in sviluppo, 9.0.124 nell'immagine Docker | `pom.xml`, `Dockerfile` |
| Java | codice compilato per Java 11, test per Java 21 | `pom.xml` |
| Database | MariaDB 11.4.13 in Docker e nello sviluppo locale; il README indica MySQL/MariaDB | `docker-compose.yml`, README |
| Driver | MySQL Connector/J 8.4.0 | `pom.xml` |
| Pool | risorsa JNDI `jdbc/ecommerce` (DBCP2): al più 20 connessioni, attesa massima 5 s, validazione con `SELECT 1` | `context.xml` |
| Deploy | `ROOT.war` alla radice `/`, perché i collegamenti usano percorsi assoluti | `pom.xml`, `Dockerfile` |
| Utente del database | `beautyblind_app`, con i soli privilegi `SELECT`, `INSERT`, `UPDATE`, `DELETE` | `docker/db/02-privileges.sql`, esecuzioneLocaleVS |

Un database creato con lo schema precedente non reimporta il dump: va aggiornato una volta con
`aggiornamento-schema.sql`, come spiegato in [Docker.md](Docker.md) ed
[esecuzioneLocaleVS.md](esecuzioneLocaleVS.md).

## 4. Dati

### 4.1 Schema

Tabelle InnoDB in `utf8mb4` con collation `utf8mb4_general_ci`, che confronta le stringhe senza
distinguere le maiuscole. "A cascata" indica una chiave esterna con `ON DELETE CASCADE ON UPDATE CASCADE`.

| Colonna | Tipo | Vincoli | Campo del bean |
|---|---|---|---|
| **`utente`** | | | **`UserBean`** |
| `ID` | int | PK, auto-increment | `ID` |
| `NOME`, `COGNOME` | varchar(100) | NOT NULL | `Nome`, `Cognome` |
| `USER_NAME` | varchar(50) | NOT NULL, UNIQUE | `UserName` |
| `DATA_NASCITA` | date | NOT NULL | `DataNascita` |
| `email` | varchar(100) | NOT NULL, non unica | `email` |
| `password` | varchar(100) | NOT NULL; contiene l'hash SHA-256 (64 cifre esadecimali) | `password` |
| `permessi_admin` | tinyint(1) | NOT NULL, default 0 | `permessiAdmin` |
| `indirizzoBase` | varchar(50) | NOT NULL | `indirizzoBase` |
| `telefono` | varchar(15) | NOT NULL | `numeroDiTelefono` (`getPhoneNumber`) |
| **`ordine`** (carrello oppure ordine) | | UNIQUE (`UTENTE_ID`, `carrello`); CHECK `carrello_coerente` | **`OrdineBean`** |
| `ID` | int | PK, auto-increment | `idOrdine`; per il carrello, `cartID` |
| `prezzo_tot` | decimal(10,2) | NULL ammesso | `prezzoTot` |
| `percentuale_IVA` | decimal(10,0) | NULL ammesso | `percentualeIva` |
| `data_inoltro` | date | NULL ammesso | `dataInoltro` (letta come stringa) |
| `indirizzo_consegna` | varchar(100) | NULL ammesso | `indirizzoConsegna` |
| `stato` | varchar(20) | NOT NULL, default `'in corso'` | — |
| `carrello` | tinyint(1) | default 1; vale 1 se e solo se `stato` è `'in corso'`, altrimenti NULL (CHECK) | — |
| `UTENTE_ID` | int | FK → `utente`, a cascata; NULL ammesso | — |
| **`contiene`** (righe di carrelli e ordini) | | PK (`ID_ordine`, `ID_prodotto`) | **`ProductBean`** |
| `ID_ordine` | int | FK → `ordine`, a cascata | — |
| `ID_prodotto` | int | FK → `prodotto`, a cascata | `ID` |
| `QTAordinata` | int | NOT NULL, default 1 | `QTAordinata` |
| `prezzo_all_acquisto` | double | NOT NULL, default 0 | `prezzoAllAcquisto` |
| **`prodotto`** | | | **`ProductBean`** |
| `ID` | int | PK, auto-increment | `ID` |
| `nome` | varchar(100) | NOT NULL | `nome` |
| `descrizione` | text | NOT NULL | `descrizione` |
| `prezzo` | decimal(10,2) | NOT NULL | `prezzo` |
| `grammatura` | decimal(10,1) | NOT NULL | `grammatura` |
| `unita` | varchar(2) | NOT NULL, default `'g'` | `unita` |
| `categoria` | varchar(100) | NOT NULL | `categoria` |
| `subCategoria` | varchar(100) | NULL ammesso | `subCategoria` |
| `QTAmagazzino` | int | NOT NULL | `QTAmagazzino` |
| `percentualeSconto` | decimal(10,0) | NOT NULL; percentuale intera | `percentualeSconto` |
| `colorazione` | varchar(20) | NOT NULL | `colorazione` |
| `DataInserimento` | date | NOT NULL, default 2024-05-02 | `DataInserimento` |
| **`immagine`** | | | nessun bean |
| `ID` | int | PK, auto-increment | |
| `IMMAGINE` | longblob | NOT NULL | |
| `ruolo` | varchar(20) | NOT NULL, default `'principale'`; valori usati `'principale'` e `'secondaria'` | |
| `PRODOTTO_ID` | int | FK → `prodotto`, a cascata; NULL ammesso | |
| **`recensione`** | | UNIQUE (`PRODOTTO_ID`, `userID`) | **`RecensioneBean`** |
| `ID` | int | PK, auto-increment | `ID` |
| `data_inserimento` | date | NOT NULL | `dataInserimento` |
| `votazione` | int | NOT NULL | `votazione` |
| `commento` | text | NOT NULL | `commento` |
| `PRODOTTO_ID` | int | FK → `prodotto`, a cascata; NULL ammesso | `PRODOTTO_ID` |
| `userID` | int | FK → `utente`, senza cascata; NULL ammesso | `userID` |

**Dati iniziali** del dump: 30 prodotti, tra cui un prodotto di prova (ID 99, categoria `viso`, senza
sottocategoria); 59 immagini (ogni prodotto ha la principale, manca solo la secondaria del prodotto 35);
3 utenti, di cui un amministratore; 5 ordini, cioè 3 carrelli e 2 ordini conclusi; nessuna recensione.

### 4.2 Modello concettuale

- **Il carrello è un ordine.** Non esiste una tabella del carrello: il carrello di un utente è la riga di
  `ordine` con `stato = 'in corso'` (e `carrello = 1`), e i suoi prodotti sono le righe di `contiene`. Il
  suo ID è il `cartID` della sessione e il parametro `idCarrello` di `CartModel`.
- **Ciclo di vita dell'ordine**: `'in corso'` (carrello) → `'concluso'`, solo con
  `CartModel.confermaOrdine`, che nello stesso aggiornamento porta `carrello` a NULL. Il codice non usa
  altri stati e non prevede l'annullamento.
- **Un ordine concluso conserva solo i prezzi.** Alla conferma si salvano prezzo unitario pagato,
  totale, IVA, data e indirizzo; nome e descrizione dei prodotti mostrati nello storico vengono dalla
  tabella `prodotto` al momento della lettura.
- **La giacenza** (`QTAmagazzino`) limita le quantità nel carrello e diminuisce alla conferma (R10, R26).
- **La valutazione non è memorizzata**: media e numero delle recensioni si calcolano a ogni lettura del
  catalogo (R21).
- **Il ruolo è un attributo dell'utente** (`permessi_admin`): non ci sono tabelle dei ruoli.
- **Categorie e sottocategorie sono stringhe** del prodotto, senza tabella di riferimento: 4 categorie
  (`Viso`, `Labbra`, `Occhi`, `Sopracciglia`) e 16 sottocategorie. Il menu le usa in minuscolo e il
  filtro funziona perché la collation ignora le maiuscole. `all` è un valore riservato che toglie il
  filtro (R18).
- **Immagini**: ogni prodotto ha un'immagine `principale` e di solito una `secondaria`. Solo i prodotti
  con l'immagine principale compaiono nel catalogo e nella ricerca (R18, R20).

### 4.3 Invarianti dei dati

| ID | Invariante | Garantito da | Verifica |
|---|---|---|---|
| I01 | Uno username identifica al più un utente | UNIQUE su `USER_NAME` | schema |
| I02 | Ogni utente ha esattamente un ordine `'in corso'`, cioè un carrello | "al più uno" dal database: UNIQUE (`UTENTE_ID`, `carrello`) con `carrello` = 1 solo per l'ordine in corso (CHECK `carrello_coerente`); "almeno uno" dal codice: `AddUser` crea utente e carrello nella stessa transazione, `confermaOrdine` conclude il carrello e ne crea uno nuovo nella stessa transazione | schema; 28, 28b, 29, 24b, 25, 26; prova |
| I03 | Un prodotto compare al più una volta in un carrello o in un ordine | PK di `contiene` | schema |
| I04 | Un utente scrive al più una recensione per prodotto | UNIQUE (`PRODOTTO_ID`, `userID`) | schema |
| I05 | Nel database ci sono solo hash delle password, mai password in chiaro | `UserControl` cifra la password prima di `AddUser` | 39 |
| I06 | Un ordine concluso ha totale, IVA 20, data e indirizzo; un carrello ha questi campi NULL | `confermaOrdine`; il carrello è creato con il solo `utente_id` | 24, 25; codice |
| I07 | La giacenza di un prodotto non diventa negativa per effetto di un ordine | `confermaOrdine` la diminuisce con `QTAmagazzino >= ?` nella condizione, e annulla l'ordine se non basta | 24c, 26g; prova |

Dipendono da I02 il login (`getUserTramiteCredenziali` cerca l'utente insieme al suo carrello), `getIdCart`,
`getIdCartByUserId` e `CartModel.getProducts`.

### 4.4 Vincoli assenti

- Prezzi, sconti, quantità e punteggi non hanno vincoli `CHECK`. Li controlla il server: i campi del
  prodotto e lo sconto in `AdminControl` (R27), il punteggio in `ProductControl` (R16), la quantità nel
  carrello rispetto alla giacenza in `CartControl` (R10).
- `percentualeSconto` e `percentuale_IVA` sono interi (`decimal(10,0)`), come i campi `int` dei bean.

## 5. Stato della sessione

### 5.1 Attributi

| Attributo | Tipo | Scritto da | Letto da | Significato |
|---|---|---|---|---|
| `userID` | Integer | `UserControl` (login) | `CartControl`, `ProductControl`, `UserControl`, `LoginRegistrazione.jsp`, `productView.jsp` | ID dell'utente loggato |
| `username` | String | `UserControl` (login) | `CartControl`, `AreaUtente.jsp` | username dell'utente loggato |
| `cartID` | Integer | `UserControl` (login), `CartControl` (dopo la conferma) | `CartControl` | ID del carrello, cioè dell'ordine in corso |
| `indirizzo` | String | `UserControl` (login) | `CartControl` (conferma) | indirizzo di consegna (R05) |
| `adminRoles` | Boolean | `UserControl` (login) | `AuthFilter`, `AdminControl`, `AreaUtente.jsp` | ruolo di amministratore |
| `cart` | `CartBean` | `CartControl`; `UserControl` lo toglie dopo un login riuscito | `CartControl`, `UserControl` (registrazione e login), `Cart.jsp` | carrello mostrato |
| `products` | Collection di `ProductBean` | `AdminControl` (`readallEditPrice`) | `admin/modificaProdotto.jsp` | prodotti a cui applicare lo sconto |
| `listaUtenti` | Collection di `UserBean` | `AdminControl` (`visualizzaUtenti`) | `admin/visualizzaUtenti.jsp` | elenco degli utenti |

### 5.2 Invarianti della sessione

| ID | Invariante | Verifica |
|---|---|---|
| I08 | `userID`, `username`, `cartID`, `indirizzo` e `adminRoles` vengono scritti insieme da un login riuscito; nessun'altra azione scrive `userID`, `username`, `indirizzo` e `adminRoles` (`cartID` cambia anche dopo una conferma, I10) | 49, 50 |
| I09 | È amministratore solo una sessione con `adminRoles` uguale a `Boolean.TRUE`: un attributo assente o falso vale "non admin" | 60–66, 68a, 68b |
| I10 | Per l'utente loggato `cartID` è l'ID del suo carrello (I02); dopo una conferma diventa quello del nuovo carrello, restituito dalla stessa transazione | 49, 76, 24d |
| I11 | Il `cart` in sessione viene creato vuoto alla prima richiesta a `/Cart`. Per l'ospite è il carrello; per l'utente loggato è una copia del carrello del database, ricaricata all'inizio di ogni richiesta a `/Cart` | 72, 74 |
| I12 | Un login riuscito rinnova l'identificativo di sessione; se c'era già un utente, la vecchia sessione viene invalidata e i dati vanno in una nuova. Il logout invalida la sessione | 53, 53c, 57 |

## 6. Regole di dominio

| ID | Regola | Dove | Verifica |
|---|---|---|---|
| R01 | Prezzo unitario scontato = prezzo − prezzo × percentualeSconto / 100 | `CartModel.confermaOrdine` (prezzo fatturato); la stessa formula è ripetuta in `Cart.jsp`, `Home.jsp`, `productView.jsp` | 24 (sconti 0, 50 e 100%) |
| R02 | Totale dell'ordine = somma dei prezzi scontati moltiplicati per le quantità, con due decimali. L'IVA non viene aggiunta; la spedizione gratuita è solo un testo di `Cart.jsp` | `confermaOrdine`, `Cart.jsp` | 24, 25; prova |
| R03 | L'ordine registra un'IVA fissa del 20% (`CartModel.PERC_IVA`) | `confermaOrdine` | 24 |
| R04 | La data di inoltro è quella della conferma, nel formato aaaa-mm-gg | `confermaOrdine` | 24 |
| R05 | L'indirizzo di consegna è l'indirizzo dell'utente letto al login (attributo `indirizzo`): non se ne può scegliere un altro | `UserControl`, `CartControl` | 49, 76 |
| R06 | Si conferma un ordine solo da loggati e con il carrello non vuoto | `CartControl` | 76b, 76d |
| R07 | La conferma è atomica: prezzi fatturati, giacenze, chiusura dell'ordine e nuovo carrello, tutti o nessuno | `confermaOrdine` | 24b, 26, 26g, 26h |
| R08 | Lo storico mostra solo gli ordini conclusi dell'utente loggato; un ordine non trovato dà 404 | `UserModel.getOrdineById`, `getAllOrdine`; `CartControl` passa l'utente della sessione | 33, 33f, 33g, 77, 77b, 77c; stato `'concluso'`: codice |
| R09 | Nel carrello i prodotti si riconoscono per ID e non con `equals`: il prodotto ricaricato dal database può avere prezzo, sconto o giacenza diversi | `CartBean` | 2b, 7, 8, 9b |
| R10 | Aggiungere un prodotto già presente ne aumenta la quantità di 1, ma la quantità nel carrello non supera la giacenza; decrementare un prodotto con quantità 1 lo toglie; `elimina` lo toglie qualunque sia la quantità; un prodotto inesistente non si aggiunge (404) | `CartBean`, `CartControl`, `CartModel` | 4, 5, 6, 20, 22, 73b-73e, 74b, 74d-74f, 75b |
| R11 | Alla registrazione i prodotti del carrello dell'ospite vengono copiati nel nuovo carrello con le stesse quantità; l'utente non viene loggato | `UserControl` | 40, 40b; login: codice |
| R12 | La registrazione è atomica (utente e carrello insieme) e il nuovo utente non è amministratore | `UserModel.AddUser` | 27, 28, 28b, 29 |
| R13 | I dati della registrazione rispettano le regole della sezione 7.5 | `UserControl`, `FormValidation.js` | 41–47b |
| R14 | Password: da 8 a 20 caratteri, almeno una maiuscola, una minuscola, una cifra e un carattere che non sia lettera, cifra o spazio; salvata come hash SHA-256 esadecimale dei byte UTF-8, senza sale | `UserControl` | 39, 41, 41b, 42 |
| R15 | Login: lo username deve essere alfanumerico e la password conforme a R14, altrimenti il database non viene interrogato; le credenziali sono username e hash della password | `UserControl` | 49–52 |
| R16 | Recensione: solo da loggati; punteggio intero da 1 a 5; commento con escape HTML (`Html.escape`) prima del salvataggio; data di oggi; una per utente e prodotto | `ProductControl`, `RecensioneModel` | 70, 70b, 70d, 37a, 79; unicità: schema |
| R17 | Una recensione può essere modificata solo dal suo autore, e solo quella sul prodotto indicato | `ProductControl.salvaRecensione` | 71, 71b |
| R18 | Catalogo: solo prodotti con l'immagine principale, ordinati per ID; con la categoria `all` non si filtra, nemmeno per sottocategoria | `ProductModel` | 15b, 18b, 18d |
| R19 | Home paginata: offset = (pagina − 1) × limit; pagina e limit mancanti, non numerici o minori di 1 valgono 1 e 15. I link "Previous" e "Next" mantengono lo stesso limit; "Next" compare quando la pagina è piena | `ProductControl`, `Home.jsp` | 71e; link: prova |
| R20 | Ricerca: i prodotti con l'immagine principale il cui nome contiene il testo cercato, al più 6, in un JSON con i nomi sottoposti a escape | `ProductModel.doRetrieveByInput` | 16, 16b; limite di 6: codice |
| R21 | Valutazione di un prodotto = media e numero delle sue recensioni (0 se non ne ha); nella home ogni card mostra le stelle del proprio prodotto. Un prodotto inserito da non più di 30 giorni ha il banner NEW | `ProductModel`, `HomeAnimations.js` | 15, 18b, 18d; stelle e banner: prova |
| R22 | Area admin solo con `adminRoles` uguale a `Boolean.TRUE`, controllato due volte: da `AuthFilter` su `/admin/*` e `/Admin`, e da `AdminControl` | `AuthFilter`, `AdminControl` | 59–67, 68a, 68b |
| R23 | Uno sconto non salvato (fuori da 0–100, non numerico, prodotto inesistente o errore del database) porta alla pagina di errore, non alla pagina dei prodotti | `AdminControl`, `AzioniAdminModel` | 35b, 69, 69b, 69c, 69e |
| R24 | Nuovo prodotto: id generato dal database, data di inserimento di oggi, unità e sottocategoria (facoltativa) scelte nel form, due immagini (principale e secondaria) | `AdminControl`, `AzioniAdminModel` | 34a, 34c, 34d; prova |
| R25 | "Non trovato" si rappresenta con un oggetto vuoto, una lista vuota o −1, mai con `null`, nei DAO di `Model` | DAO | 12, 23b, 31, 32a, 32b, 33, 33c, 33e, 33g, 38b, 38d |
| R26 | Alla conferma la giacenza di ogni prodotto diminuisce della quantità ordinata; se non basta l'ordine non viene confermato e l'utente va alla pagina di errore | `confermaOrdine`, `CartControl` | 24c, 26g, 76c; prova |
| R27 | I campi di un nuovo prodotto sono controllati anche sul server: nome, descrizione, categoria e colorazione non vuoti; prezzo e grammatura numeri finiti maggiori di 0; giacenza intera non negativa; sconto intero da 0 a 100 (0 se manca); unità `g` o `ml`. Se un controllo fallisce, l'admin torna al form | `AdminControl.prodottoDalForm` | 69f, 69g |
| R28 | Al login di un ospite i prodotti del suo carrello vengono uniti al carrello salvato: per un prodotto già presente le quantità si sommano | `UserControl` | 53b, 53d; prova |

## 7. Componenti e contratti

Precondizioni e postcondizioni sono scritte in linguaggio naturale. Le precondizioni descrivono ciò che
il chiamante deve assicurare: nessun metodo le controlla, salvo dove è detto.

### 7.1 Quanto sono verificati i contratti

| Gruppo | Classi | Casi delle loro classi di test |
|---|---|---|
| Nel perimetro di JaCoCo e PIT ([coverage.md](coverage.md), [mutation.md](mutation.md)) | `CartModel`, `ProductModel`, `UserModel`, `RecensioneModel`, `CartBean`, `ProductBean`, `AzioniAdminModel` | 204; mutation score 96% (PIT esegue i 238 casi dei package `Model` e `admin`) |
| Testate, fuori dal perimetro | `UserControl`, `AuthFilter`, `CartControl`, `ProductControl`, `AdminControl`, `IMGmodel`, `Html`, `RecensioneBean` | 179 |
| Non testate | `StarControl`, `GetPicturesControl`, `LocalServer`, le JSP e gli script; `UserBean` e `OrdineBean` solo attraverso i DAO e le servlet | — |

Servlet non testate, JSP e script sono stati provati con l'applicazione avviata (colonna "Verifica":
*prova*).

### 7.2 Bean

#### `CartBean`

Carrello dell'ospite, salvato in sessione; per l'utente loggato contiene la copia del carrello del
database (I11). Stato: la lista `products` di `ProductBean`.

- **Invariante di classe**: `products` non è `null` (lo crea il costruttore; nessun chiamante passa
  `null` a `setProducts`).
- **Invarianti di protocollo**, mantenuti dai chiamanti e non da `CartBean`:
  - (a) nella lista non ci sono due prodotti con lo stesso ID: `CartControl` aggiunge un prodotto solo se
    `contiene` è falso (test 73b), e il carrello letto dal database non ha ripetizioni (I03);
  - (b) tra una richiesta e l'altra ogni prodotto di un carrello dell'ospite ha quantità almeno 1 e al più
    pari alla giacenza letta all'ultima aggiunta: `aggiungi` inserisce il prodotto e ne incrementa subito
    la quantità, solo se resta entro la giacenza, e il decremento toglie il prodotto quando la quantità è 1;
  - (c) nessun prodotto del carrello è il prodotto vuoto (ID −1): `CartControl` risponde 404 invece di
    aggiungerlo.

"Con lo stesso ID" significa con lo stesso `getID()` (R09). Tutti i metodi che ricevono un prodotto
richiedono che non sia `null`.

| Metodo | Precondizione | Postcondizione | Verifica |
|---|---|---|---|
| `addProduct(p)` | per mantenere (a) e (c): `contiene(p)` falso e `p` non vuoto | `p`, lo stesso oggetto, è aggiunto in coda; nessun controllo su ID o quantità | 1 |
| `deleteProduct(p)` | | il primo prodotto con lo stesso ID è tolto; se non c'è, il carrello non cambia | 2a, 2b, 73d |
| `contiene(p)` | | vero se e solo se nel carrello c'è un prodotto con lo stesso ID; non modifica niente | 9b |
| `incrementaQuantitàOrdinata(p)` | per mantenere (b): quantità attuale minore della giacenza di `p` | la quantità del prodotto con lo stesso ID aumenta di 1 (cambia l'oggetto nel carrello, non `p`); se non c'è, il carrello non cambia | 4, 7, 9c, 73b |
| `rimuoviSingolaQuantitàOrdinata(p)` | | se il prodotto con lo stesso ID ha quantità al più 1 viene tolto, senza modificarlo; altrimenti la sua quantità diminuisce di 1; se non c'è, il carrello non cambia | 5, 6, 8, 9, 9d, 73c |
| `getQuantitàOrdinata(p)` | | la quantità del prodotto con lo stesso ID, oppure −1; non modifica niente | 3, 73c |
| `getProducts()` | | la lista interna, non una copia: modificarla modifica il carrello | 1 |
| `setProducts(l)` | `l` non `null` | la lista interna diventa `l`, lo stesso oggetto | 74 |

#### `ProductBean`

Prodotto del catalogo. Lo stesso bean rappresenta anche una riga di carrello (`QTAordinata`) o di ordine
(`prezzoAllAcquisto`). Valori iniziali: `ID` −1, stringhe vuote, numeri 0, `DataInserimento` `null`.

Ogni lettura valorizza un insieme diverso di campi. I nove campi di base sono `ID`, `nome`, `descrizione`,
`prezzo`, `grammatura`, `categoria`, `QTAmagazzino`, `percentualeSconto` e `colorazione`; gli altri
restano al valore iniziale:

| Lettura | Campi in più rispetto ai nove di base |
|---|---|
| `ProductModel.doRetrieveByKey` | nessuno |
| `ProductModel.doRetrieveAll`, `doRetrieveByCat`, `doRetrieveBySubCat` | `unita`, `subCategoria` (`null` se nel database manca), `DataInserimento`, `votazione`, `Nrecensioni` |
| `CartModel.getProducts` | `QTAordinata` |
| `UserModel.getOrdineById` | `QTAordinata`, `prezzoAllAcquisto` |

`votazione` e `Nrecensioni` restano 0 per i prodotti senza recensioni.

| Metodo | Contratto | Verifica |
|---|---|---|
| `isEmpty()` | vero se e solo se i dieci campi confrontati da `equals` hanno il valore iniziale: `ID` −1; `nome`, `descrizione`, `unita`, `categoria`, `colorazione` vuoti; `prezzo`, `grammatura`, `QTAmagazzino`, `percentualeSconto` uguali a 0. Richiede che quelle stringhe non siano `null` | 10d, 12 |
| `equals(o)` | falso con `null` o con un oggetto di un'altra classe; altrimenti vero se e solo se sono uguali `ID`, `nome`, `descrizione`, `prezzo`, `grammatura`, `unita`, `categoria`, `QTAmagazzino`, `percentualeSconto`, `colorazione`. Non conta `subCategoria`, `QTAordinata`, `votazione`, `Nrecensioni`, `DataInserimento`, `prezzoAllAcquisto` | 10b, 10c |
| `hashCode()` | calcolato sugli stessi dieci campi: oggetti uguali per `equals` hanno lo stesso valore | 10 |
| `StartoJSON()` | richiede `DataInserimento` non `null`, quindi un bean letto da `doRetrieveAll` o dalle letture per categoria; restituisce `{"ID":…,"votazione":…,"Nrecensioni":…,"DataInserimento":"aaaa-mm-gg"}` | prova |
| getter e setter | i getter non modificano niente; i setter non controllano i valori | — |

**Vincoli di dominio**, candidati a diventare invarianti: `prezzo` > 0, `grammatura` > 0,
`QTAmagazzino` ≥ 0, `QTAordinata` ≥ 0, 0 ≤ `percentualeSconto` ≤ 100, `unita` uguale a `g` o `ml`, campi
stringa diversi da `subCategoria` non `null`. Per i prodotti nuovi li garantisce `AdminControl` (R27), la
giacenza la mantiene non negativa `confermaOrdine` (I07) e i campi stringa sono NOT NULL nello schema; i
setter però accettano qualsiasi valore, quindi come invarianti JML richiedono precondizioni sui setter. I
prodotti del dump li rispettano tutti.

#### `UserBean`, `OrdineBean`, `RecensioneBean`

| Bean | Valori iniziali | "Non trovato" | Note |
|---|---|---|---|
| `UserBean` | `ID` 0, stringhe vuote, `DataNascita` 1/1/1970, `cartID` −1, `permessiAdmin` falso | il bean iniziale (`ID` 0) | `password` contiene l'hash; `cartID` è letto solo da `getUserTramiteCredenziali`. Solo getter e setter |
| `OrdineBean` | `idOrdine` 0, totale e IVA 0, stringhe vuote, `prodotti` `null` | `idOrdine` 0 con lista di prodotti vuota: `getOrdineById` imposta sempre la lista | `dataInoltro` è una stringa aaaa-mm-gg. Solo getter e setter |
| `RecensioneBean` | `ID`, `votazione`, `PRODOTTO_ID`, `userID` −1; `dataInserimento` 1/1/1970; `commento` vuoto | il bean iniziale | `isEmpty()` è vero se e solo se tutti i campi hanno il valore iniziale (un commento `null` conta come vuoto): test 38a, 38c, 38d. `equals` e `hashCode` usano `ID`, `votazione`, `PRODOTTO_ID`, `userID`, `dataInserimento` e `commento`: test 10e, 10f |

### 7.3 DAO

#### Contratti comuni

Valgono per tutti i metodi di `CartModel`, `ProductModel`, `UserModel`, `RecensioneModel`,
`AzioniAdminModel` e `IMGmodel`.

| ID | Contratto | Verifica |
|---|---|---|
| C1 | Connessione, statement e `ResultSet` aperti vengono chiusi su ogni percorso (try-with-resources) | 14, 14b, 19, 23, 24d, 26c, 33i, 34c, 36a, 36b, 38f, 78c, 78d |
| C2 | Se `getConnection()` fallisce, il metodo lancia la stessa `SQLException`, non una `NullPointerException` | 17, 17b, 26b, 33h, 38e |
| C3 | Se una query fallisce, la stessa `SQLException` arriva al chiamante e le risorse sono chiuse | 14, 17c, 17d, 26d, 33j |
| C4 | Se fallisce la chiusura dello statement, dopo una query riuscita arriva al chiamante quell'errore; dopo una query fallita arriva l'errore della query, con quello della chiusura tra i soppressi. In entrambi i casi la connessione viene chiusa | 17e–17h, 26e, 26f, 33k–33n, 38g, 38h |
| C5 | I valori arrivano al database come parametri del `PreparedStatement`; fa eccezione l'ordinamento (`order`), concatenato alla query: i chiamanti passano sempre la costante `"ID"` | 18, 18c, 78a; `order`: codice |
| C6 | Il `DataSource` si ottiene una sola volta, con il lookup JNDI nel blocco statico. Fuori da Tomcat il lookup fallisce, viene stampato un errore e `ds` resta `null`; i test lo sostituiscono con un mock | codice |

Per i DAO di `Model` vale R25. Le transazioni sono descritte nella sezione 9.

#### `CartModel`

| Metodo | Precondizioni (dal chiamante) | Postcondizioni | Verifica |
|---|---|---|---|
| `addProduct(idCarrello, prodotto, giaPresente, qta)` | `idCarrello` è il carrello dell'utente; `prodotto` esiste; `giaPresente` è vero se e solo se il prodotto è già nel carrello; `qta` ≥ 1 | con `giaPresente` vero la quantità della riga aumenta di `qta`; con `giaPresente` falso viene inserita una riga con quantità `qta`. Se `giaPresente` è falso ma la riga esiste: `SQLException` (chiave primaria); se è vero ma la riga non esiste: nessuna modifica | 20, 21 |
| `deleteProduct(idCarrello, prodotto, isUltimo)` | come sopra; `isUltimo` è vero se la quantità è al più 1 o se il prodotto va tolto | con `isUltimo` vero la riga è cancellata; altrimenti la quantità diminuisce di 1, ma solo se è maggiore di 1 | 22 |
| `getProducts(id)` | `id` è l'ID **dell'utente**, non del carrello | i prodotti del carrello dell'utente con la loro quantità; lista vuota se il carrello è vuoto | 23, 23b |
| `confermaOrdine(indirizzo, idOrdine, idUtente, prodotti)` | `idOrdine` è il carrello di `idUtente`; `prodotti` non è `null` e contiene i prodotti di quel carrello con quantità e prezzi attuali (`CartControl` li ricarica dal database nella stessa richiesta). Il metodo accetta anche una lista vuota: il controllo è in `CartControl` (R06) | in un'unica transazione (R07): per ogni prodotto `prezzo_all_acquisto` = prezzo scontato (R01) e giacenza diminuita della quantità (R26); l'ordine diventa `'concluso'`, con `carrello` NULL, totale (R02), IVA 20 (R03), data di oggi (R04) e indirizzo; viene creato un nuovo carrello per `idUtente`, e il metodo ne restituisce l'id. Se una giacenza non basta o un passo fallisce: rollback, autocommit ripristinato, `SQLException` al chiamante | 24, 24b, 24c, 24d, 25, 26, 26g, 26h |

`confermaOrdine` non verifica che `idOrdine` appartenga a `idUtente` né che sia in corso; quantità e
prezzi sono quelli dei bean ricevuti. Incremento e decremento non usano subquery sulla tabella che
aggiornano, quindi funzionano anche su MySQL (test 20, 22).

#### `ProductModel`

| Metodo | Precondizioni | Postcondizioni | Verifica |
|---|---|---|---|
| `doRetrieveByKey(id)` | — | il prodotto con quell'ID (nove campi di base), anche se non ha immagini; prodotto vuoto (`isEmpty`) se non esiste | 11–14 |
| `doRetrieveAll(order, offset, limit)` | `order` `null`, vuoto o nome di colonna fidato (C5); `offset` e `limit` non negativi | i prodotti con l'immagine principale, ordinati per `order` se indicato, a partire da `offset` e al più `limit`, con unità, sottocategoria e votazioni | 15, 15b |
| `doRetrieveByCat(category, order)` | `category` non `null` | con `all` tutti i prodotti con l'immagine principale; altrimenti quelli della categoria (maiuscole ignorate); stessi campi di `doRetrieveAll` | 18, 18b |
| `doRetrieveBySubCat(category, subcategory, order)` | `category` non `null` | con `all` come sopra, e `subcategory` è ignorata; altrimenti i prodotti di quella categoria e sottocategoria | 18c, 18d |
| `doRetrieveByInput(input)` | `input` non `null` (con `null` cerca la stringa "null") | JSON `[{"Id": n, "nome": "…"}, …]` con al più 6 prodotti (R20); `[]` se non ce ne sono. Nei nomi virgolette, barre rovesciate e caratteri di controllo diventano sequenze di escape | 16, 16b, 17 |
| `doDelete(code)` | — | cancella il prodotto e, a cascata, le sue immagini, recensioni e righe di carrelli e ordini; vero se e solo se ha cancellato almeno una riga. Nessuna servlet lo usa (L01) | 19 |

Le votazioni (R21) vengono da una seconda query su tutte le recensioni, raggruppate per prodotto.

#### `UserModel`

| Metodo | Precondizioni | Postcondizioni | Verifica |
|---|---|---|---|
| `AddUser(user)` | campi non `null`; password già cifrata | in un'unica transazione: nuovo utente con `permessi_admin` 0 e nuovo carrello vuoto. Con uno username già usato: `SQLException` (I01) e nessuna modifica | 27, 28, 28b, 29 |
| `getUserTramiteCredenziali(username, pwd)` | `pwd` è l'hash della password | l'utente con `ID`, username, password, `cartID`, indirizzo, telefono e ruolo (non nome, cognome, email e data di nascita); utente vuoto se le credenziali non corrispondono o se l'utente non ha un carrello (I02) | 30, 31 |
| `getIdCart(username, pwd)` | `pwd` è l'hash | l'ID del carrello dell'utente, −1 se non lo trova | 32a |
| `getIdCartByUserId(id)` | — | l'ID del carrello dell'utente, −1 se non lo trova | 32b |
| `getUserTramiteID(id)` (non è in `UserModel_intf`) | — | tutti i dati dell'utente, con `cartID` −1; utente vuoto se non esiste | 33b, 33c |
| `getUtenti()` | — | tutti gli utenti con tutti i dati e `cartID` −1; lista vuota se non ce ne sono | 33d, 33e |
| `getAllOrdine(idUtente)` | — | gli ID degli ordini conclusi dell'utente, senza un ordine definito; lista vuota se non ce ne sono | 33 |
| `getOrdineById(idUtente, idOrdine)` | — | se l'ordine appartiene all'utente ed è concluso: totale, IVA, data, indirizzo e prodotti con quantità e prezzo pagato; altrimenti ordine vuoto (`idOrdine` 0) senza prodotti | 33f, 33g, 33j, 33m, 33n |

#### `RecensioneModel`

| Metodo | Precondizioni (le assicura `ProductControl`) | Postcondizioni | Verifica |
|---|---|---|---|
| `addRecensione(userID, prodID, punteggio, commento)` | utente loggato; 1 ≤ `punteggio` ≤ 5; `commento` già sottoposto a escape | nuova recensione con la data di oggi; una seconda recensione dello stesso utente sullo stesso prodotto dà `SQLException` (I04) | 37a |
| `modificaRecensione(idRecensione, punteggio, commento)` | `idRecensione` è la recensione dell'utente sul prodotto (R17); punteggio e commento come sopra | aggiorna punteggio e commento, non la data; con un ID inesistente non modifica niente e stampa solo un messaggio | 37b |
| `getAllRecensioni(prodID)` | — | le recensioni del prodotto; lista vuota se non ce ne sono | 38a, 38b |
| `getRecensione(userID, prodID)` | — | la recensione dell'utente sul prodotto, oppure la recensione vuota (`isEmpty`) | 38c, 38d |

#### `AzioniAdminModel`

| Metodo | Precondizioni (le assicura `AdminControl`, R27) | Postcondizioni | Verifica |
|---|---|---|---|
| `inserisciProdotto(p)` | `p` con i valori di dominio validi (7.2) e la data di inserimento | inserisce 11 campi (tutti tranne l'id), con la sottocategoria vuota o assente salvata come NULL; se ha inserito una riga scrive nel bean l'id generato e restituisce vero, altrimenti restituisce falso; se il database non restituisce l'id: `SQLException` e bean invariato | 34a–34e, 36a |
| `modificaSconto(id, sconto)` | 0 ≤ `sconto` ≤ 100 | aggiorna lo sconto; vero se e solo se il prodotto esiste ed è stato modificato | 35, 35b, 36b |

#### `IMGmodel`

| Metodo | Precondizioni | Postcondizioni | Verifica |
|---|---|---|---|
| `load(idProdotto, ruolo)` (statico) | — | i byte dell'immagine del prodotto con quel ruolo, oppure `null` se non esiste; errori del database come `SQLException` | 78a, 78b, 78c |
| `updatePhoto(idProdotto, ruolo, percorso)` (statico) | il file esiste ed è leggibile | inserisce nella tabella `immagine` il contenuto del file con il ruolo e il prodotto indicati; errori come `SQLException` o `IOException` | 78d |

### 7.4 Servlet e filtro

#### `AuthFilter`

Il percorso è `servletPath` seguito da `pathInfo`, confrontato senza maiuscole (`isAreaAdmin`). Il filtro
non crea sessioni.

| Percorso | Sessione | `adminRoles` | Esito | Verifica |
|---|---|---|---|---|
| non inizia con `/admin` | qualsiasi | qualsiasi | la richiesta prosegue | 59 |
| inizia con `/admin` | assente | — | redirect a `<contextPath>/LoginRegistrazione.jsp` | 60, 63b, 64 |
| inizia con `/admin` | presente | diverso da `Boolean.TRUE` | redirect a `<contextPath>/LoginRegistrazione.jsp` | 61, 63, 65 |
| inizia con `/admin` | presente | `Boolean.TRUE` | la richiesta prosegue | 62, 66 |

`web.xml` applica il filtro a ogni URL della servlet `Admin` (test 67).

#### `AdminControl` (`/Admin`)

Prima di qualsiasi azione controlla il ruolo come `AuthFilter`: senza ruolo admin fa il redirect alla
pagina di login senza usare i DAO (test 68a, 68b). Le azioni ignorano le maiuscole e i redirect includono
il context path. Funzioni di supporto: `scontoValido(s)` è vero se e solo se 0 ≤ s ≤ 100;
`prodottoDalForm(request)` restituisce il prodotto descritto dal form, con la data di oggi, oppure `null`
se un controllo di R27 fallisce.

| Azione | Effetto | Risposta | Verifica |
|---|---|---|---|
| `inserisciProdotto` | `prodottoDalForm`; se il prodotto è valido e viene inserito, salva le immagini dei campi `immagine1` (principale) e `immagine2` (secondaria) con l'id generato | redirect alla scheda del nuovo prodotto; se non valido o non inserito, al form | 69f, 69g; salvataggio: prova |
| `readallEditPrice` | mette in sessione l'intero catalogo (`products`) | redirect a `/admin/modificaProdotto.jsp` | solo il ruolo (68a, 68b); prova |
| `visualizzaUtenti` | mette in sessione gli utenti (`listaUtenti`) | redirect a `/admin/visualizzaUtenti.jsp` | solo il ruolo (68a, 68b) |
| `impostaSconto` | con `id` e `sconto` interi e lo sconto valido, `modificaSconto(id, sconto)` | se salvato, forward a `/admin/modificaProdotto.jsp`; altrimenti redirect a `/Error.jsp` (R23) | 69, 69b, 69c, 69e |
| mancante, vuota o sconosciuta | nessuno | redirect a `/Error.jsp` | 69d |

Se il salvataggio di un'immagine fallisce dopo l'inserimento del prodotto, l'eccezione porta alla pagina di
errore di `web.xml`.

#### `UserControl` (`/User`)

Le risposte JSON contengono il solo campo `val`: `"1"` se l'operazione è riuscita, `"0"` altrimenti.

| Azione | Condizioni | Effetto | Risposta | Verifica |
|---|---|---|---|---|
| `register` | dati validi (7.5) | salva l'utente con la password cifrata (R12, R14), poi copia il carrello dell'ospite (R11) | `val` 1; dati non validi o errore del database: `val` 0 | 39–48 |
| `login` | username e password presenti e nel formato giusto (R15) | se le credenziali corrispondono: nuovo identificativo di sessione, sessione scritta (I08, I12), carrello dell'ospite unito a quello salvato (R28) e tolto dalla sessione | `val` 1 o 0; formato non valido: forward a `LoginRegistrazione.jsp`; errore del database: redirect a `/Error.jsp` | 49–54 |
| `DatiUtente` | `userID` in sessione | legge i dati dell'utente | forward ad `AreaUtente.jsp`; senza login, forward a `LoginRegistrazione.jsp`; errore del database: redirect a `/Error.jsp` | 55, 55b, 56 |
| `logout` | — | invalida la sessione | forward a `LoginRegistrazione.jsp` | 57 |
| altra azione | — | — | forward a `Home.jsp` | 58 |
| nessuna azione | — | — | risposta vuota | codice |

#### `CartControl` (`/Cart`)

All'inizio di ogni richiesta crea un carrello vuoto in sessione se manca e, se l'utente è loggato
(`username` e `userID` in sessione), sostituisce il contenuto del carrello con quello del database (I11).
Ogni azione su un prodotto lo rilegge con `doRetrieveByKey`. Le azioni ignorano le maiuscole; i percorsi
sono assoluti, senza context path. Funzione di supporto: `sottoGiacenza(cart, p)` è vera se e solo se la
quantità di `p` nel carrello (0 se assente) è minore della giacenza di `p`.

| Azione | Ospite | Utente loggato | Risposta | Verifica |
|---|---|---|---|---|
| `aggiungi&id` | se `sottoGiacenza`: se il prodotto non c'è lo aggiunge, poi aumenta la quantità di 1 | se `sottoGiacenza`: `addProduct(cartID, p, contiene(p), 1)` | redirect a `/`; prodotto inesistente: 404 | 73, 73b, 73e, 74b, 74f, 75b |
| `aumentaQTA&id` | se `sottoGiacenza`: quantità +1, se il prodotto c'è | se `sottoGiacenza`: `addProduct(cartID, p, true, 1)` | redirect a `/Cart?action=visualizza`; prodotto inesistente: 404 | 73b, 73e, 74c, 74f, 75b |
| `decrementaQTA&id` | quantità −1, oppure toglie il prodotto se la quantità è 1 | `deleteProduct(cartID, p, quantità ≤ 1)` | redirect a `/Cart?action=visualizza` | 73c, 74d |
| `elimina&id` | toglie il prodotto | `deleteProduct(cartID, p, true)` | forward a `/Cart?action=visualizza` | 73d, 74e |
| `confermaOrdine` | — | con il carrello non vuoto, `confermaOrdine(indirizzo, cartID, userID, prodotti)`; poi carrello vuoto e `cartID` uguale all'id restituito | ospite: redirect al login; carrello vuoto: redirect al carrello; riuscita: redirect a `/Cart?action=getOrdineStorico&id=<ordine confermato>`; errore o giacenza insufficiente: redirect a `/Error.jsp`, carrello invariato | 76, 76b, 76c, 76d |
| `getOrdineStorico&id` | — | `getOrdineById(userID, id)` | forward a `/ordineStorico.jsp`; ordine non trovato: 404; errore del database: redirect a `/Error.jsp`; ospite: redirect al login | 76d, 77, 77c, 77d |
| `StoricoCompletoOrdini` | — | `getAllOrdine(userID)` | forward a `/StoricoOrdini.jsp`; errore del database: redirect a `/Error.jsp`; ospite: redirect al login | 76d, 77b, 77d |
| `visualizza`, mancante o sconosciuta | — | — | forward a `/Cart.jsp` | 72, 72b, 74 |

Con un `id` mancante o non numerico (`aggiungi`, `aumentaQTA`, `decrementaQTA`, `elimina`,
`getOrdineStorico`) la risposta è 400 e il carrello non cambia (test 75). Un errore del database nelle
azioni sul carrello viene solo stampato e la risposta è quella di un'azione riuscita (L03).

#### `ProductControl` (`/`)

| Metodo e azione | Parametri | Effetto | Risposta | Verifica |
|---|---|---|---|---|
| GET senza azione | `page`, `limit` | `doRetrieveAll("ID", offset, limit)` (R19) | forward a `Home.jsp` con `products`, `currentPage`, `limit`; errore del database: 500 | 71e |
| GET `read` | `ProductID` | prodotto, recensione dell'utente (ID −1 per l'ospite) e recensioni del prodotto | forward a `productView.jsp`; `ProductID` mancante, non numerico o di un prodotto inesistente: 404 | 71c, 71d |
| GET `readall` | — | `doRetrieveByCat("all", "ID")` | forward a `Home.jsp` | prova |
| GET `readbycat` | `category` | `doRetrieveByCat(category, "ID")` | forward a `Home.jsp` | prova |
| GET `readbysubcat` | `category`, `subcategory` | `doRetrieveBySubCat(category, subcategory, "ID")` | forward a `Home.jsp` | prova |
| GET `dettagliBarraRicercaJSON` | `input` | `doRetrieveByInput(input)` | JSON della ricerca | prova |
| GET con altra azione | — | — | risposta vuota | codice |
| POST `recensione`, `modificaRecensione` | `productID`, `punteggio`, `commento`, `recensioneID` | `salvaRecensione` (R16, R17) | `{"val":"1"}` solo se la recensione è stata salvata, altrimenti `{"val":"0"}` | 70–71b |

Nelle azioni GET un errore del database produce un'eccezione e la pagina di errore di `web.xml`.

`salvaRecensione` restituisce falso senza usare il database se in sessione non c'è `userID`, se
`productID` o `punteggio` non sono numeri o se il punteggio è fuori da 1–5. Per una modifica restituisce
falso, senza modificare niente, anche se l'utente non ha recensito il prodotto o se `recensioneID` non è
l'ID della sua recensione. Con un errore del database restituisce falso. Funzioni di supporto:
`punteggioValido(v)` è vero se e solo se 1 ≤ v ≤ 5; `commentoValido(t)` restituisce `Html.escape(t)`;
`interoPositivo(s, d)` restituisce `s` come intero se è un numero almeno 1, altrimenti `d`.

#### `Html`

`Html.escape(t)` sostituisce `&`, `<`, `>`, `"` e `'` con `&amp;`, `&lt;`, `&gt;`, `&quot;` e `&#039;`,
carattere per carattere, quindi anche la `&` di un'entità già scritta; con `null` restituisce la stringa
vuota (test 79). Lo usano `ProductControl` per i commenti, che vengono salvati già sottoposti a escape, e le
JSP per tutto il resto del testo mostrato.

#### `StarControl` e `GetPicturesControl`

- `StarControl` (`/StarControl`) restituisce un array JSON con ID, votazione, numero di recensioni e data
  di inserimento di tutti i prodotti del catalogo, letti con `doRetrieveAll("ID", 0, 9999)`; `[]` con il
  catalogo vuoto, 500 con un errore del database. Lo usa `HomeAnimations.js` (R21).
- `GetPicturesControl` (`/getPicture`) restituisce i byte dell'immagine indicata da `IDpic` e `ruolo`
  (`principale` se manca), con tipo `image/png` per i file con la firma PNG e `image/jpeg` per gli altri,
  dichiarato prima dei byte (`tipoImmagine`). `IDpic` mancante o non numerico: 400; nessuna immagine: 404,
  e le pagine mostrano un'immagine di riserva; errore del database: 500.

### 7.5 Validazione degli input

Il server ripete i controlli del browser, perché una richiesta diretta salta il JavaScript. Un campo che
contiene solo spazi conta come vuoto. Login e registrazione inviano i parametri codificati
(`URLSearchParams`), come il form delle recensioni.

| Campo | Regola sul server | Nel browser | Verifica |
|---|---|---|---|
| nome, cognome | obbligatori; solo lettere `[A-Za-z]`, quindi niente accenti, apostrofi o spazi | stessa regola | 47, 47b |
| username | obbligatorio; solo lettere e cifre | stessa regola | 47, 47b, 51 |
| telefono | `^\+?([0-9]{2})\)?[-. ]?([0-9]{4})[-. ]?([0-9]{4})$`: 10 cifre, con separatori e `+` iniziale facoltativi | stessa espressione | 44 |
| data di nascita | una data esistente nel formato aaaa-mm-gg, senza altro testo (`LocalDate.parse`), non futura e successiva a "oggi meno 100 anni": oggi è accettata, domani ed esattamente 100 anni fa no | stesso intervallo | 45, 45b, 46, 46b |
| email | `^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$` | stessa espressione | 43 |
| indirizzo | obbligatorio | obbligatorio, composto da regione, provincia, comune e CAP coerenti tra loro secondo i file JSON | 47 |
| password | da 8 a 20 caratteri; almeno una maiuscola, una minuscola, una cifra e un carattere che non sia lettera, cifra o spazio | stessa regola | 41, 41b, 42 |
| password al login | stessa regola, con username alfanumerico | stessa regola | 51, 52 |
| punteggio della recensione | intero da 1 a 5 | da 1 a 5 stelle | 70, 70b |
| commento della recensione | nessun vincolo; escape HTML | nessuno | 70, 79 |
| campi del nuovo prodotto | R27 | stessi controlli (`validateForm`) e vincoli HTML | 69f, 69g |
| sconto (admin) | intero da 0 a 100 | `min` 0, `max` 100 | 69e |

### 7.6 Viste

Le JSP stampano con `Html.escape` il testo di utenti e prodotti; i commenti delle recensioni sono stampati
così come sono salvati, già sottoposti a escape.

| Vista | Legge | Chiama |
|---|---|---|
| `Home.jsp` | richiesta: `products`, `currentPage`, `limit` | `?action=read`, `/Cart?action=aggiungi`; pagine successive e precedenti solo nella home paginata; `/StarControl` tramite `HomeAnimations.js`, che abbina le votazioni alle card con `data-id` |
| `productView.jsp` | richiesta: `product`, `recensione`, `listaRecensioni`; sessione: `userID` | `/Cart?action=aggiungi`; `POST /` per le recensioni (`recensioniProdotto.js`) |
| `Cart.jsp` | sessione: `cart` | `/Cart` con `decrementaQTA`, `aumentaQTA`, `elimina`, `confermaOrdine` |
| `LoginRegistrazione.jsp` | sessione: `userID` (se c'è, redirect a `/User?action=DatiUtente`) | `POST /User` per login e registrazione (`FormValidation.js`, `indirizzo.js`) |
| `AreaUtente.jsp` | richiesta: `datiUtente`; sessione: `username`, `adminRoles` | storico, carrello, logout; per l'admin `/Admin?action=readallEditPrice`, `/admin/InserisciProdotto.jsp`, `/admin/visualizzaUtenti.jsp` |
| `StoricoOrdini.jsp` | richiesta: `ordini` | `/Cart?action=getOrdineStorico&id=…` |
| `ordineStorico.jsp` | richiesta: `ordine` | — |
| `admin/InserisciProdotto.jsp` | — | `POST /Admin` multipart con `inserisciProdotto` |
| `admin/modificaProdotto.jsp` | sessione: `products` (se manca, redirect a `/`) | `POST /Admin` con `impostaSconto` |
| `admin/visualizzaUtenti.jsp` | sessione: `listaUtenti` (se manca, redirect a `/Admin?action=visualizzaUtenti`) | — |
| `html/menu.html`, incluso in tutte le JSP tranne le pagine di errore, `StoricoOrdini.jsp` e `ordineStorico.jsp` | — | `/?action=readall`, `readbycat`, `readbysubcat`; ricerca con `barraDiRicerca.js` |
| `html/footer.html` | — | `/` |
| `Contatti.jsp` | — | nessuna servlet: apre il client di posta |
| `Error.jsp`, `Error404.jsp`, `Error500.jsp` | — | — |

## 8. Flussi principali

**F1 – Catalogo.**
1. `GET /` (home paginata, R19) oppure, dal menu, `/?action=readall`, `readbycat` o `readbysubcat` (R18):
   `ProductControl` legge i prodotti con `ProductModel` e li passa a `Home.jsp`.
2. Le immagini arrivano da `/getPicture`; `HomeAnimations.js` chiede a `/StarControl` votazioni e date
   dell'intero catalogo e disegna stelle e banner NEW della card di ogni prodotto, cercandolo per ID (R21).
3. La barra di ricerca chiama `/?action=dettagliBarraRicercaJSON` 200 ms dopo ogni carattere e costruisce i
   suggerimenti con il DOM (R20).
4. `/?action=read&ProductID=…` apre la scheda del prodotto con le recensioni.

**F2 – Carrello dell'ospite e registrazione.**
1. L'ospite aggiunge prodotti al carrello in sessione (`/Cart?action=aggiungi`), entro la giacenza (R10).
2. La conferma dell'ordine da ospite rimanda al login (R06).
3. `FormValidation.js` controlla il form e invia `POST /User` con `action=register`; `UserControl`
   ripete i controlli (7.5), cifra la password (R14), chiama `AddUser` (R12) e copia il carrello
   dell'ospite (R11).
4. Con `val` 1 il browser mostra il form di login: l'utente non è ancora loggato.

**F3 – Login e carrello dell'utente.**
1. `POST /User` con `action=login`: controllo del formato (R15), poi `getUserTramiteCredenziali` con
   l'hash della password. Se le credenziali corrispondono, l'identificativo di sessione viene rinnovato,
   la sessione viene scritta (I08) e il carrello dell'ospite viene unito a quello salvato (R28).
2. Il browser apre l'area utente (`/User?action=DatiUtente`, poi `AreaUtente.jsp`), con i pulsanti
   dell'admin se `adminRoles` è vero.
3. A ogni richiesta a `/Cart` il carrello in sessione viene ricaricato dal database (I11); le modifiche
   vanno a `CartModel` sul carrello `cartID`.

**F4 – Conferma dell'ordine e storico.**
1. Da `Cart.jsp`: `POST /Cart?action=confermaOrdine`.
2. `CartControl` ricarica il carrello, controlla login e carrello non vuoto (R06) e chiama
   `confermaOrdine` con l'indirizzo della sessione (R01–R05, R07, R26).
3. In sessione restano un carrello vuoto e il `cartID` del nuovo carrello, restituito da `confermaOrdine`
   (I10).
4. Redirect a `/Cart?action=getOrdineStorico&id=…`: `getOrdineById` con l'utente della sessione (R08)
   e fattura in `ordineStorico.jsp`. `/Cart?action=StoricoCompletoOrdini` elenca gli ordini conclusi.

**F5 – Recensione.**
1. Nella scheda l'utente loggato sceglie le stelle e scrive il commento; `recensioniProdotto.js` invia
   `POST /` con `action=recensione` (`recensioneID` −1) oppure `modificaRecensione`.
2. `salvaRecensione` controlla login, punteggio (R16) e autore (R17), fa l'escape del commento e chiama
   `RecensioneModel`.
3. La risposta `val` è mostrata senza ricaricare la pagina.

**F6 – Area amministratore.**
1. Ogni richiesta a `/admin/*` e a `/Admin` passa da `AuthFilter`, e `AdminControl` ricontrolla il
   ruolo (R22).
2. Sconto: `/Admin?action=readallEditPrice` mette i prodotti in sessione e apre
   `admin/modificaProdotto.jsp`; il form invia `impostaSconto`; se lo sconto non è salvato si va alla
   pagina di errore (R23).
3. Nuovo prodotto: `admin/InserisciProdotto.jsp` invia il form multipart; `AdminControl` controlla i campi
   (R27), `inserisciProdotto` salva il prodotto e ne legge l'id generato, poi le due immagini vengono scritte
   nella cartella `uploadTemp` dell'applicazione e salvate nel database con `IMGmodel.updatePhoto` (R24).
4. Utenti: `admin/visualizzaUtenti.jsp`; se l'elenco non è in sessione, `/Admin?action=visualizzaUtenti`
   lo carica.

## 9. Aspetti trasversali

**Transazioni.** Solo `AddUser` e `confermaOrdine` usano una transazione esplicita: autocommit
disattivato, `commit` dopo l'ultimo passo, `rollback` in caso di errore con l'eventuale errore del
rollback tra i soppressi, autocommit ripristinato alla fine. Le altre operazioni sono singole istruzioni
in autocommit. La copia del carrello dell'ospite alla registrazione e la sua unione al login sono fuori da
queste transazioni: un prodotto non copiato non annulla la registrazione o il login.

**Concorrenza.** Ogni servlet ha una sola istanza di ciascuno dei suoi DAO, usata da tutti i thread; i
DAO non hanno stato. Alcuni metodi sono `synchronized` (`CartModel.addProduct`, `ProductModel.doRetrieveByKey`
e `doDelete`, i metodi di `RecensioneModel`), ma il blocco vale per una sola istanza: tra richieste
contemporanee la coerenza dei dati dipende dal database. Lo garantiscono i vincoli (I01–I04) e l'UPDATE
condizionato della giacenza (I07): due conferme contemporanee non possono ordinare più pezzi di quelli in
magazzino. `CartBean` non è protetto da richieste contemporanee della stessa sessione.

**Gestione degli errori.**

| Strategia | Dove |
|---|---|
| risposta JSON con `val` 0 | registrazione e login (`UserControl`), recensioni (`ProductControl`) |
| redirect a `Error.jsp` | errore del database al login, nei dati utente e nello storico; conferma dell'ordine fallita o con giacenza insufficiente; sconto non salvato; azione admin mancante o sconosciuta |
| risposta 400 | `id` mancante o non numerico nelle azioni del carrello; `IDpic` non valido |
| risposta 404 | prodotto inesistente nel carrello o nella scheda; ordine non trovato; immagine mancante |
| risposta 500 (`Error500.jsp`) | errore del database nella home paginata, in `StarControl` e nelle immagini |
| eccezione al container (pagina di errore di `web.xml`) | errori del database nelle altre pagine del catalogo; immagine del nuovo prodotto non salvata |
| errore solo stampato | azioni sul carrello, copia e unione del carrello dell'ospite, caricamento di prodotti e utenti nell'area admin (L03) |

**Sicurezza.**
- Accesso all'area admin controllato due volte (R22).
- Query con parametri in tutti i DAO, `IMGmodel` compreso; fa eccezione solo la clausola `ORDER BY`, con un
  valore fisso (C5).
- Testo di utenti e prodotti sottoposto a escape nelle pagine; commenti delle recensioni sottoposti a
  escape prima del salvataggio (`Html`).
- Identificativo di sessione rinnovato al login (I12).
- Password con SHA-256 senza sale (R14); nessun limite ai tentativi di login (L02).
- Nessuna password nel repository: le credenziali sono generate in locale e passate a Docker come
  secrets ([Docker.md](Docker.md), [esecuzioneLocaleVS.md](esecuzioneLocaleVS.md)).

## 10. Limiti noti

Scelte che restano nel sistema e che le specifiche devono tenere presenti.

| ID | Limite | Effetto sulle specifiche |
|---|---|---|
| L01 | `ProductModel.doDelete` è verificato dal test 19 ma nessuna servlet lo usa; se venisse usato cancellerebbe a cascata anche le righe degli ordini conclusi | va specificato come metodo del DAO, sapendo che non fa parte di nessun flusso |
| L02 | Password con SHA-256 senza sale e senza limite ai tentativi di login; il logout è una richiesta GET, come prevede il test 57 | — |
| L03 | Nelle azioni sul carrello, nella copia e nell'unione del carrello dell'ospite e nel caricamento delle liste dell'area admin un errore del database viene solo stampato e la risposta è quella di un'azione riuscita | le postcondizioni di queste azioni valgono solo se il database risponde |
| L04 | Nei DAO di `Model` alcune condizioni scelgono solo quale messaggio stampare (7 mutanti equivalenti, [mutation.md](mutation.md)) | non hanno effetti da specificare |
| L05 | Il vincolo `CHECK` di `ordine` è applicato da MariaDB e da MySQL 8.0.16 in poi; le versioni precedenti di MySQL lo accettano ma lo ignorano. L'unicità del carrello (UNIQUE) vale con qualsiasi versione. L'applicazione è stata provata solo su MariaDB | — |
| L06 | `StarControl` e `GetPicturesControl` non hanno test di unità: sono verificate solo con l'applicazione avviata | i loro contratti (7.4) sono "prova", non "test" |

## 11. Indicazioni per il Lab 5

Corrispondenza tra le categorie dello step 7.1 del Lab 5 e i componenti di BeautyBlind.

| Categoria del Lab | Componenti | Contratti in questo documento | Note |
|---|---|---|---|
| State management | `CartBean` | 7.2: invariante di classe, invarianti di protocollo, contratti dei metodi; R09, R10 | usa solo classi Java standard: adatto sia a ESC sia a RAC. Per dimostrare gli invarianti di protocollo servono precondizioni su `addProduct` e `incrementaQuantitàOrdinata` |
| Data validation | `Html.escape`; `AdminControl.scontoValido` e `prodottoDalForm`; `ProductControl.punteggioValido`, `commentoValido` e `interoPositivo`; `CartControl.sottoGiacenza`; `UserControl`: `vuoto`, `isAllLetter`, `isAlphanumeric`, `emailIsValid`, `phoneNumberIsValid`, `passwordIsValid`, `birthDateIsValid`, `datiRegistrazioneValidi`, `hashPassword`; `GetPicturesControl.tipoImmagine` | 7.4, 7.5; R13–R16, R27 | `Html.escape`, `scontoValido`, `punteggioValido`, `interoPositivo`, `sottoGiacenza` e `tipoImmagine` sono funzioni pure su stringhe, interi o array: le più adatte a ESC. Espressioni regolari, date e hash di `UserControl` sono difficili per ESC, quindi conviene RAC |
| Security checks | `AuthFilter.isAreaAdmin` e `doFilter`; il controllo del ruolo in `AdminControl.doGet` | 7.4 (tabella di decisione); R22, I09 | `isAreaAdmin` dipende solo da due stringhe; `doFilter` dalla Servlet API |
| Service, logica applicativa | non c'è uno strato di servizi: le regole sono nei DAO, soprattutto in `CartModel.confermaOrdine` | 7.3: C1–C6 e contratti dei metodi; R01–R07, R26 | dipendono da JDBC e dal lookup JNDI (C6): si prestano più a RAC, con i mock dei test, che a ESC |
| Oggetti del dominio | `ProductBean` (`equals`, `hashCode`, `isEmpty`), `RecensioneBean` (`equals`, `hashCode`, `isEmpty`), getter | 7.2 | i vincoli di dominio non sono garantiti dai setter: come invarianti richiedono precondizioni sui setter |

Il Lab sconsiglia controller, viste e classi di integrazione. Qui significa lasciare fuori le JSP, il
comportamento HTTP delle servlet (`doGet`, `doPost`), `StarControl`, che non ha logica propria, e
`LocalServer`, che non è codice di produzione. Le funzioni di validazione e `AuthFilter` stanno in classi
di controllo, ma sono le uniche implementazioni di "data validation" e "security checks" del progetto.

Note pratiche:

- Fuori da Tomcat il lookup JNDI nel blocco statico dei DAO fallisce e stampa `Error:` seguito dal
  messaggio dell'eccezione JNDI, come succede anche durante i test (C6).
- Tre metodi di `CartBean` hanno la lettera `à` nel nome (`incrementaQuantitàOrdinata`,
  `rimuoviSingolaQuantitàOrdinata`, `getQuantitàOrdinata`): i sorgenti sono in UTF-8
  (`project.build.sourceEncoding` nel `pom.xml`), quindi conviene indicare la codifica a OpenJML
  (`-encoding UTF-8`).
- Le funzioni di supporto delle servlet sono private o con visibilità di package (`scontoValido`,
  `prodottoDalForm`, `tipoImmagine`): in JML i loro contratti si scrivono con casi `private` o senza
  modificatore, secondo la visibilità del metodo.

## 12. Limiti dell'analisi

- La verifica dinamica comprende i test di unità, con il database simulato, e una prova
  dell'applicazione avviata su un database MariaDB 11.4.13 inizializzato con `ecommerce.sql`. La prova ha
  coperto:
  - catalogo e paginazione;
  - stelle e banner NEW, con la home eseguita in un browser headless;
  - immagini, scheda e ricerca;
  - carrello e giacenza, registrazione e login;
  - conferma dell'ordine, anche con giacenza insufficiente, e storico;
  - sconto e inserimento di un prodotto;
  - vincoli dello schema e script di aggiornamento.
- Del JavaScript di login e registrazione è stata controllata la sintassi, non il comportamento in un
  browser: la codifica dei parametri è stata provata inviando al server i valori codificati.
- Il comportamento con MySQL non è stato provato (L05).
- Le postcondizioni dei DAO riguardano lo stato del database: in JML si possono esprimere solo sui valori
  passati a JDBC o restituiti, come fanno i test con i mock.
