# Piano dei test

Elenco dei test di unità del progetto, tutti implementati in `src/test/java`. Il numero di ogni test
è all'inizio del suo `@DisplayName` e compare nella tabella dei risultati della pipeline.
La strategia di test è descritta in [testing.md](testing.md).

Nei test dei DAO il database viene sostituito da mock di `DataSource`, `Connection`, `PreparedStatement` e `ResultSet`.

Nei test di `UserControl`, `AuthFilter` e `AdminControl` la richiesta, la risposta e la sessione HTTP sono oggetti finti creati con Mockito, e i DAO sono mock.

## Come sono scelti i casi di test

I casi derivano da tecniche black-box:

- **Classi di equivalenza, criterio debole.** Le classi valide sono raccolte nel minor numero di casi;
  ogni caso contiene al massimo una classe non valida, così un errore non ne nasconde un altro
  (es. test 47: un solo campo vuoto per caso).
- **Valori limite.** Si provano l'ultimo valore accettato e il primo rifiutato: password di 8 e 20
  caratteri (41b) contro 7 e 21 (41), date di oggi e di domani (45b, 45), 0 e 1 righe cancellate (19).
- **Partizione per categorie.** Per ogni parametro si individuano categorie e scelte: per esempio
  0, 1 o più risultati, oppure la categoria `all` contro una categoria specifica. La connessione al
  database fallita è la scelta di errore: si prova una volta per ogni metodo dei DAO che chiude le
  risorse a mano (17, 17b, 26b, 33h, 38e). I metodi con try-with-resources sono esclusi, perché lì
  l'eccezione arriva al chiamante senza codice di chiusura che possa sbagliare.
- **Tabella di decisione.** Nei controlli di accesso (`AuthFilter`, `AdminControl`) le condizioni
  "area admin", "utente loggato" e "ruolo admin" stabiliscono se la richiesta prosegue o se l'utente
  viene reindirizzato al login. In `AdminControl` il ruolo viene controllato prima di leggere l'azione
  richiesta, che quindi è indifferente: per l'admin basta provare una sola azione.

I sotto-numeri (es. 9b, 9c) indicano test aggiunti accanto a quelli dello stesso metodo, senza rinumerare gli altri.

## Classi escluse dai test

| Classe | Motivo |
|--------|--------|
| Servlet: `CartControl`, `ProductControl`, `StarControl`, `GetPicturesControl` | Hanno poca logica propria: leggono i parametri, chiamano un DAO e passano la richiesta a una JSP. |
| Bean: `UserBean`, `OrdineBean`, `RecensioneBean` e i getter/setter di `ProductBean` | Contengono solo getter e setter, quindi i test sarebbero poco utili. `CartBean` invece viene testato perché contiene la logica del carrello; di `ProductBean` si testano `equals`, `hashCode` e `isEmpty` (test 10-10d). |
| `IMGmodel` | È interamente statico: per testarlo servirebbero mock statici, complessi da configurare e con poco beneficio. |
| Interfacce `*_intf` | Non contengono codice da eseguire. |
| `dev.LocalServer` | Serve solo ad avviare l'app in locale ed è escluso dal WAR, quindi non è codice di produzione. |
| JSP (es. `Cart.jsp`) | Non si possono testare con unit test Java. La formula del prezzo scontato che contengono è comunque verificata dal test 24 su `CartModel`. |

### Eccezioni tra i control

| Classe | Perché viene testata |
|--------|----------------------|
| `UserControl` | Contiene logica vera: registrazione, login con hash della password e gestione della sessione. I controlli sui dati del form sono fatti anche in JavaScript, ma una richiesta HTTP diretta li salta, quindi il server deve ripeterli. I validatori sono `private` e vengono verificati attraverso le richieste a `doPost`, senza cambiarne la visibilità. |
| `AuthFilter` | Protegge l'area `/admin` e la servlet `/Admin`: è un controllo di sicurezza. Un test verifica anche la sua mappatura in `web.xml`. |
| `AdminControl` | Esegue le azioni dell'amministratore. Si testa solo il controllo del ruolo, che la servlet ripete come difesa in profondità: un utente anonimo e uno non admin vengono respinti per ognuna delle tre azioni, mentre un admin supera il controllo (test 69). Le azioni in sé si limitano a passare i parametri al DAO, già testato. |

