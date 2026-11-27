# Piano dei test

Elenco dei test di unità del progetto, tutti implementati in `src/test/java`. Il numero di ogni test
è all'inizio del suo `@DisplayName` e compare nella tabella dei risultati della pipeline.
La strategia di test è descritta in [testing.md](testing.md). Le regole che i test verificano, con la loro
fonte, sono raccolte in [design.md](design.md).

Nei test dei DAO e di `IMGmodel` il database viene sostituito da mock di `DataSource`, `Connection`, `PreparedStatement` e `ResultSet`.

Nei test di `UserControl`, `AuthFilter`, `AdminControl`, `ProductControl` e `CartControl` la richiesta, la risposta e la sessione HTTP sono oggetti finti creati con Mockito, e i DAO sono mock.

## 1. `CartBeanTest` (carrello ospite), `ProductBeanTest` e `RecensioneBeanTest`

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
| 10e | `recensioniUguali_stessoHashCode` | Due `RecensioneBean` uguali secondo `equals()` hanno lo stesso `hashCode()`. |
| 10f | `autoreDiverso_recensioniDiverse` | Due recensioni che differiscono solo per l'autore (`userID`) non sono uguali. |

## 2. `ProductModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 11 | `doRetrieveByKey_idEsistente_beanPopolato` | `doRetrieveByKey` passa l'id come parametro della query e copia nel bean i campi letti dal `ResultSet`. |
| 12 | `doRetrieveByKey_idInesistente_beanVuoto` | Se la query non trova righe, `doRetrieveByKey` restituisce un bean vuoto (`isEmpty()`). |
| 13 | `doRetrieveByKey_prezzoDecimale_prezzoNonTroncato` | Un prezzo di 12,99 e una grammatura decimale vengono copiati nel bean senza perdere i decimali. |
| 14 | `doRetrieveByKey_sqlException_risorseChiuse` | Se la query fallisce, `PreparedStatement` e `Connection` vengono chiusi comunque. |
| 14b | `metodi_chiamataRiuscita_risorseChiuse` | Test parametrizzato sui cinque metodi che chiudevano le risorse a mano: dopo una chiamata riuscita ogni statement e ogni connessione aperti vengono chiusi (due di ciascuno nei metodi che leggono anche le votazioni). |
| 15 | `doRetrieveAll_limitOffset_parametriImpostatiERisultati` | `doRetrieveAll` passa `limit` e `offset` come parametri e restituisce tutti i prodotti letti, con tutti i campi (descrizione, categoria, sottocategoria, magazzino, sconto, colorazione, data di inserimento e unità di misura, che la home mostra accanto alla grammatura), le votazioni e i prezzi decimali corretti. |
| 15b | `doRetrieveAll_orderNulloVuotoOIndicato_orderBySoloSeIndicato` | Test parametrizzato: la query contiene `ORDER BY` solo se `order` è indicato, e non quando è `null` o vuoto. |
| 16 | `doRetrieveByInput_zeroUnoOPiuRisultati_jsonCorretto` | `doRetrieveByInput` costruisce un JSON valido con 0, 1 e più risultati, senza virgola dopo l'ultimo elemento. |
| 16b | `doRetrieveByInput_nomeConCaratteriSpeciali_jsonValido` | Virgolette, barra rovesciata e caratteri di controllo nel nome di un prodotto vengono scritti nel JSON come sequenze di escape, quindi il JSON resta valido. |
| 17 | `doRetrieveByInput_connessioneFallita_lanciaSQLException` | Se `getConnection()` fallisce, `doRetrieveByInput` propaga la `SQLException` invece di lanciare una `NullPointerException`. |
| 17b | `altriMetodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sugli altri quattro metodi che chiudevano le risorse a mano (`doDelete` usava già try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |
| 17c | `metodi_queryProdottiFallita_errorePropagatoERisorseChiuse` | Test parametrizzato su `doRetrieveByInput`, `doRetrieveAll`, `doRetrieveByCat` e `doRetrieveBySubCat`: se la query dei prodotti fallisce, la stessa `SQLException` arriva al chiamante e statement e connessione vengono chiusi. |
| 17d | `metodi_queryVotazioniFallita_errorePropagatoERisorseChiuse` | Test parametrizzato sui tre metodi che leggono anche le votazioni: se fallisce la seconda query, l'errore arriva al chiamante e vengono chiusi gli statement di entrambe le query e le due connessioni. |
| 17e | `metodi_chiusuraStatementFallita_errorePropagatoEConnessioneChiusa` | Test parametrizzato sui cinque metodi del test 14b: se dopo una query riuscita fallisce la chiusura dello statement, quell'errore arriva al chiamante e la connessione viene chiusa comunque. |
| 17f | `metodi_queryEChiusuraStatementFallite_erroreDellaQueryPropagato` | Test parametrizzato sugli stessi cinque metodi: se fallisce la query e poi anche la chiusura dello statement, al chiamante arriva l'errore della query, con quello della chiusura tra i soppressi, e la connessione viene chiusa. |
| 17g | `metodi_chiusuraStatementVotazioniFallita_errorePropagatoEConnessioniChiuse` | Come il 17e, ma sulla query delle votazioni dei tre metodi che la eseguono: vengono chiuse le connessioni di entrambe le query. |
| 17h | `metodi_queryEChiusuraVotazioniFallite_erroreDellaQueryPropagato` | Come il 17f, ma sulla query delle votazioni: arriva l'errore della query e vengono chiuse le connessioni di entrambe le query. |
| 18 | `doRetrieveByCat_categoriaMalevola_nessunaSqlInjection` | Con la categoria `x' OR '1'='1` il testo iniettato non compare nella query SQL, perché la categoria viene passata come parametro. |
| 18b | `doRetrieveByCat_categoriaAll_tuttiIProdottiConVotazioni` | Con la categoria `all`, che l'app usa per mostrare l'intero catalogo, la query non filtra per categoria, ordina per ID e restituisce i prodotti con tutti i campi, comprese unità di misura e sottocategoria, e con le loro votazioni. |
| 18c | `doRetrieveBySubCat_categoriaESottocategoriaMalevole_nessunaSqlInjection` | Come il test 18, ma per la categoria e la sottocategoria di `doRetrieveBySubCat`. |
| 18d | `doRetrieveBySubCat_categoriaAll_tuttiIProdottiConVotazioni` | Come il test 18b, ma per `doRetrieveBySubCat`. |
| 19 | `doDelete_zeroOUnaRigaCancellata_trueSoloSeCancellaEChiudeConnessione` | Test parametrizzato con i valori limite 0 e 1: `doDelete` restituisce `true` solo se cancella almeno una riga, e in entrambi i casi chiude la connessione. |

## 3. `CartModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 20 | `addProduct_giaPresente_eseguiUpdate` | Se il prodotto è già nel carrello, `addProduct` esegue l'`UPDATE` che aumenta la quantità della riga della quantità indicata (3 parametri), senza una subquery sulla stessa tabella, che MySQL non accetta. |
| 21 | `addProduct_nuovo_eseguiInsertConQuantita` | Se il prodotto è nuovo, `addProduct` esegue l'`INSERT` con la quantità indicata. |
| 22 | `deleteProduct_ultimoONonUltimo_deleteOppureUpdate` | Se `isUltimo` è `true`, `deleteProduct` esegue un `DELETE`; altrimenti esegue un `UPDATE` che toglie 1 alla quantità della riga (2 parametri), senza subquery. |
| 23 | `getProducts_righePresenti_prodottiMappati` | `getProducts` copia nei bean prezzo, quantità ordinata e gli altri campi di ogni riga, e chiude il `ResultSet` letto. |
| 23b | `getProducts_nessunaRiga_listaVuota` | Se il carrello è vuoto, `getProducts` restituisce una lista vuota. |
| 24 | `confermaOrdine_scontiDiversi_totaleEPrezziCorretti` | Test parametrizzato con sconto dello 0%, del 50% e del 100% e con più prodotti e quantità: verifica i prezzi fatturati e il totale passati al DB, l'ordine e il prodotto di ogni prezzo, l'IVA del 20% e la data di inoltro di oggi. |
| 24b | `confermaOrdine_ordineValido_transazioneConfermata` | Un ordine riuscito viene confermato con `commit()` dopo l'ultimo passo, senza rollback, e l'autocommit viene poi ripristinato. |
| 24c | `confermaOrdine_giacenzaSufficiente_giacenzaDiminuita` | La giacenza di ogni prodotto diminuisce della quantità ordinata, con un `UPDATE` che la modifica solo se basta (`QTAmagazzino >= ?`). |
| 24d | `confermaOrdine_ordineValido_idNuovoCarrelloRestituito` | `confermaOrdine` restituisce l'id del nuovo carrello generato dal database e chiude il `ResultSet` delle chiavi. |
| 25 | `confermaOrdine_carrelloVuoto_totaleZeroENuovoCarrello` | Con il carrello vuoto, `confermaOrdine` salva un totale di 0 e crea comunque il nuovo carrello. |
| 26 | `confermaOrdine_erroreSecondoPasso_nessunaModificaParziale` | Se la conferma dell'ordine fallisce, i prezzi già scritti vengono annullati con il rollback della transazione. |
| 26b | `metodi_connessioneFallita_lancianoSQLException` | Test parametrizzato su `addProduct`, `deleteProduct` e `getProducts`, che chiudevano le risorse a mano (`confermaOrdine` usava già try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |
| 26c | `metodi_chiamataRiuscita_risorseChiuse` | Test parametrizzato sugli stessi tre metodi: dopo una chiamata riuscita statement e connessione vengono chiusi. |
| 26d | `metodi_queryFallita_errorePropagatoERisorseChiuse` | Test parametrizzato sugli stessi tre metodi: se la query fallisce, la stessa `SQLException` arriva al chiamante e statement e connessione vengono chiusi. |
| 26e | `metodi_chiusuraStatementFallita_errorePropagatoEConnessioneChiusa` | Test parametrizzato sugli stessi tre metodi: se dopo una query riuscita fallisce la chiusura dello statement, quell'errore arriva al chiamante e la connessione viene chiusa comunque. |
| 26f | `metodi_queryEChiusuraStatementFallite_erroreDellaQueryPropagato` | Test parametrizzato sugli stessi tre metodi: se fallisce la query e poi anche la chiusura dello statement, al chiamante arriva l'errore della query, con quello della chiusura tra i soppressi, e la connessione viene chiusa. |
| 26g | `confermaOrdine_giacenzaInsufficiente_nessunaModifica` | Valore limite della giacenza: se un prodotto ne ha meno della quantità ordinata, l'errore arriva al chiamante, la transazione viene annullata e non vengono né chiuso l'ordine né creato un nuovo carrello. |
| 26h | `confermaOrdine_idNuovoCarrelloMancante_rollback` | Se il database non restituisce l'id del nuovo carrello, la conferma fallisce e viene annullata. |

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
| 33b | `getUserTramiteID_trovato_utentePopolato` | `getUserTramiteID` copia nel bean tutti i dati dell'utente con l'id indicato, data di nascita e password comprese; l'id del carrello resta −1. |
| 33c | `getUserTramiteID_nonTrovato_beanVuoto` | Con un id inesistente, `getUserTramiteID` restituisce un bean vuoto e non `null`. |
| 33d | `getUtenti_piuUtenti_utentiMappati` | `getUtenti` restituisce tutti gli utenti, ciascuno con tutti i suoi dati, data di nascita compresa, e il proprio ruolo. |
| 33e | `getUtenti_nessunUtente_listaVuota` | Se non ci sono utenti, `getUtenti` restituisce una lista vuota. |
| 33f | `getOrdineById_ordineDellUtente_ordineEProdottiMappati` | `getOrdineById` cerca l'ordine usando sia l'id dell'utente sia quello dell'ordine, e ne copia i dati e i prodotti con tutti i loro campi, compreso il prezzo pagato al momento dell'acquisto. |
| 33g | `getOrdineById_ordineNonTrovato_ordineVuoto` | Se l'ordine non esiste o appartiene a un altro utente, `getOrdineById` restituisce un ordine vuoto, senza prodotti. |
| 33h | `metodi_connessioneFallita_lancianoSQLException` | Test parametrizzato sui sette metodi che chiudevano le risorse a mano (`AddUser` usava già try-with-resources): se `getConnection()` fallisce, ognuno propaga la `SQLException`. |
| 33i | `metodi_chiamataRiuscita_risorseChiuse` | Test parametrizzato sugli stessi sette metodi: dopo una chiamata riuscita ogni statement e ogni connessione aperti vengono chiusi (due di ciascuno in `getOrdineById`). |
| 33j | `getOrdineById_letturaProdottiFallita_errorePropagatoERisorseChiuse` | Se fallisce la lettura dei prodotti dopo che l'ordine è stato trovato, la stessa `SQLException` arriva al chiamante e vengono chiusi entrambi gli statement e le due connessioni. |
| 33k | `metodi_chiusuraStatementFallita_errorePropagatoEConnessioneChiusa` | Test parametrizzato sui sette metodi del test 33h: se dopo una query riuscita fallisce la chiusura dello statement, quell'errore arriva al chiamante e la connessione viene chiusa comunque. |
| 33l | `metodi_queryEChiusuraStatementFallite_erroreDellaQueryPropagato` | Test parametrizzato sugli stessi sette metodi: se fallisce la query e poi anche la chiusura dello statement, al chiamante arriva l'errore della query, con quello della chiusura tra i soppressi, e la connessione viene chiusa. |
| 33m | `getOrdineById_chiusuraStatementProdottiFallita_errorePropagatoEConnessioniChiuse` | Come il 33k, ma sulla lettura dei prodotti di `getOrdineById`: vengono chiuse le connessioni di entrambe le query. |
| 33n | `getOrdineById_letturaEChiusuraProdottiFallite_erroreDellaQueryPropagato` | Come il 33l, ma sulla lettura dei prodotti di `getOrdineById`: arriva l'errore della query e vengono chiuse le connessioni di entrambe le query. |

## 5. `AzioniAdminModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 34a | `inserisciProdotto_prodottoValido_campiImpostati` | `inserisciProdotto` passa al DB gli 11 campi del prodotto, unità e sottocategoria comprese, ciascuno nella posizione giusta, e non passa l'id. |
| 34b | `inserisciProdotto_esitoUpdate_trueOppureFalse` | Test parametrizzato: `inserisciProdotto` restituisce `true` se `executeUpdate` è maggiore di 0, altrimenti `false`. |
| 34c | `inserisciProdotto_inserito_idGeneratoNelBean` | Dopo l'inserimento il bean ha l'id generato dal database, e il `ResultSet` delle chiavi viene chiuso. |
| 34d | `inserisciProdotto_sottocategoriaVuota_null` | Test parametrizzato: una sottocategoria vuota o assente viene salvata come `NULL`. |
| 34e | `inserisciProdotto_idNonRestituito_sqlException` | Se il database non restituisce l'id generato, `inserisciProdotto` lancia `SQLException` e l'id del bean non cambia. |
| 35 | `modificaSconto_parametri_scontoEIdImpostati` | `modificaSconto` passa al DB la percentuale di sconto e l'id del prodotto. |
| 35b | `modificaSconto_esitoUpdate_trueOppureFalse` | Test parametrizzato con i valori limite 1 e 0: `modificaSconto` restituisce `true` solo se il prodotto è stato modificato, `false` con un id inesistente. |
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
| 38g | `metodi_chiusuraStatementFallita_errorePropagatoEConnessioneChiusa` | Test parametrizzato sugli stessi quattro metodi: se dopo una query riuscita fallisce la chiusura dello statement, quell'errore arriva al chiamante e la connessione viene chiusa comunque. |
| 38h | `metodi_queryEChiusuraStatementFallite_erroreDellaQueryPropagato` | Test parametrizzato sugli stessi quattro metodi: se fallisce la query e poi anche la chiusura dello statement, al chiamante arriva l'errore della query, con quello della chiusura tra i soppressi, e la connessione viene chiusa. |

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
| 46b | `register_dataInesistenteOConTestoInPiu_registrazioneRifiutata` | Test parametrizzato: una data nel formato giusto ma inesistente (`2000-13-45`, `2001-02-29`) o seguita da altro testo (`2000-05-10abc`) viene rifiutata, non normalizzata. |
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
| 53b | `login_ospiteConCarrello_prodottiUnitiAlCarrelloSalvato` | Al login di un ospite i prodotti del suo carrello vengono uniti al carrello salvato: per un prodotto già presente la quantità si somma, uno nuovo viene inserito. Il carrello dell'ospite non resta in sessione. |
| 53c | `login_ospite_idSessioneRinnovato` | Dopo il login di un ospite l'identificativo di sessione viene rinnovato (`changeSessionId`). |
| 53d | `login_credenzialiErrate_sessioneECarrelloInvariati` | Con credenziali errate l'identificativo di sessione non cambia, il carrello salvato non viene toccato e quello dell'ospite resta in sessione. |
| 54 | `login_erroreDatabase_redirectPaginaErrore` | Se il DB lancia un'eccezione, l'utente viene reindirizzato a `Error.jsp`. |
| 55 | `datiUtente_utenteLoggato_forwardAreaUtente` | I dati dell'utente loggato vengono letti dal DB e passati ad `AreaUtente.jsp`. |
| 55b | `datiUtente_erroreDatabase_redirectPaginaErrore` | Se la lettura dei dati dell'utente fallisce, l'utente va a `Error.jsp` e l'area utente non viene mostrata. |
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
| 69b | `impostaSconto_prodottoInesistente_paginaDiErrore` | Se il prodotto non esiste lo sconto non viene salvato, e l'admin va alla pagina di errore invece che alla pagina dei prodotti. |
| 69c | `impostaSconto_erroreDatabase_paginaDiErrore` | Come il test 69b, ma quando il database fallisce. |
| 69d | `azioneMancanteOSconosciuta_utenteAdmin_paginaDiErrore` | Test parametrizzato: con un'azione mancante, vuota o sconosciuta l'admin viene reindirizzato a `Error.jsp`, senza usare i DAO. |
| 69e | `impostaSconto_valoreNonValido_paginaDiErroreSenzaDao` | Test parametrizzato con i valori limite -1 e 101, uno sconto non numerico e un id non numerico: lo sconto non viene salvato e l'admin va a `Error.jsp`. |
| 69f | `inserisciProdotto_campoNonValido_ritornoAlFormSenzaDao` | Test parametrizzato, una classe non valida per caso: nome, descrizione, categoria o colorazione vuoti o assenti, prezzo o grammatura 0, non numerici o infiniti, giacenza negativa o non intera, sconto 101, unità diversa da `g` e `ml`. Il DAO non viene chiamato e l'admin torna al form. |
| 69g | `inserisciProdotto_valoriAlLimite_prodottoPassatoAlDao` | Test parametrizzato con i valori limite validi: sconto 0 e 100, sconto lasciato vuoto (vale 0) e giacenza 0. Il prodotto viene passato al DAO. |

## 10. `ProductControlTest`

Si testano l'inserimento e la modifica delle recensioni (`doPost`), la scheda del prodotto e i parametri della home (`doGet`). Le altre pagine del catalogo passano solo i dati dei DAO alle JSP e non sono testate.

| # | Test | Descrizione |
|---|------|-------------|
| 70 | `recensione_punteggioValido_salvataERispostaPositiva` | Test parametrizzato con i valori limite validi 1 e 5: la recensione viene salvata a nome dell'utente loggato e la risposta è `val 1`. |
| 70b | `azioniRecensione_punteggioNonValido_nienteSalvatoERispostaNegativa` | Test parametrizzato su inserimento e modifica: con punteggio 0, 6 o non numerico la recensione non viene salvata e la risposta è `val 0`. |
| 70c | `azioniRecensione_erroreDatabase_rispostaNegativa` | Test parametrizzato su inserimento e modifica: se il database fallisce la risposta è `val 0`. |
| 70d | `azioniRecensione_utenteNonLoggato_nienteSalvatoERispostaNegativa` | Test parametrizzato su inserimento e modifica: senza login il database non viene usato e la risposta è `val 0`. |
| 71 | `modificaRecensione_recensioneDellUtente_modificataERispostaPositiva` | L'utente modifica la propria recensione: viene salvata e la risposta è `val 1`. |
| 71b | `modificaRecensione_recensioneDiUnAltroUtente_nonModificataERispostaNegativa` | Test parametrizzato: con l'id della recensione di un altro utente nessuna recensione viene modificata, sia se l'utente non ha recensito il prodotto sia se ne ha una propria. La risposta è `val 0`. |
| 71c | `read_prodottoMancanteOInesistente_rispostaNotFound` | Test parametrizzato: con `ProductID` assente, non numerico o di un prodotto inesistente la risposta è 404, la scheda non viene mostrata e le recensioni non vengono lette. |
| 71d | `read_prodottoEsistente_attributiEForward` | Per un prodotto esistente la scheda riceve il prodotto, la recensione dell'utente (vuota per l'ospite) e le recensioni del prodotto. |
| 71e | `home_paginaELimite_offsetCalcolato` | Test parametrizzato: la home legge i prodotti da `(page - 1) × limit`; pagina e limite mancanti, non numerici o minori di 1 valgono come la prima pagina da 15 prodotti. |

