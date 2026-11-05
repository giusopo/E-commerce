# Analisi e verifica di esecuzione

Verifica effettuata il 5 novembre 2026 su Windows x64 con JDK 23.0.1,
Maven Wrapper 3.3.4 / Maven 3.9.16, Tomcat 9.0.123 e MariaDB portabile 11.4.13.

## Interventi per VS Code

- Aggiunti `pom.xml` e Maven Wrapper: dipendenze scaricate automaticamente e compilazione con `--release 11`.
- Eliminato il riferimento assoluto al `servlet-api.jar` di un'installazione locale di Tomcat.
- Aggiornati i metadati Eclipse affinché il progetto possa essere importato come progetto Maven.
- Sostituito il vecchio driver MySQL 8.0.12 con MySQL Connector/J 8.4.0 gestito da Maven.
- Aggiunto il launcher `dev.LocalServer`, con JSP, servlet annotate e datasource JNDI `jdbc/ecommerce`.
- Configurato l'avvio alla radice `/`, coerente con i collegamenti presenti nelle pagine.
- Aggiunte configurazioni F5, task di compilazione, creazione WAR e gestione del database locale.
- Installato Extension Pack for Java su questo PC.
- Gestito nei task Windows il `JAVA_HOME` esistente, che indicava un JDK 21 non installato,
  rilevando il JDK 23 dal `PATH` senza cambiare le variabili globali.
- Predisposto un database portabile isolato sulla porta 3307, con verifica SHA-256 dell'archivio,
  importazione iniziale e conservazione dei dati fra gli avvii.
- Forzata la trasmissione UTF-8 del dump: la code page OEM di Windows alterava gli apostrofi
  tipografici e provocava errori SQL durante l'importazione.

## Controlli eseguiti

| Controllo | Esito |
| --- | --- |
| `clean package` | BUILD SUCCESS; 24 sorgenti Java compilati |
| `target/ROOT.war` | Generato; launcher e librerie del server esclusi, driver JDBC incluso |
| Avvio con `compile exec:java` | Tomcat disponibile su localhost:8080 |
| Avvio diretto della classe Java con classpath Maven | Riuscito, senza dipendere dal classloader di Maven |
| Importazione del dump | 30 prodotti, 59 immagini, 3 utenti iniziali |
| Riavvio del database e avvio ripetuto del task | Dati conservati, nessuna seconda importazione |
| Home `/` | HTTP 200, 15 prodotti |
| Catalogo `/?action=readall` | HTTP 200, 30 prodotti |
| Categoria `viso` | HTTP 200, 13 prodotti |
| Dettaglio prodotto 1 | HTTP 200 |
| Immagine principale prodotto 1 | HTTP 200, 45.425 byte |
| Ricerca `FONDO` | JSON con il prodotto FONDO FILTER |
| Login, contatti, carrello e CSS | HTTP 200 |
| Carrello anonimo | Aggiunta del prodotto 1 e visualizzazione riuscite |
| Credenziali inesistenti | Risposta JSON `val=0` |
| Registrazione e login temporanei | Risposta JSON `val=1` per entrambi |
| Area utente e storico ordini del nuovo account | HTTP 200 |
| Precompilazione delle 11 JSP pubbliche | HTTP 200 per tutte |
| Precompilazione delle 3 JSP amministrative con sessione amministratore | HTTP 200 per tutte |
| URL inesistente | HTTP 404 |
| Controllo del diff Git | Nessun errore di whitespace |

Gli account temporanei creati durante i controlli e i relativi carrelli sono stati rimossi.
La configurazione F5 è stata predisposta e il launcher è stato verificato da terminale;
non è stata automatizzata un'interazione con il pulsante F5 dell'interfaccia di VS Code.
Il repository non conteneva una suite di test automatici. Non sono stati verificati
completamente checkout, gestione magazzino, upload e comportamento con richieste concorrenti.

## Criticità applicative già presenti

Questi punti restano nel codice applicativo e sono distinti dalla configurazione di esecuzione:

1. **Protezione delle azioni amministrative incompleta.** `WEB-INF/web.xml` applica `AuthFilter`
   a `/admin/*`, mentre il controller è pubblicato come `/Admin`. Inoltre il filtro verifica
   solo gli URL che iniziano con `/admin`. Occorre proteggere anche il controller e verificare
   i permessi prima di ciascuna operazione amministrativa.
2. **Password scritte nei log.** `control/UserControl.java` stampa la password ricevuta durante
   il login e valori hash durante la registrazione. Queste stampe vanno eliminate.
3. **Query immagini costruita con parametri HTTP concatenati.** `GestioneIMG/IMGmodel.java`,
   metodo `load`, concatena ID e ruolo nella query. Usare parametri JDBC `?` e validare l'ID.
4. **Connessioni e statement condivisi.** Diversi model conservano oggetti JDBC nei campi
   di istanza; i controller riutilizzano model statici. Usare risorse locali con
   `try-with-resources` per evitare interferenze tra richieste concorrenti.
5. **Conferma ordine senza un'unica transazione.** `Model/CartModel.java`, metodo `confermaOrdine`,
   aggiorna righe d'ordine, ordine e nuovo carrello con connessioni separate.
   Una transazione unica consentirebbe il rollback completo in caso di errore.

Le istruzioni operative e la configurazione di un database esterno sono in [README.md](README.md).