---

## 1. `CartBeanTest` (carrello ospite) e `ProductBeanTest`

| # | Test | Descrizione |
|---|------|-------------|
| 1 | `addProduct_prodottoNuovo_vieneAggiuntoAllaLista` | Dopo `addProduct` il carrello contiene il prodotto aggiunto. |
| 2a | `deleteProduct_idAssente_carrelloInvariato` | Se si rimuove un ID che non è nel carrello, il carrello resta invariato. |
| 2b | `deleteProduct_stessoIdPrezzoDiverso_prodottoRimosso` | Viene rimosso il prodotto con lo stesso ID anche se il prezzo è diverso, perché il confronto avviene solo per ID. |
| 3 | `getQuantitaOrdinata_prodottoPresenteOAssente_restituisceQuantitaOppureMenoUno` | Test parametrizzato: per un prodotto presente il metodo restituisce la quantità ordinata, per un prodotto assente restituisce −1. |
| 4 | `incrementa_prodottoIdentico_quantitaDaUnoADue` | Incrementando un prodotto già nel carrello, la sua quantità passa da 1 a 2. Il prodotto è il secondo del carrello e il primo non deve cambiare. |
| 5 | `decrementa_quantitaAlmenoDue_quantitaDiminuisceDiUno` | Test parametrizzato: decrementando un prodotto con quantità 3, oppure 2 (valore limite), la quantità diminuisce di 1. Il prodotto è il secondo del carrello e il primo non deve cambiare. |
| 6 | `decrementa_quantitaUno_prodottoRimosso` | Decrementando un prodotto con quantità 1, il prodotto viene rimosso dal carrello. |
| 7 | `incrementa_stessoIdMaScontoOMagazzinoCambiati_quantitaAumenta` | Il prodotto ricaricato dal DB ha lo stesso ID ma sconto o magazzino diversi: l'incremento deve funzionare comunque. |
| 8 | `decrementa_stessoIdMaScontoOMagazzinoCambiati_quantitaDiminuisce` | Come il test 7, ma per il decremento. |
| 9 | `decrementa_quantitaUno_prodottoRimossoNonVieneModificato` | Dopo la rimozione, il prodotto rimosso conserva la quantità 1: l'oggetto non deve essere modificato. |
| 9b | `contiene_scontoCambiato_veroSoloPerLoStessoId` | Test parametrizzato: `contiene()` riconosce dal suo ID un prodotto già nel carrello, anche se lo sconto è cambiato, e restituisce `false` per un ID diverso. |
| 9c | `incrementa_prodottoAssente_carrelloInvariato` | Incrementando un prodotto che non è nel carrello, il carrello resta invariato: nessun prodotto viene aggiunto o modificato. |
| 9d | `decrementa_prodottoAssente_carrelloInvariato` | Decrementando un prodotto che non è nel carrello, il carrello resta invariato: nessun prodotto viene rimosso o modificato. |
| 10 | `productBean_oggettiUguali_stessoHashCode` | Due `ProductBean` uguali secondo `equals()` hanno lo stesso `hashCode()`. |
| 10b | `equals_unSoloCampoDiverso_prodottiDiversi` | Test parametrizzato sui 10 campi confrontati da `equals()`: due prodotti che differiscono in un solo campo non sono uguali. |
| 10c | `equals_nullOAltraClasse_false` | `equals()` restituisce `false` se l'altro oggetto è `null` o appartiene a un'altra classe. |
| 10d | `isEmpty_unSoloCampoValorizzato_false` | Test parametrizzato sui 10 campi: un bean appena creato è vuoto, ma basta valorizzare un solo campo perché `isEmpty()` restituisca `false`. |

