# Piano dei test

Elenco dei test di unità del progetto, tutti implementati in `src/test/java`. Il numero di ogni test
è all'inizio del suo `@DisplayName` e compare nella tabella dei risultati della pipeline.
La strategia di test è descritta in [testing.md](testing.md).

Nei test dei DAO il database viene sostituito da mock di `DataSource`, `Connection`, `PreparedStatement` e `ResultSet`.

Nei test di `UserControl` e `AuthFilter` richiesta, risposta e sessione HTTP sono oggetti finti creati con Mockito, e i DAO sono mock.

## Classi escluse dai test

| Classe | Motivo |
|--------|--------|
| Servlet: `CartControl`, `ProductControl`, `StarControl`, `AdminControl`, `GetPicturesControl` | Hanno poca logica propria: leggono i parametri, chiamano un DAO e passano la richiesta a una JSP. |
| Bean: `UserBean`, `OrdineBean`, `RecensioneBean` e `ProductBean` (eccetto il test 10) | Contengono solo getter e setter, i test sarebbero poco utili. `CartBean` invece lo testiamo siccome ha metodi che gestiscono la logica del carrello. |
| `IMGmodel` | È interamente statico: per testarlo servirebbero mock statici, complessi da configurare e con poco beneficio. |
| Interfacce `*_intf` | Non contengono codice da eseguire. |
| `dev.LocalServer` | Serve solo ad avviare l'app in locale ed è escluso dal WAR, quindi non è codice di produzione. |
| JSP (es. `Cart.jsp`) | Non si possono testare con unit test Java. La formula del prezzo scontato che contengono è comunque verificata dal test 24 su `CartModel`. |

### Eccezioni tra i control

| Classe | Perché viene testata |
|--------|----------------------|
| `UserControl` | Contiene logica vera: registrazione, login con hash della password e gestione della sessione. I controlli sui dati del form sono fatti anche in JavaScript, ma una richiesta HTTP diretta li salta: il server deve ripeterli. I validatori sono `private`, quindi vengono verificati attraverso le richieste a `doPost`, senza cambiarne la visibilità. |
| `AuthFilter` | Protegge l'area `/admin`: è un controllo di sicurezza. |

---

## 1. `CartBeanTest` (carrello ospite)

| # | Test | Descrizione |
|---|------|-------------|
| 1 | `addProduct_prodottoNuovo_vieneAggiuntoAllaLista` | Dopo l'aggiunta, il carrello contiene il prodotto. |
| 2a | `deleteProduct_idAssente_carrelloInvariato` | Se l'ID non è nel carrello, il carrello resta invariato. |
| 2b | `deleteProduct_stessoIdPrezzoDiverso_prodottoRimosso` | Rimuove il prodotto con lo stesso ID, anche se il prezzo è diverso: il confronto avviene solo per ID. |
| 3 | `getQuantitaOrdinata_prodottoPresenteOAssente_restituisceQuantitaOppureMenoUno` | Restituisce la quantità ordinata del prodotto, oppure −1 se il prodotto non è nel carrello. |
| 4 | `incrementa_prodottoIdentico_quantitaDaUnoADue` | Incrementare un prodotto già nel carrello porta la quantità da 1 a 2. |
| 5 | `decrementa_quantitaTre_quantitaDiventaDue` | Decrementare un prodotto con quantità 3 la porta a 2. |
| 6 | `decrementa_quantitaUno_prodottoRimosso` | Decrementare un prodotto con quantità 1 lo rimuove dal carrello. |
| 7 | `incrementa_stessoIdMaScontoOMagazzinoCambiati_quantitaAumenta` | Il prodotto ricaricato dal DB ha lo stesso ID ma sconto o magazzino diversi. L'incremento deve funzionare comunque. |
| 8 | `decrementa_stessoIdMaScontoOMagazzinoCambiati_quantitaDiminuisce` | Come il test 7, ma per il decremento. |
| 9 | `decrementa_quantitaUno_prodottoRimossoNonVieneModificato` | Dopo la rimozione, l'oggetto rimosso deve mantenere la quantità 1 e non va modificato. |
| 9b | `contiene_stessoIdMaScontoCambiato_restituisceTrue` | `contiene()` riconosce per ID un prodotto già nel carrello, anche se lo sconto è cambiato. |
| 10 | `productBean_oggettiUguali_stessoHashCode` | Due `ProductBean` uguali secondo `equals()` devono avere lo stesso `hashCode()`. |