## 11. `CartControlTest`

| # | Test | Descrizione |
|---|------|-------------|
| 72 | `visualizza_ospiteSenzaCarrello_carrelloVuotoCreatoEMostrato` | Senza carrello in sessione ne viene creato uno vuoto e viene mostrato `Cart.jsp`, senza usare il database. |
| 72b | `azioneMancanteOSconosciuta_carrelloMostrato` | Test parametrizzato: con azione mancante, vuota o sconosciuta la richiesta non fallisce e viene mostrato il carrello. |
| 73 | `aggiungi_ospiteProdottoNuovo_aggiuntoConQuantitaUno` | L'ospite aggiunge un prodotto nuovo: entra nel carrello in sessione con quantità 1. |
| 73b | `aggiungiEAumenta_ospiteProdottoPresente_quantitaIncrementata` | Test parametrizzato su `aggiungi` e `aumentaQTA`: un prodotto già nel carrello aumenta di 1 e non viene duplicato. |
| 73c | `decrementaQTA_ospite_quantitaDiminuitaOProdottoTolto` | Test parametrizzato con il valore limite 1: da quantità 2 si passa a 1, da quantità 1 il prodotto viene tolto. |
| 73d | `elimina_ospite_soloQuelProdottoTolto` | `elimina` toglie solo il prodotto indicato; gli altri restano. |
| 73e | `aggiungiEAumenta_ospiteAlLimiteDellaGiacenza_quantitaInvariata` | Test parametrizzato su `aggiungi` e `aumentaQTA`, con il valore limite della giacenza: se il carrello contiene già tutti i pezzi disponibili la quantità non aumenta. |
| 74 | `visualizza_utenteLoggato_carrelloCaricatoDalDatabase` | Per un utente loggato il carrello viene letto dal database prima di essere mostrato. |
| 74b | `aggiungi_utenteLoggato_inseritoOIncrementatoNelDatabase` | Test parametrizzato: un prodotto nuovo viene inserito nel carrello del database, uno già presente viene incrementato. |
| 74c | `aumentaQTA_utenteLoggato_quantitaIncrementataNelDatabase` | `aumentaQTA` incrementa la quantità nel database. |
| 74d | `decrementaQTA_utenteLoggato_quantitaDiminuitaOProdottoTolto` | Test parametrizzato con il valore limite 1: con quantità 2 la quantità nel database diminuisce, con quantità 1 il prodotto viene tolto. |
| 74e | `elimina_utenteLoggato_prodottoToltoDalDatabase` | `elimina` toglie il prodotto dal carrello del database, qualunque sia la quantità. |
| 74f | `aggiungiEAumenta_utenteLoggatoAlLimiteDellaGiacenza_databaseNonModificato` | Come il test 73e, ma per l'utente loggato: il carrello del database non viene modificato. |
| 75 | `azioniConId_idNonValido_rispostaBadRequest` | Test parametrizzato sulle cinque azioni che leggono un id: con id non numerico o mancante la risposta è 400 e il carrello non viene modificato. |
| 75b | `aggiungiEAumenta_prodottoInesistente_rispostaNotFound` | Test parametrizzato su `aggiungi` e `aumentaQTA`, per ospite e utente loggato: con l'id di un prodotto inesistente la risposta è 404 e il carrello non viene modificato. |
| 76 | `confermaOrdine_utenteLoggato_ordineConfermatoENuovoCarrello` | La conferma passa al DAO indirizzo, carrello, utente e prodotti; in sessione restano un carrello vuoto e l'id del nuovo carrello restituito dalla conferma, poi viene mostrato l'ordine. |
| 76b | `confermaOrdine_carrelloVuoto_ordineNonConfermato` | Con il carrello vuoto nessun ordine viene confermato e l'utente torna al carrello. |
| 76c | `confermaOrdine_erroreDatabase_carrelloInvariatoEPaginaErrore` | Se la conferma fallisce, il carrello resta invariato e l'utente va alla pagina di errore, non allo storico dell'ordine. |
| 76d | `azioniOrdini_utenteNonLoggato_redirectAlLogin` | Test parametrizzato su conferma, storico di un ordine e storico completo: senza login il database non viene usato e l'utente va al login. |
| 77 | `getOrdineStorico_utenteLoggato_ordineDellUtenteMostrato` | L'ordine viene letto con l'id dell'utente in sessione (la query filtra per utente) e passato a `ordineStorico.jsp`. |
| 77b | `storicoCompletoOrdini_utenteLoggato_ordiniDellUtenteMostrati` | Gli ordini dell'utente loggato vengono passati a `StoricoOrdini.jsp`. |
| 77c | `getOrdineStorico_ordineNonTrovato_rispostaNotFound` | Se l'ordine non esiste, non è concluso o è di un altro utente, la risposta è 404 e non viene mostrata una fattura vuota. |
| 77d | `storico_erroreDatabase_paginaDiErrore` | Test parametrizzato su storico di un ordine e storico completo: se il database fallisce l'utente va a `Error.jsp`, non a una pagina vuota. |