## 2. `ProductModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 11 | `doRetrieveByKey_idEsistente_beanPopolato` | `doRetrieveByKey` passa l'id come parametro della query e copia nel bean i campi letti dal `ResultSet`. |
| 12 | `doRetrieveByKey_idInesistente_beanVuoto` | Se la query non trova righe, `doRetrieveByKey` restituisce un bean vuoto (`isEmpty()`). |
| 13 | `doRetrieveByKey_prezzoDecimale_prezzoNonTroncato` | Un prezzo di 12,99 e una grammatura decimale vengono copiati nel bean senza perdere i decimali. |
| 14 | `doRetrieveByKey_sqlException_risorseChiuse` | Se la query fallisce, `PreparedStatement` e `Connection` vengono chiusi comunque. |
| 15 | `doRetrieveAll_limitOffset_parametriImpostatiERisultati` | `doRetrieveAll` passa `limit` e `offset` come parametri e restituisce tutti i prodotti letti, con le votazioni e con i prezzi decimali corretti. |
| 15b | `doRetrieveAll_orderNulloVuotoOIndicato_orderBySoloSeIndicato` | Test parametrizzato: la query contiene `ORDER BY` solo se `order` è indicato, e non quando è `null` o vuoto. |
| 16 | `doRetrieveByInput_zeroUnoOPiuRisultati_jsonCorretto` | `doRetrieveByInput` costruisce un JSON valido con 0, 1 e più risultati, senza virgola dopo l'ultimo elemento. |
| 17 | `doRetrieveByInput_connessioneFallita_lanciaSQLException` | Se `getConnection()` fallisce, `doRetrieveByInput` propaga la `SQLException` invece di lanciare una `NullPointerException`. |
| 17b | `altriMetodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sugli altri quattro metodi che chiudono le risorse a mano (`doDelete` usa try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |
| 18 | `doRetrieveByCat_categoriaMalevola_nessunaSqlInjection` | Con la categoria `x' OR '1'='1` il testo iniettato non compare nella query SQL, perché la categoria viene passata come parametro. |
| 18b | `doRetrieveByCat_categoriaAll_tuttiIProdottiConVotazioni` | Con la categoria `all`, che l'app usa per mostrare l'intero catalogo, la query non filtra per categoria, ordina per ID e restituisce i prodotti con le loro votazioni. |
| 18c | `doRetrieveBySubCat_categoriaESottocategoriaMalevole_nessunaSqlInjection` | Come il test 18, ma per la categoria e la sottocategoria di `doRetrieveBySubCat`. |
| 18d | `doRetrieveBySubCat_categoriaAll_tuttiIProdottiConVotazioni` | Come il test 18b, ma per `doRetrieveBySubCat`. |
| 19 | `doDelete_zeroOUnaRigaCancellata_trueSoloSeCancellaEChiudeConnessione` | Test parametrizzato con i valori limite 0 e 1: `doDelete` restituisce `true` solo se cancella almeno una riga, e in entrambi i casi chiude la connessione. |

## 3. `CartModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 20 | `addProduct_giaPresente_eseguiUpdate` | Se il prodotto è già nel carrello, `addProduct` esegue l'`UPDATE` della quantità con 4 parametri. |
| 21 | `addProduct_nuovo_eseguiInsertConQuantita` | Se il prodotto è nuovo, `addProduct` esegue l'`INSERT` con la quantità indicata. |
| 22 | `deleteProduct_ultimoONonUltimo_deleteOppureUpdate` | Se `isUltimo` è `true`, `deleteProduct` esegue un `DELETE`; altrimenti esegue un `UPDATE` che toglie 1 alla quantità. |
| 23 | `getProducts_righePresenti_prodottiMappati` | `getProducts` copia nei bean prezzo, quantità ordinata e gli altri campi di ogni riga. |
| 23b | `getProducts_nessunaRiga_listaVuota` | Se il carrello è vuoto, `getProducts` restituisce una lista vuota. |
| 24 | `confermaOrdine_scontiDiversi_totaleEPrezziCorretti` | Test parametrizzato con sconto dello 0%, del 50% e del 100% e con più prodotti e quantità: verifica i prezzi fatturati e il totale passati al DB. |
| 25 | `confermaOrdine_carrelloVuoto_totaleZeroENuovoCarrello` | Con il carrello vuoto, `confermaOrdine` salva un totale di 0 e crea comunque il nuovo carrello. |
| 26 | `confermaOrdine_erroreSecondoPasso_nessunaModificaParziale` | Se la conferma dell'ordine fallisce, i prezzi già scritti vengono annullati con il rollback della transazione. |
| 26b | `metodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sui tre metodi che chiudono le risorse a mano (`confermaOrdine` usa try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |

## 4. `UserModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 27 | `addUser_utenteValido_parametriInOrdine` | `AddUser` passa i 9 campi dell'utente nell'ordine giusto, con `permessi_admin = "0"`. |
| 28 | `addUser_utenteValido_carrelloCreatoConIdUtente` | `AddUser` crea il carrello associato all'id dell'utente appena inserito. |
| 29 | `addUser_creazioneCarrelloFallita_utenteNonSalvato` | Se la creazione del carrello fallisce, l'inserimento dell'utente viene annullato. |
| 30 | `getUserTramiteCredenziali_credenzialiCorrette_utentePopolato` | Con credenziali corrette, `getUserTramiteCredenziali` restituisce l'utente con id, id del carrello e flag admin. |
| 31 | `getUserTramiteCredenziali_credenzialiErrate_beanVuoto` | Con credenziali errate, `getUserTramiteCredenziali` restituisce un bean vuoto e non `null`. |
| 32a | `getIdCart_trovatoONonTrovato_idOppureMenoUno` | Test parametrizzato: `getIdCart` cerca il carrello con username e password e ne restituisce l'id, oppure −1 se il carrello non esiste. |
| 32b | `getIdCartByUserId_trovatoONonTrovato_idOppureMenoUno` | Test parametrizzato: `getIdCartByUserId` cerca il carrello con l'id dell'utente e ne restituisce l'id, oppure −1 se il carrello non esiste. |
| 33 | `getAllOrdine_ordiniPresentiOAssenti_listaIdOppureVuota` | `getAllOrdine` restituisce gli id degli ordini dell'utente, oppure una lista vuota se non ce ne sono. |
| 33b | `getUserTramiteID_trovato_utentePopolato` | `getUserTramiteID` copia nel bean i dati dell'utente con l'id indicato; l'id del carrello resta −1. |
| 33c | `getUserTramiteID_nonTrovato_beanVuoto` | Con un id inesistente, `getUserTramiteID` restituisce un bean vuoto e non `null`. |
| 33d | `getUtenti_piuUtenti_utentiMappati` | `getUtenti` restituisce tutti gli utenti, ciascuno con il proprio ruolo. |
| 33e | `getUtenti_nessunUtente_listaVuota` | Se non ci sono utenti, `getUtenti` restituisce una lista vuota. |
| 33f | `getOrdineById_ordineDellUtente_ordineEProdottiMappati` | `getOrdineById` cerca l'ordine usando sia l'id dell'utente sia quello dell'ordine, e ne copia i dati e i prodotti, compreso il prezzo pagato al momento dell'acquisto. |
| 33g | `getOrdineById_ordineNonTrovato_ordineVuoto` | Se l'ordine non esiste o appartiene a un altro utente, `getOrdineById` restituisce un ordine vuoto, senza prodotti. |
| 33h | `metodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sui sette metodi che chiudono le risorse a mano (`AddUser` usa try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |

## 5. `AzioniAdminModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 34a | `inserisciProdotto_prodottoValido_dieciCampiImpostati` | `inserisciProdotto` passa al DB i 10 campi del prodotto, ciascuno nella posizione giusta. |
| 34b | `inserisciProdotto_esitoUpdate_trueOppureFalse` | Test parametrizzato: `inserisciProdotto` restituisce `true` se `executeUpdate` è maggiore di 0, altrimenti `false`. |
| 35 | `modificaSconto_parametri_scontoEIdImpostati` | `modificaSconto` passa al DB la percentuale di sconto e l'id del prodotto. |
| 36a | `inserisciProdotto_dopoEsecuzione_risorseChiuse` | Dopo `inserisciProdotto`, `PreparedStatement` e `Connection` risultano chiusi. |
| 36b | `modificaSconto_dopoEsecuzione_risorseChiuse` | Dopo `modificaSconto`, `PreparedStatement` e `Connection` risultano chiusi. |

