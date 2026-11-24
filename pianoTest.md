# Piano dei test

Elenco dei test di unità del progetto, tutti implementati in `src/test/java`. Il numero di ogni test
è all'inizio del suo `@DisplayName` e compare nella tabella dei risultati della pipeline.
La strategia di test è descritta in [testing.md](testing.md).

Nei test dei DAO il database viene sostituito da mock di `DataSource`, `Connection`, `PreparedStatement` e `ResultSet`.

## Classi escluse dai test

| Classe | Motivo |
|--------|--------|
| Servlet: `CartControl`, `ProductControl`, `UserControl`, `StarControl`, `AdminControl`, `GetPicturesControl` | Hanno poca logica propria: leggono i parametri, chiamano un DAO e passano la richiesta a una JSP. |
| Validatori in `UserControl` | Sono metodi `private`, per poterli testare dovremmo intervenire sui modificatori di accessibilità |
| `AuthFilter` | Richiede mock di richiesta, risposta e sessione. È un controllo di sicurezza, quindi è il primo test aggiuntivo da scrivere. |
| Bean: `UserBean`, `OrdineBean`, `RecensioneBean` e `ProductBean` (eccetto il test 10) | Contengono solo getter e setter, i test sarebbero poco utili. `CartBean` invece lo testiamo siccome ha metodi che gestiscono la logica del carrello. |
| `IMGmodel` | È interamente statico: per testarlo servirebbero mock statici, complessi da configurare e con poco beneficio. |
| Interfacce `*_intf` | Non contengono codice da eseguire. |
| `dev.LocalServer` | Serve solo ad avviare l'app in locale ed è escluso dal WAR, quindi non è codice di produzione. |
| JSP (es. `Cart.jsp`) | Non si possono testare con unit test Java. La formula del prezzo scontato che contengono è comunque verificata dal test 24 su `CartModel`. |

---

## 1. `CartBeanTest` (carrello ospite)

| # | Test | Descrizione |
|---|------|-------------|
| 1 | `addProduct_prodottoNuovo_vieneAggiuntoAllaLista` | Dopo l'aggiunta, il carrello contiene il prodotto. |
| 2 | `deleteProduct_prodottoPresenteOAssente_rimuovePerIdOppureNonCambiaNulla` | Rimuove il prodotto con lo stesso ID. Se l'ID non è nel carrello, il carrello resta invariato. |
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
| 19 | `doDelete_nessunaRigaCancellata_restituisceFalse` | Se `executeUpdate` non cancella righe, il metodo restituisce `false`. |

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
| 32 | `getIdCart_trovatoONonTrovato_idOppureMenoUno` | `getIdCart` e `getIdCartByUserId` restituiscono l'id del carrello, oppure −1 se non esiste. |
| 33 | `getAllOrdine_ordiniPresentiOAssenti_listaIdOppureVuota` | Restituisce gli id degli ordini dell'utente, oppure una lista vuota. |

## 5. `AzioniAdminModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 34 | `inserisciProdotto_esitoUpdate_trueOppureFalse` | Passa i 10 campi del prodotto. Restituisce `true` se `executeUpdate` è maggiore di 0, altrimenti `false`. |
| 35 | `modificaSconto_parametri_scontoEIdImpostati` | Passa al DB la percentuale di sconto e l'id del prodotto. |
| 36 | `azioniAdmin_dopoEsecuzione_risorseChiuse` | Dopo `inserisciProdotto` e `modificaSconto`, `PreparedStatement` e `Connection` sono chiusi. |

## 6. `RecensioneModelTest`

| # | Test | Descrizione |
|---|------|-------------|
| 37 | `addEModificaRecensione_parametriCorretti` | `addRecensione` passa al DB utente, prodotto, punteggio, commento e data di oggi. `modificaRecensione` passa punteggio, commento e id della recensione. |
| 38 | `getRecensioni_presentiOAssenti_mappateOppureVuote` | `getAllRecensioni` e `getRecensione` copiano i dati nei bean, oppure restituiscono una lista vuota o un bean vuoto. |

---