## 12. `IMGmodelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 78a | `load_ruoloMalevolo_parametriEImmagine` | Con il ruolo `principale' OR '1'='1` il testo non compare nella query: id e ruolo arrivano come parametri, e `load` restituisce i byte letti. |
| 78b | `load_nessunaRiga_null` | Senza un'immagine con quel ruolo `load` restituisce `null`. |
| 78c | `load_chiamataRiuscita_risorseChiuse` | Dopo `load` `ResultSet`, statement e connessione sono chiusi. |
| 78d | `updatePhoto_file_immagineSalvataERisorseChiuse` | `updatePhoto` passa al DB il contenuto del file con la sua lunghezza, il ruolo e il prodotto, e chiude statement e connessione. |

## 13. `HtmlTest`

| # | Test | Descrizione |
|---|------|-------------|
| 79 | `escape_caratteriSpeciali_entita` | Test parametrizzato con una classe per carattere speciale: `&`, `<`, `>`, `"` e `'` diventano entità HTML, anche dentro un'entità già scritta (la `&` per prima); il testo senza caratteri speciali non cambia e `null` diventa la stringa vuota. |

---

## Criteri di adeguatezza

| Criterio | Come si verifica |
|----------|------------------|
| Ogni classe di equivalenza, valida o non valida, ha almeno un caso di test; per gli intervalli si provano i valori limite, sia dentro sia fuori. | Con le tabelle di questo piano. |
| Copertura strutturale: per ogni package almeno l'80% delle righe, il 75% dei branch e il 100% delle classi. | Con JaCoCo: `./mvnw verify` fallisce se una soglia non è raggiunta. Dal controllo sono esclusi i package `control` e `GestioneIMG`, che contengono servlet non testate, `AdminControl`, `UserBean`, `OrdineBean` e `RecensioneBean`. Codecov misura lo stesso perimetro (`codecov.yml`); dettagli in [coverage.md](coverage.md). |
| Mutation testing: almeno l'80% dei mutanti uccisi. | Con PIT, avviato a mano: su GitHub dal workflow *Mutation Testing* (`mutation.yml`, non a ogni push) e in locale con `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`. Le classi analizzate sono `Model.*` e `AzioniAdminModel`; l'analisi dei mutanti è in [mutation.md](mutation.md). L'andamento delle metriche, dalla suite di 100 casi a quella attuale, è in [metriche.md](metriche.md). |