## 2. `ProductModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 11 | `doRetrieveByKey_idEsistente_beanPopolato` | Passa l'id come parametro e copia i campi del `ResultSet` nel bean. |
| 12 | `doRetrieveByKey_idInesistente_beanVuoto` | Se la query non trova righe, restituisce un bean vuoto (`isEmpty()`). |
| 13 | `doRetrieveByKey_prezzoDecimale_prezzoNonTroncato` | Un prezzo di 12,99 (e una grammatura decimale) deve restare 12,99 nel bean, senza perdere i decimali. |
| 14 | `doRetrieveByKey_sqlException_risorseChiuse` | Se la query fallisce, `PreparedStatement` e `Connection` vengono chiusi comunque. |
| 15 | `doRetrieveAll_limitOffset_parametriImpostatiERisultati` | Passa `limit` e `offset` come parametri e restituisce tutti gli N prodotti letti. |
| 16 | `doRetrieveByInput_zeroUnoOPiuRisultati_jsonCorretto` | Costruisce un JSON valido con 0, 1 e più risultati, controllando la rimozione dell'ultima virgola. |
| 17 | `doRetrieveByInput_connessioneFallita_lanciaSQLException` | Se `getConnection()` fallisce, il metodo deve propagare `SQLException`, non una `NullPointerException`. |
| 18 | `doRetrieveByCat_categoriaMalevola_nessunaSqlInjection` | Con la categoria `x' OR '1'='1`, la query SQL non deve contenere il testo iniettato. |
| 19 | `doDelete_nessunaRigaCancellata_restituisceFalseEChiudeConnessione` | Se `executeUpdate` non cancella righe, il metodo restituisce `false` e chiude comunque la connessione. |

## 3. `CartModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 20 | `addProduct_giaPresente_eseguiUpdate` | Con un prodotto già nel carrello esegue l'`UPDATE` della quantità, con 4 parametri. |
| 21 | `addProduct_nuovo_eseguiInsertConQuantita` | Con un prodotto nuovo esegue l'`INSERT`, passando la quantità indicata. |
| 22 | `deleteProduct_ultimoONonUltimo_deleteOppureUpdate` | Se `isUltimo` è true esegue `DELETE`, altrimenti esegue un `UPDATE` che toglie 1 alla quantità. |
| 23 | `getProducts_righePresenti_prodottiMappati` | Copia nei bean prezzo, quantità ordinata e gli altri campi di ogni riga. |
| 24 | `confermaOrdine_scontiDiversi_totaleEPrezziCorretti` | Test parametrizzato con sconto 0, 50 e 100 e con più prodotti e quantità. Verifica il prezzo fatturato e il totale passati al DB. |
| 25 | `confermaOrdine_carrelloVuoto_totaleZeroENuovoCarrello` | Con il carrello vuoto il totale è 0 e viene creato comunque il nuovo carrello. |
| 26 | `confermaOrdine_erroreSecondoPasso_nessunaModificaParziale` | Se la conferma dell'ordine fallisce, i prezzi già scritti vengono annullati (transazione con rollback). |

## 4. `UserModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 27 | `addUser_utenteValido_parametriInOrdine` | Passa i 9 campi dell'utente nell'ordine giusto, con `permessi_admin = "0"`. |
| 28 | `addUser_utenteValido_carrelloCreatoConIdUtente` | Crea il carrello associato all'id dell'utente appena inserito. |
| 29 | `addUser_creazioneCarrelloFallita_utenteNonSalvato` | Se la creazione del carrello fallisce, l'inserimento dell'utente viene annullato. |
| 30 | `getUserTramiteCredenziali_credenzialiCorrette_utentePopolato` | Restituisce l'utente con id, cartID e flag admin. |
| 31 | `getUserTramiteCredenziali_credenzialiErrate_beanVuoto` | Con credenziali errate restituisce un bean vuoto (non `null`). |
| 32a | `getIdCart_trovatoONonTrovato_idOppureMenoUno` | Test parametrizzato: `getIdCart` cerca il carrello con username e password e ne restituisce l'id, oppure −1 se non esiste. |
| 32b | `getIdCartByUserId_trovatoONonTrovato_idOppureMenoUno` | Test parametrizzato: `getIdCartByUserId` cerca il carrello con l'id dell'utente e ne restituisce l'id, oppure −1 se non esiste. |
| 33 | `getAllOrdine_ordiniPresentiOAssenti_listaIdOppureVuota` | Restituisce gli id degli ordini dell'utente, oppure una lista vuota. |

## 5. `AzioniAdminModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 34a | `inserisciProdotto_prodottoValido_dieciCampiImpostati` | Passa al DB i 10 campi del prodotto, ciascuno nella posizione giusta. |
| 34b | `inserisciProdotto_esitoUpdate_trueOppureFalse` | Test parametrizzato: restituisce `true` se `executeUpdate` è maggiore di 0, altrimenti `false`. |
| 35 | `modificaSconto_parametri_scontoEIdImpostati` | Passa al DB la percentuale di sconto e l'id del prodotto. |
| 36a | `inserisciProdotto_dopoEsecuzione_risorseChiuse` | Dopo `inserisciProdotto`, `PreparedStatement` e `Connection` sono chiusi. |
| 36b | `modificaSconto_dopoEsecuzione_risorseChiuse` | Dopo `modificaSconto`, `PreparedStatement` e `Connection` sono chiusi. |

