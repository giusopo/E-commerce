# Piano dei test

Elenco dei test di unità del progetto, tutti implementati in `src/test/java`. Il numero di ogni test
è all'inizio del suo `@DisplayName` e compare nella tabella dei risultati della pipeline.
La strategia di test è descritta in [testing.md](testing.md).

Nei test dei DAO il database viene sostituito da mock di `DataSource`, `Connection`, `PreparedStatement` e `ResultSet`.

Nei test di `UserControl`, `AuthFilter` e `AdminControl` la richiesta, la risposta e la sessione HTTP sono oggetti finti creati con Mockito, e i DAO sono mock.

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
| 14b | `metodi_chiamataRiuscita_risorseChiuse` | Test parametrizzato sui cinque metodi che chiudono le risorse a mano: dopo una chiamata riuscita ogni statement e ogni connessione aperti vengono chiusi (due di ciascuno nei metodi che leggono anche le votazioni). |
| 15 | `doRetrieveAll_limitOffset_parametriImpostatiERisultati` | `doRetrieveAll` passa `limit` e `offset` come parametri e restituisce tutti i prodotti letti, con tutti i campi (descrizione, categoria, magazzino, sconto, colorazione, data di inserimento), le votazioni e i prezzi decimali corretti. |
| 15b | `doRetrieveAll_orderNulloVuotoOIndicato_orderBySoloSeIndicato` | Test parametrizzato: la query contiene `ORDER BY` solo se `order` è indicato, e non quando è `null` o vuoto. |
| 16 | `doRetrieveByInput_zeroUnoOPiuRisultati_jsonCorretto` | `doRetrieveByInput` costruisce un JSON valido con 0, 1 e più risultati, senza virgola dopo l'ultimo elemento. |
| 17 | `doRetrieveByInput_connessioneFallita_lanciaSQLException` | Se `getConnection()` fallisce, `doRetrieveByInput` propaga la `SQLException` invece di lanciare una `NullPointerException`. |
| 17b | `altriMetodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sugli altri quattro metodi che chiudono le risorse a mano (`doDelete` usa try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |
| 17c | `metodi_queryProdottiFallita_errorePropagatoERisorseChiuse` | Test parametrizzato su `doRetrieveByInput`, `doRetrieveAll`, `doRetrieveByCat` e `doRetrieveBySubCat`: se la query dei prodotti fallisce, la stessa `SQLException` arriva al chiamante e statement e connessione vengono chiusi. |
| 17d | `metodi_queryVotazioniFallita_errorePropagatoERisorseChiuse` | Test parametrizzato sui tre metodi che leggono anche le votazioni: se fallisce la seconda query, l'errore arriva al chiamante e vengono chiusi gli statement di entrambe le query e le due connessioni. |
| 18 | `doRetrieveByCat_categoriaMalevola_nessunaSqlInjection` | Con la categoria `x' OR '1'='1` il testo iniettato non compare nella query SQL, perché la categoria viene passata come parametro. |
| 18b | `doRetrieveByCat_categoriaAll_tuttiIProdottiConVotazioni` | Con la categoria `all`, che l'app usa per mostrare l'intero catalogo, la query non filtra per categoria, ordina per ID e restituisce i prodotti con tutti i campi, compresa l'unità di misura, e con le loro votazioni. |
| 18c | `doRetrieveBySubCat_categoriaESottocategoriaMalevole_nessunaSqlInjection` | Come il test 18, ma per la categoria e la sottocategoria di `doRetrieveBySubCat`. |
| 18d | `doRetrieveBySubCat_categoriaAll_tuttiIProdottiConVotazioni` | Come il test 18b, ma per `doRetrieveBySubCat`. |
| 19 | `doDelete_zeroOUnaRigaCancellata_trueSoloSeCancellaEChiudeConnessione` | Test parametrizzato con i valori limite 0 e 1: `doDelete` restituisce `true` solo se cancella almeno una riga, e in entrambi i casi chiude la connessione. |

## 3. `CartModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 20 | `addProduct_giaPresente_eseguiUpdate` | Se il prodotto è già nel carrello, `addProduct` esegue l'`UPDATE` della quantità con 4 parametri. |
| 21 | `addProduct_nuovo_eseguiInsertConQuantita` | Se il prodotto è nuovo, `addProduct` esegue l'`INSERT` con la quantità indicata. |
| 22 | `deleteProduct_ultimoONonUltimo_deleteOppureUpdate` | Se `isUltimo` è `true`, `deleteProduct` esegue un `DELETE`; altrimenti esegue un `UPDATE` che toglie 1 alla quantità. |
| 23 | `getProducts_righePresenti_prodottiMappati` | `getProducts` copia nei bean prezzo, quantità ordinata e gli altri campi di ogni riga, e chiude il `ResultSet` letto. |
| 23b | `getProducts_nessunaRiga_listaVuota` | Se il carrello è vuoto, `getProducts` restituisce una lista vuota. |
| 24 | `confermaOrdine_scontiDiversi_totaleEPrezziCorretti` | Test parametrizzato con sconto dello 0%, del 50% e del 100% e con più prodotti e quantità: verifica i prezzi fatturati e il totale passati al DB, l'ordine e il prodotto di ogni prezzo, l'IVA del 20% e la data di inoltro di oggi. |
| 24b | `confermaOrdine_ordineValido_transazioneConfermata` | Un ordine riuscito viene confermato con `commit()` dopo l'ultimo passo, senza rollback, e l'autocommit viene poi ripristinato. |
| 25 | `confermaOrdine_carrelloVuoto_totaleZeroENuovoCarrello` | Con il carrello vuoto, `confermaOrdine` salva un totale di 0 e crea comunque il nuovo carrello. |
| 26 | `confermaOrdine_erroreSecondoPasso_nessunaModificaParziale` | Se la conferma dell'ordine fallisce, i prezzi già scritti vengono annullati con il rollback della transazione. |
| 26b | `metodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sui tre metodi che chiudono le risorse a mano (`confermaOrdine` usa try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |
| 26c | `metodi_chiamataRiuscita_risorseChiuse` | Test parametrizzato sugli stessi tre metodi: dopo una chiamata riuscita statement e connessione vengono chiusi. |
| 26d | `metodi_queryFallita_errorePropagatoERisorseChiuse` | Test parametrizzato sugli stessi tre metodi: se la query fallisce, la stessa `SQLException` arriva al chiamante e statement e connessione vengono chiusi. |

## 4. `UserModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 27 | `addUser_utenteValido_parametriInOrdine` | `AddUser` passa i 9 campi dell'utente nell'ordine giusto, con `permessi_admin = "0"`. |
| 28 | `addUser_utenteValido_carrelloCreatoConIdUtente` | `AddUser` crea il carrello associato all'id dell'utente appena inserito. |
| 28b | `addUser_utenteValido_transazioneConfermata` | Una registrazione riuscita viene confermata con `commit()` dopo la creazione del carrello, senza rollback, e l'autocommit viene poi ripristinato. |
| 29 | `addUser_creazioneCarrelloFallita_utenteNonSalvato` | Se la creazione del carrello fallisce, l'inserimento dell'utente viene annullato. |
| 30 | `getUserTramiteCredenziali_credenzialiCorrette_utentePopolato` | Con credenziali corrette, `getUserTramiteCredenziali` restituisce l'utente con id, password cifrata, id del carrello, indirizzo, telefono e flag admin. |
| 31 | `getUserTramiteCredenziali_credenzialiErrate_beanVuoto` | Con credenziali errate, `getUserTramiteCredenziali` restituisce un bean vuoto e non `null`. |
| 32a | `getIdCart_trovatoONonTrovato_idOppureMenoUno` | Test parametrizzato: `getIdCart` cerca il carrello con username e password e ne restituisce l'id, oppure −1 se il carrello non esiste. |
| 32b | `getIdCartByUserId_trovatoONonTrovato_idOppureMenoUno` | Test parametrizzato: `getIdCartByUserId` cerca il carrello con l'id dell'utente e ne restituisce l'id, oppure −1 se il carrello non esiste. |
| 33 | `getAllOrdine_ordiniPresentiOAssenti_listaIdOppureVuota` | `getAllOrdine` restituisce gli id degli ordini dell'utente, oppure una lista vuota se non ce ne sono. |
| 33b | `getUserTramiteID_trovato_utentePopolato` | `getUserTramiteID` copia nel bean tutti i dati dell'utente con l'id indicato, password compresa; l'id del carrello resta −1. |
| 33c | `getUserTramiteID_nonTrovato_beanVuoto` | Con un id inesistente, `getUserTramiteID` restituisce un bean vuoto e non `null`. |
| 33d | `getUtenti_piuUtenti_utentiMappati` | `getUtenti` restituisce tutti gli utenti, ciascuno con tutti i suoi dati e il proprio ruolo. |
| 33e | `getUtenti_nessunUtente_listaVuota` | Se non ci sono utenti, `getUtenti` restituisce una lista vuota. |
| 33f | `getOrdineById_ordineDellUtente_ordineEProdottiMappati` | `getOrdineById` cerca l'ordine usando sia l'id dell'utente sia quello dell'ordine, e ne copia i dati e i prodotti con tutti i loro campi, compreso il prezzo pagato al momento dell'acquisto. |
| 33g | `getOrdineById_ordineNonTrovato_ordineVuoto` | Se l'ordine non esiste o appartiene a un altro utente, `getOrdineById` restituisce un ordine vuoto, senza prodotti. |
| 33h | `metodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sui sette metodi che chiudono le risorse a mano (`AddUser` usa try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |
| 33i | `metodi_chiamataRiuscita_risorseChiuse` | Test parametrizzato sugli stessi sette metodi: dopo una chiamata riuscita ogni statement e ogni connessione aperti vengono chiusi (due di ciascuno in `getOrdineById`). |
| 33j | `getOrdineById_letturaProdottiFallita_errorePropagatoERisorseChiuse` | Se fallisce la lettura dei prodotti dopo che l'ordine è stato trovato, la stessa `SQLException` arriva al chiamante e vengono chiusi entrambi gli statement e le due connessioni. |

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
| 38f | `metodi_chiamataRiuscita_risorseChiuse` | Test parametrizzato sugli stessi quattro metodi: dopo una chiamata riuscita statement e connessione vengono chiusi. |

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
| Copertura strutturale: per ogni package almeno l'80% delle righe, il 75% dei branch e il 100% delle classi. | Con JaCoCo: `./mvnw verify` fallisce se una soglia non è raggiunta. Dal controllo sono escluse le classi non testate, elencate sopra, e il package `control`. Codecov misura lo stesso perimetro (`codecov.yml`); dettagli in [coverage.md](coverage.md). |
| Mutation testing: almeno l'80% dei mutanti uccisi. | Con PIT, avviato a mano: su GitHub dal workflow *Mutation Testing* (`mutation.yml`, non a ogni push) e in locale con `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`. Le classi analizzate sono `Model.*` e `AzioniAdminModel`; l'analisi dei mutanti è in [mutation.md](mutation.md). L'andamento delle metriche, dalla suite di 100 casi a quella attuale, è in [metriche.md](metriche.md). |

## Strategia di regressione

A ogni modifica si riesegue l'intera suite (*retest-all*): `ci.yml` la lancia a ogni push e pull request
verso `main`. I 211 casi richiedono pochi secondi, quindi non serve selezionarne un sottoinsieme. Se un
test fallisce, la pipeline non produce né il WAR né l'immagine Docker.

## Risultati

### Copertura

| Package | Righe prima → dopo | Branch prima → dopo |
|---------|--------------------|---------------------|
| `Model` (i quattro DAO) | 69% → 97% | 47% → 92% |
| `Model.Bean` (`CartBean`, `ProductBean`) | 89% → 96% | 65% → 100% |
| `admin` (`AzioniAdminModel`) | 90% → 90% | 75% → 75% |

"Prima" indica la suite di 100 casi, "dopo" quella attuale di 211 casi descritta in questo piano, con la
variabile `rs` mai usata tolta da `CartModel`. Prima le soglie non erano raggiunte nei package `Model` e
`Model.Bean`; ora `./mvnw verify` le rispetta tutte. I valori intermedi sono in [metriche.md](metriche.md).

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

## Difetti trovati dai test

| Test | Difetto | Correzione |
|---|---|---|
| 7-9b | Incremento e decremento confrontavano i prodotti con `equals()` | confronto per ID |
| 10 | `hashCode()` assente | aggiunto, coerente con `equals()` |
| 13 | Prezzo e grammatura letti con `getInt`, decimali persi | `getDouble` |
| 17 | `NullPointerException` al posto della `SQLException` | chiusura solo se la connessione esiste |
| 18 | SQL injection su categoria e sottocategoria | parametri del `PreparedStatement` |
| 26 | Ordine confermato a metà se un passo falliva | transazione con `rollback` |
| 29 | Utente registrato senza carrello | transazione con `rollback` |
| 36 | Connessioni mai chiuse, pool esaurito | try-with-resources |
| 38 | `RecensioneBean.isEmpty()` sempre falso | confronto con i valori di default |
| 41-45 | Il server accettava password deboli, email, telefoni e date non validi | validazione anche sul server |
| 46 | Data in formato errato: errore 500 | risposta `val 0` |
| 48 | Errore del database segnalato come registrazione riuscita | risposta `val 0` |
| 52, 56 | `NullPointerException` senza parametri di login o senza login | controllo dei valori `null` |
| 53 | Secondo login: dati scritti nella sessione appena invalidata | uso della nuova sessione |
| 64-68 | Azioni admin (`/Admin`) eseguibili senza login | filtro su `/Admin` e ruolo verificato nella servlet |


## Rami non coperti

Con questi test tutti i package controllati da JaCoCo superano le soglie (righe 80%, branch 75%).
I rami che restano scoperti non sono raggiungibili, oppure non producono un effetto da verificare:

| Classe | Rami | Motivo |
|--------|------|--------|
| `UserControl` | `emptyUser` e metà di `Validate` | `emptyUser` non viene mai chiamato e `Validate` ricontrolla campi già verificati da `datiRegistrazioneValidi`. È codice morto e per ora resta; la variabile `rs` mai usata di `CartModel`, invece, è stata tolta. |
| `RecensioneModel` | il caso `connection == null` e il caso con 0 righe modificate | `getConnection()` non restituisce mai `null`: in caso di problemi lancia un'eccezione. Con 0 righe il metodo stampa solo un messaggio, quindi non c'è un risultato da verificare. |
| `AzioniAdminModel` | il caso `rowsUpdated < 1` | Il blocco dell'`if` è vuoto, quindi non c'è nulla da verificare. |

---