## Strategia di regressione

A ogni modifica si riesegue l'intera suite (*retest-all*): `ci.yml` la lancia a ogni push e pull request
verso `main`. I 383 casi richiedono pochi secondi, quindi non serve selezionarne un sottoinsieme. Se un
test fallisce, la pipeline non produce né il WAR né l'immagine Docker.

## Risultati

### Copertura

| Package | Righe prima → dopo | Branch prima → dopo |
|---------|--------------------|---------------------|
| `Model` (i quattro DAO) | 69% → 96% | 47% → 95% |
| `Model.Bean` (`CartBean`, `ProductBean`) | 89% → 98% | 65% → 100% |
| `admin` (`AzioniAdminModel`) | 90% → 92% | 75% → 100% |

"Prima" indica la suite di 100 casi, "dopo" quella attuale di 383 casi descritta in questo piano, con
la variabile `rs` mai usata tolta da `CartModel`, le chiusure di tutti i DAO con try-with-resources e le
correzioni emerse ricostruendo le specifiche e il design dal codice ([design.md](design.md)).
Prima le soglie non erano raggiunte nei package `Model` e `Model.Bean`; ora `./mvnw verify` le rispetta
tutte. I valori intermedi sono in [metriche.md](metriche.md).

Alcune classi sono testate ma restano fuori dal controllo delle soglie: `UserControl` (righe 93%,
branch 96%), `AuthFilter` (righe 82%, branch 100%), `CartControl` (righe 92%, branch 98%), `ProductControl`,
di cui si testano recensioni, scheda del prodotto e parametri della home (righe 66%), `AdminControl`, di cui
si testano il controllo del ruolo, lo sconto, i controlli del form di inserimento e la pagina di errore
(righe 70%), `IMGmodel` (righe 89%, branch 100%) e `Html` (righe e branch 100%).

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
| 26f | In `addProduct`, `deleteProduct` e `getProducts`, se dopo una query fallita falliva anche la chiusura dello statement, l'errore della query andava perso, sostituito da quello della chiusura | try-with-resources |
| 17f, 17h, 33l, 33n, 38h | Lo stesso difetto del 26f in `ProductModel`, `UserModel` e `RecensioneModel` | try-with-resources |
| 33k, 38g | In `UserModel` e `RecensioneModel`, se falliva la chiusura dello statement, la connessione restava aperta | try-with-resources |
| 29 | Utente registrato senza carrello | transazione con `rollback` |
| 36 | Connessioni mai chiuse, pool esaurito | try-with-resources |
| 38 | `RecensioneBean.isEmpty()` sempre falso | confronto con i valori di default |
| 41-45 | Il server accettava password deboli, email, telefoni e date non validi | validazione anche sul server |
| 46 | Data in formato errato: errore 500 | risposta `val 0` |
| 48 | Errore del database segnalato come registrazione riuscita | risposta `val 0` |
| 52, 56 | `NullPointerException` senza parametri di login o senza login | controllo dei valori `null` |
| 53 | Secondo login: dati scritti nella sessione appena invalidata | uso della nuova sessione |
| 64-68 | Azioni admin (`/Admin`) eseguibili senza login | filtro su `/Admin` e ruolo verificato nella servlet |
| 70b, 70c | Punteggio non valido o errore del database segnalati come recensione salvata (`val 1`); punteggio non numerico: errore 500 | risposta `val 0` |
| 70d | Recensione senza login: `NullPointerException` | risposta `val 0`, database non usato |
| 71 | La modifica di una recensione non restituiva nessuna risposta al client | risposta `val` anche per la modifica |
| 71b | Recensione di un altro utente modificabile cambiando l'id inviato | si modifica solo la recensione dell'utente loggato |
| 72b | Senza azione `NullPointerException`, con un'azione sconosciuta pagina vuota | si mostra il carrello |
| 75 | Id del prodotto o dell'ordine non numerico o mancante: errore 500 | risposta 400 |
| 76b | Ordine confermato con il carrello vuoto | conferma solo con prodotti nel carrello |
| 76c | Conferma fallita mostrata come ordine inoltrato | pagina di errore |
| 33b, 33d | `getUserTramiteID` e `getUtenti` non leggevano la data di nascita: nell'elenco dell'amministratore tutti risultavano nati l'1/1/1970 | lettura di `DATA_NASCITA` |
| 35b, 69b, 69c | Uno sconto non salvato, per un prodotto inesistente o per un errore del database, veniva mostrato come salvato | `modificaSconto` restituisce l'esito; pagina di errore |
| 69d | Azione admin mancante o sconosciuta: redirect a `/error.jsp`, che non esiste (pagina 404) | redirect a `Error.jsp` |
| 20, 22 | Incremento e decremento della quantità con una subquery sulla tabella aggiornata, che MySQL non accetta (errore 1093) | la quantità si calcola sulla riga stessa |
| 24c, 26g, 73e, 74f | Giacenza mai controllata né aggiornata: si potevano ordinare più pezzi di quelli in magazzino | la conferma diminuisce la giacenza solo se basta, altrimenti annulla tutto; nel carrello la quantità non supera la giacenza |
| 24d, 26h, 76 | Se dopo una conferma riuscita falliva la lettura del nuovo carrello, la sessione restava sull'ordine concluso e le aggiunte finivano lì | l'id del nuovo carrello viene dalla stessa transazione della conferma |
| 75b | Un id di prodotto inesistente aggiungeva al carrello dell'ospite un prodotto vuoto | risposta 404 |
| 53b | Al login il carrello dell'ospite andava perso | unito al carrello salvato, con le quantità sommate |
| 53c | Al login di un ospite l'identificativo di sessione restava lo stesso | `changeSessionId()` |
| 46b | Date inesistenti (`2000-13-45`) normalizzate invece che rifiutate; testo dopo la data ignorato | `LocalDate.parse` |
| 55b, 77d | Errore del database nei dati utente o nello storico: pagina vuota | pagina di errore |
| 77c | Ordine non trovato: fattura vuota con numero 0 | risposta 404 |
| 71c | Scheda senza `ProductID`: `NullPointerException` nella JSP; con un id inesistente, un prodotto vuoto | risposta 404 |
| 71e | `limit` non numerico: errore 500; pagina 0 o negativa: errore SQL | prima pagina da 15 prodotti |
| 69e-69g | Sconto e campi del nuovo prodotto controllati solo nel browser, e il form di inserimento era troncato | controlli anche sul server; form completato |
| 34a, 34c, 34d | I prodotti inseriti dall'admin avevano sempre l'unità `g`, nessuna sottocategoria e un id scritto a mano | id generato dal database, unità e sottocategoria scelte nel form |
| 15 | La home non leggeva l'unità di misura e mostrava ogni grammatura in `ml` | unità letta e mostrata |
| 16b | Un nome con virgolette rendeva non valido il JSON della ricerca | escape JSON |
| 10e, 10f | `RecensioneBean` ridefiniva `equals` senza `hashCode` e senza confrontare l'autore | `hashCode` coerente e autore nel confronto |
| 78a, 78d | `IMGmodel` concatenava `IDpic` e `ruolo` della richiesta alla query (SQL injection), chiudeva lo statement sbagliato e non chiudeva il file | parametri e try-with-resources |
| 79 | Dati degli utenti, come l'indirizzo, stampati nelle pagine senza escape | escape HTML nelle JSP |