## 6. `RecensioneModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 37a | `addRecensione_parametriCorretti` | `addRecensione` passa al DB utente, prodotto, punteggio, commento e la data di oggi. |
| 37b | `modificaRecensione_parametriCorretti` | `modificaRecensione` passa al DB punteggio, commento e id della recensione. |
| 38a | `getAllRecensioni_righePresenti_recensioniMappate` | `getAllRecensioni` copia nei bean i dati di ogni recensione del prodotto. |
| 38b | `getAllRecensioni_nessunaRiga_listaVuota` | Se il prodotto non ha recensioni, `getAllRecensioni` restituisce una lista vuota. |
| 38c | `getRecensione_trovata_recensioneMappata` | `getRecensione` copia nel bean la recensione che l'utente ha scritto sul prodotto. |
| 38d | `getRecensione_nonTrovata_beanVuoto` | Se l'utente non ha recensito il prodotto, `getRecensione` restituisce un bean vuoto (`isEmpty()`) e non `null`. |
| 38e | `metodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sui quattro metodi del DAO: se `getConnection()` fallisce, ognuno propaga la `SQLException`. |

## 7. `UserControlTest`

### Registrazione (`action=register`)

| # | Test | Descrizione |
|---|------|-------------|
| 39 | `register_datiValidi_utenteSalvatoConPasswordCifrata` | Con dati validi l'utente viene salvato una sola volta, con la password cifrata in SHA-256 e mai in chiaro. La risposta è `val 1`. |
| 40 | `register_carrelloOspitePieno_prodottiCopiatiNelNuovoCarrello` | I prodotti del carrello dell'ospite vengono copiati nel carrello del nuovo utente, con le stesse quantità. |
| 40b | `register_carrelloNonCopiabile_nessunProdottoCopiato` | Test parametrizzato: se il carrello dell'ospite è vuoto, o se il carrello del nuovo utente non viene trovato, nessun prodotto viene copiato, ma la registrazione riesce comunque. |
| 41 | `register_passwordDebole_registrazioneRifiutata` | Test parametrizzato: vengono rifiutate le password troppo corte (7 caratteri), troppo lunghe (21) o prive di maiuscola, minuscola, cifra o carattere speciale. La risposta è `val 0` e l'utente non viene salvato. |
| 41b | `register_passwordAiLimitiDiLunghezza_registrazioneAccettata` | Test parametrizzato con i valori limite validi: le password di 8 e di 20 caratteri vengono accettate. |
| 42 | `register_passwordVuota_registrazioneRifiutata` | Una password vuota viene rifiutata, anche se il suo hash non è vuoto. |
| 43 | `register_emailNonValida_registrazioneRifiutata` | Un'email in formato non valido viene rifiutata. |
| 44 | `register_telefonoNonValido_registrazioneRifiutata` | Test parametrizzato: viene rifiutato un telefono con lettere, con 9 cifre o con 11 cifre (i valori limite attorno alle 10 richieste). |
| 45 | `register_dataNascitaFuturaOOltreCentoAnni_registrazioneRifiutata` | Test parametrizzato con i valori limite: viene rifiutata una data di nascita di domani o di esattamente 100 anni fa. |
| 45b | `register_dataNascitaAlLimite_registrazioneAccettata` | Test parametrizzato con i valori limite validi: viene accettata una data di nascita di oggi o di 100 anni fa più un giorno. |
| 46 | `register_dataNascitaNonValida_rispostaDiErrore` | Una data in formato sbagliato (`10/05/2000`) produce la risposta `val 0`, senza che la richiesta fallisca con un'eccezione. |
| 47 | `register_campoObbligatorioVuoto_registrazioneRifiutata` | Test parametrizzato: la registrazione viene rifiutata se un campo obbligatorio è vuoto, se il nome manca (`null`) o se il nome contiene solo spazi. |
| 47b | `register_formatoNomeCognomeOUsernameNonValido_registrazioneRifiutata` | Test parametrizzato: la registrazione viene rifiutata se il nome o il cognome contengono una cifra, o se lo username contiene un trattino basso. |
| 48 | `register_erroreDatabase_rispostaDiErrore` | Se il salvataggio nel DB fallisce, la risposta è `val 0` e non `val 1`. |

### Login, dati utente e logout

| # | Test | Descrizione |
|---|------|-------------|
| 49 | `login_credenzialiCorrette_sessioneImpostata` | Test parametrizzato per un utente normale e per un admin: dopo il login la sessione contiene username, id, carrello, indirizzo e ruolo admin. La risposta è `val 1`. |
| 50 | `login_passwordErrata_accessoNegato` | Con credenziali errate la risposta è `val 0` e la sessione non contiene alcun utente. |
| 51 | `login_formatoNonValido_nessunAccessoAlDatabase` | Test parametrizzato: con uno username non alfanumerico o una password non conforme il DB non viene interrogato e l'utente torna alla pagina di login. |
| 52 | `login_parametriMancanti_nessunaEccezione` | Senza username e password la richiesta non fallisce con un'eccezione e il DB non viene interrogato. |
| 53 | `login_sessioneGiaAttiva_datiNellaNuovaSessione` | Se un utente era già loggato, la vecchia sessione viene invalidata e i dati del nuovo login vengono salvati nella nuova sessione. |
| 54 | `login_erroreDatabase_redirectPaginaErrore` | Se il DB lancia un'eccezione, l'utente viene reindirizzato a `Error.jsp`. |
| 55 | `datiUtente_utenteLoggato_forwardAreaUtente` | I dati dell'utente loggato vengono letti dal DB e passati ad `AreaUtente.jsp`. |
| 56 | `datiUtente_utenteNonLoggato_nessunaEccezione` | Senza login la richiesta non fallisce con un'eccezione e il DB non viene interrogato. |
| 57 | `logout_sessioneAttiva_sessioneInvalidataEForwardLogin` | Il logout, anche via GET, invalida la sessione e riporta l'utente alla pagina di login. |
| 58 | `azioneSconosciuta_forwardHome` | Con un'azione sconosciuta l'utente viene riportato alla home. |

## 8. `AuthFilterTest`

| # | Test | Descrizione |
|---|------|-------------|
| 59 | `doFilter_paginaPubblica_richiestaInoltrata` | Una pagina fuori da `/admin` viene servita senza redirect. |
| 60 | `doFilter_adminSenzaLogin_redirectAlLogin` | Senza login, una pagina di `/admin` non viene servita e l'utente viene reindirizzato alla pagina di login. |
| 61 | `doFilter_utenteNonAdmin_redirectAlLogin` | Un utente loggato ma non admin viene reindirizzato alla pagina di login. |
| 62 | `doFilter_utenteAdmin_richiestaInoltrata` | Un admin accede alla pagina di `/admin`. |
| 63 | `doFilter_contextPathNonVuoto_redirectConContextPath` | Se l'app è pubblicata sotto un percorso (es. `/shop`), il redirect al login mantiene lo stesso percorso. |
| 63b | `doFilter_adminNelPathInfoSenzaLogin_redirectAlLogin` | Se l'area admin compare solo nel path info (servlet path vuoto), un utente senza login viene comunque reindirizzato al login. |
| 64 | `doFilter_servletAdminSenzaLogin_redirectAlLogin` | Senza login, una richiesta alla servlet `/Admin` non viene eseguita e l'utente viene reindirizzato alla pagina di login. |
| 65 | `doFilter_servletAdminUtenteNonAdmin_redirectAlLogin` | Un utente loggato ma non admin non può usare la servlet `/Admin`. |
| 66 | `doFilter_servletAdminUtenteAdmin_richiestaInoltrata` | Un admin può usare la servlet `/Admin`. |
| 67 | `webXml_servletAdmin_coperteDaAuthFilter` | In `web.xml` ogni URL della servlet `Admin` è coperto dalla mappatura di `AuthFilter`. |

## 9. `AdminControlTest`

| # | Test | Descrizione |
|---|------|-------------|
| 68a | `azioneAdmin_utenteAnonimo_nessunaModificaERedirect` | Test parametrizzato sulle tre azioni: senza login non viene chiamato nessun DAO e l'utente viene reindirizzato alla pagina di login. |
| 68b | `azioneAdmin_utenteNonAdmin_nessunaModificaERedirect` | Come il test 68a, ma con un utente loggato che non è admin. |
| 69 | `azioneAdmin_utenteAdmin_azioneEseguita` | Con il ruolo admin, l'azione `impostaSconto` salva lo sconto e mostra di nuovo la pagina dei prodotti. |

---

## Criteri di adeguatezza

| Criterio | Come si verifica |
|----------|------------------|
| Ogni classe di equivalenza, valida o non valida, ha almeno un caso di test; per gli intervalli si provano i valori limite, sia dentro sia fuori. | Con le tabelle di questo piano. |
| Copertura strutturale: per ogni package almeno l'80% delle righe, il 75% dei branch e il 100% delle classi. | Con JaCoCo: `./mvnw verify` fallisce se una soglia non è raggiunta. Dal controllo sono escluse le classi non testate, elencate sopra, e il package `control`. |
| Mutation testing: almeno l'80% dei mutanti uccisi. | Con PIT, avviato a mano: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`. Le classi analizzate sono `Model.*` e `AzioniAdminModel`. |