## 6. `RecensioneModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 37a | `addRecensione_parametriCorretti` | Passa al DB utente, prodotto, punteggio, commento e data di oggi. |
| 37b | `modificaRecensione_parametriCorretti` | Passa al DB punteggio, commento e id della recensione. |
| 38a | `getAllRecensioni_righePresenti_recensioniMappate` | Copia nei bean i dati di ogni recensione del prodotto. |
| 38b | `getAllRecensioni_nessunaRiga_listaVuota` | Senza recensioni restituisce una lista vuota. |
| 38c | `getRecensione_trovata_recensioneMappata` | Copia nel bean la recensione dell'utente sul prodotto. |
| 38d | `getRecensione_nonTrovata_beanVuoto` | Senza recensione restituisce un bean vuoto (`isEmpty()`), non `null`. |

## 7. `UserControlTest`

### Registrazione (`action=register`)

| # | Test | Descrizione |
|---|------|-------------|
| 39 | `register_datiValidi_utenteSalvatoConPasswordCifrata` | Con dati validi l'utente viene salvato una volta, con la password cifrata in SHA-256 e mai in chiaro. Risposta `val 1`. |
| 40 | `register_carrelloOspitePieno_prodottiCopiatiNelNuovoCarrello` | I prodotti del carrello dell'ospite vengono copiati nel carrello del nuovo utente, con le stesse quantità. |
| 41 | `register_passwordDebole_registrazioneRifiutata` | Test parametrizzato: password troppo corta (7 caratteri), troppo lunga (21), senza maiuscola, minuscola, cifra o carattere speciale. Risposta `val 0`, utente non salvato. |
| 42 | `register_passwordVuota_registrazioneRifiutata` | Una password vuota viene rifiutata, anche se il suo hash non è vuoto. |
| 43 | `register_emailNonValida_registrazioneRifiutata` | Un'email senza formato valido viene rifiutata. |
| 44 | `register_telefonoNonValido_registrazioneRifiutata` | Un numero di telefono non valido viene rifiutato. |
| 45 | `register_dataNascitaFuturaOOltreCentoAnni_registrazioneRifiutata` | Test parametrizzato: una data di nascita futura o di più di 100 anni fa viene rifiutata. |
| 46 | `register_dataNascitaNonValida_rispostaDiErrore` | Una data in formato sbagliato (`10/05/2000`) produce `val 0`, senza far fallire la richiesta con un'eccezione. |
| 47 | `register_campoObbligatorioVuoto_registrazioneRifiutata` | Con il nome vuoto la registrazione viene rifiutata. |
| 48 | `register_erroreDatabase_rispostaDiErrore` | Se il salvataggio nel DB fallisce, la risposta è `val 0` e non `val 1`. |

### Login, dati utente e logout

| # | Test | Descrizione |
|---|------|-------------|
| 49 | `login_credenzialiCorrette_sessioneImpostata` | Test parametrizzato per utente normale e admin: la sessione riceve username, id, carrello, indirizzo e ruolo admin. Risposta `val 1`. |
| 50 | `login_passwordErrata_accessoNegato` | Con credenziali errate la risposta è `val 0` e la sessione resta senza utente. |
| 51 | `login_formatoNonValido_nessunAccessoAlDatabase` | Test parametrizzato: con uno username non alfanumerico o una password non conforme il DB non viene interrogato e si torna alla pagina di login. |
| 52 | `login_parametriMancanti_nessunaEccezione` | Senza username e password la richiesta non fallisce con un'eccezione e il DB non viene interrogato. |
| 53 | `login_sessioneGiaAttiva_datiNellaNuovaSessione` | Se un utente era già loggato, la vecchia sessione viene invalidata e i dati del nuovo login finiscono nella nuova sessione. |
| 54 | `login_erroreDatabase_redirectPaginaErrore` | Se il DB lancia un'eccezione, l'utente viene rimandato a `Error.jsp`. |
| 55 | `datiUtente_utenteLoggato_forwardAreaUtente` | I dati dell'utente loggato vengono letti dal DB e passati ad `AreaUtente.jsp`. |
| 56 | `datiUtente_utenteNonLoggato_nessunaEccezione` | Senza login la richiesta non fallisce con un'eccezione e il DB non viene interrogato. |
| 57 | `logout_sessioneAttiva_sessioneInvalidataEForwardLogin` | Il logout (anche via GET) invalida la sessione e porta alla pagina di login. |
| 58 | `azioneSconosciuta_forwardHome` | Con un'azione sconosciuta si viene portati alla home. |

## 8. `AuthFilterTest`

| # | Test | Descrizione |
|---|------|-------------|
| 59 | `doFilter_paginaPubblica_richiestaInoltrata` | Una pagina fuori da `/admin` passa senza redirect. |
| 60 | `doFilter_adminSenzaLogin_redirectAlLogin` | Senza login, una pagina `/admin` porta alla pagina di login e non viene servita. |
| 61 | `doFilter_utenteNonAdmin_redirectAlLogin` | Un utente loggato ma non admin viene rimandato alla pagina di login. |
| 62 | `doFilter_utenteAdmin_richiestaInoltrata` | Un admin accede alla pagina `/admin`. |
| 63 | `doFilter_contextPathNonVuoto_redirectConContextPath` | Con l'app pubblicata sotto un percorso (es. `/shop`), il redirect al login usa lo stesso percorso. |

---