I difetti 33b, 33d, 35b e 69b-69d sono emersi ricostruendo le specifiche dal codice, quelli successivi dal
confronto tra codice, schema, pagine e test descritto in [design.md](design.md): in entrambi i casi i test
sono stati scritti dopo, e sul codice di prima falliscono. Altre correzioni della stessa revisione
riguardano schema, pagine e JavaScript, che i test di unità non raggiungono. Con l'applicazione avviata
su un database MariaDB reale sono stati verificati:
- prezzi e totali con due decimali;
- vincolo di un solo carrello per utente;
- paginazione della home con lo stesso `limit`;
- stelle della home associate al prodotto giusto, con la pagina eseguita in un browser headless;
- collegamenti alla home;
- tipo delle immagini PNG.

Per la codifica dei parametri del login e per la regola del carattere speciale nel browser è stata
controllata solo la sintassi degli script, non il loro comportamento in un browser.


## Rami non coperti

Con questi test tutti i package controllati da JaCoCo superano le soglie (righe 80%, branch 75%).
I rami che restano scoperti non sono raggiungibili, oppure non producono un effetto da verificare:

| Classe | Rami | Motivo |
|--------|------|--------|
| `UserControl` | errori del database nella copia e nell'unione del carrello dell'ospite; `NoSuchAlgorithmException` di `hashPassword` | Gli errori della copia sono solo stampati, come prima; SHA-256 è sempre disponibile nella JVM. Il codice morto (`emptyUser`, `Validate`, `addressIsValid`) è stato tolto, come la variabile `rs` mai usata di `CartModel`. |
| `RecensioneModel` | il caso con 0 righe modificate | Il metodo stampa solo un messaggio, quindi non c'è un risultato da verificare. I controlli `connection == null`, sempre falsi, sono stati tolti con il passaggio a try-with-resources. |

---