## Strategia di regressione

A ogni modifica si riesegue l'intera suite (*retest-all*): `ci.yml` la lancia a ogni push e pull request
verso `main`. I 179 casi richiedono pochi secondi, quindi non serve selezionarne un sottoinsieme. Se un
test fallisce, la pipeline non produce né il WAR né l'immagine Docker.

## Risultati

### Copertura

| Package | Righe prima → dopo | Branch prima → dopo |
|---------|--------------------|---------------------|
| `Model` (i quattro DAO) | 69% → 96% | 47% → 90% |
| `Model.Bean` (`CartBean`, `ProductBean`) | 89% → 95% | 65% → 100% |
| `admin` (`AzioniAdminModel`) | 90% → 90% | 75% → 75% |

"Prima" indica la suite di 100 casi, "dopo" quella di 179 casi descritta in questo piano. Prima le soglie
non erano raggiunte nei package `Model` e `Model.Bean`; ora `./mvnw verify` le rispetta tutte.

Alcune classi sono testate ma restano fuori dal controllo delle soglie: `UserControl` (righe 87%,
branch 68%, a causa del codice morto descritto sotto), `AuthFilter` (righe 82%, branch 100%) e
`AdminControl`, di cui si testa solo il controllo del ruolo.

### Verifica dei test con difetti inseriti

Per controllare che i casi nuovi trovino davvero degli errori, e non si limitino a eseguire il codice,
abbiamo inserito a mano cinque difetti nel codice applicativo, ciascuno in una classe diversa, e
rieseguito i test. Il codice è stato poi ripristinato.

| Difetto inserito | Test che lo scopre |
|------------------|--------------------|
| `incrementaQuantitàOrdinata` modifica sempre il primo prodotto del carrello | 4, 9c |
| `equals` di `ProductBean` non confronta la colorazione | 10b |
| `doDelete` restituisce sempre `false` | 19 (caso con 1 riga) |
| La password di 8 caratteri viene rifiutata (`> 8` invece di `>= 8`) | 41b |
| `AuthFilter` ignora il path info | 63b |

Ogni difetto è stato scoperto solo da casi nuovi o modificati con queste tecniche. I test 4 e 19
esistevano già, ma il 4 usava un carrello con un solo prodotto e il 19 provava solo il caso di 0 righe.
Tutti gli altri test sono passati: con la suite precedente nessuno dei cinque difetti sarebbe stato scoperto.

## Rami non coperti

Con questi test tutti i package controllati da JaCoCo superano le soglie (righe 80%, branch 75%).
I rami che restano scoperti non sono raggiungibili, oppure non producono un effetto da verificare:

| Classe | Rami | Motivo |
|--------|------|--------|
| `UserControl` | `emptyUser` e metà di `Validate` | `emptyUser` non viene mai chiamato e `Validate` ricontrolla campi già verificati da `datiRegistrazioneValidi`. È codice morto, e il codice applicativo non viene modificato per i test. |
| `CartModel` | `rs != null` in `addProduct` e `deleteProduct` | `rs` non viene mai assegnato, quindi la condizione è sempre falsa. |
| `RecensioneModel` | il caso `connection == null` e il caso con 0 righe modificate | `getConnection()` non restituisce mai `null`: in caso di problemi lancia un'eccezione. Con 0 righe il metodo stampa solo un messaggio, quindi non c'è un risultato da verificare. |
| `AzioniAdminModel` | il caso `rowsUpdated < 1` | Il blocco dell'`if` è vuoto, quindi non c'è nulla da verificare. |
| `ProductModel`, `UserModel` | la chiusura delle risorse della seconda query in caso di errore | Servirebbe una connessione che funziona per la prima query e fallisce alla seconda. La connessione fallita è già provata una volta per metodo (17b, 33h). |

---
