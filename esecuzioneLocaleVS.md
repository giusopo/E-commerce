# Esecuzione in locale da VS Code

Guida per lo sviluppo in locale su Windows, senza Docker: debug con F5, riavvii rapidi e database
MariaDB portabile. Per l'avvio con Docker vedere il [README](README.md).

## Avvio da VS Code su Windows

Per lo sviluppo in locale il progetto si può avviare direttamente da VS Code: gli script scaricano
da soli tutte le dipendenze necessarie.

1. Aprire **questa cartella** in VS Code, con `pom.xml` alla radice.
2. Installare l'estensione consigliata **Extension Pack for Java** (`vscjava.vscode-java-pack`)
   e avere un JDK disponibile nel `PATH`. Su questo PC sono stati verificati JDK 23 e le estensioni Java.
   Per una nuova installazione si può usare JDK 21 o successivo; il codice viene compilato per Java 11.
3. Attendere che VS Code completi l'importazione Maven.
4. In **Esegui e debug**, selezionare **BeautyBlind: avvia con database locale** e premere **F5**.
5. Quando compare `BeautyBlind pronto: http://localhost:8080/`, aprire
   **http://localhost:8080/** nel browser.

Al primo avvio vengono scaricati Maven, le dipendenze Java e MariaDB portabile per Windows x64.
È necessaria una connessione Internet e il primo avvio richiede più tempo.
Non occorre installare Tomcat, Maven o un servizio MySQL separatamente.

Il task inizializza il database `ecommerce` e importa `src/main/db/ecommerce.sql`, comprese le immagini.
I dati restano in `.local-db/`: gli avvii successivi riutilizzano il database senza reimportare il dump.
MariaDB ascolta solo su `127.0.0.1:3307`.
Un database creato con lo schema precedente si aggiorna una volta, a database avviato, senza perdere i dati:

```powershell
.tools/mariadb-11.4.13-winx64/bin/mariadb.exe --defaults-file=.local-db/root.cnf --protocol=tcp --host=127.0.0.1 --port=3307 ecommerce -e "source src/main/db/aggiornamento-schema.sql"
```

### Credenziali del database locale

Nel repository non ci sono password. Al primo avvio lo script genera password casuali di 32 caratteri
e le salva solo in `.local-db/`, cartella esclusa da Git:

- `.local-db/root.cnf`: amministratore `root`, usato solo dagli script di avvio e arresto;
- `.local-db/credentials.properties`: utente applicativo `beautyblind_app`, con i soli permessi
  `SELECT`, `INSERT`, `UPDATE` e `DELETE` sul database `ecommerce`. Il launcher lo legge automaticamente.

Per consultare il database con un client, leggere le credenziali da questi file.
Le versioni precedenti usavano credenziali fisse pubblicate nel repository (`root` e l'utente `beautyblind`).
Se `.local-db/` è stata creata con quelle versioni, il prossimo avvio sostituisce la password di `root`
ed elimina l'utente `beautyblind`, senza modificare i dati.
Per ripartire da zero, arrestare il database ed eliminare `.local-db/`: al prossimo avvio dati e credenziali
vengono ricreati.

**Shift+F5** arresta l'applicazione. Il database rimane disponibile per il prossimo avvio.
Per arrestarlo usare **Terminale → Esegui attività → BeautyBlind: arresta database locale**.

## Comandi dal terminale PowerShell

Dalla radice del progetto:

```powershell
# Avvia il database portabile e importa i dati al primo avvio
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Start-Database.ps1

# Compila e avvia Tomcat con Servlet e JSP
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Invoke-Maven.ps1 compile exec:java

# Genera target/ROOT.war
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Invoke-Maven.ps1 clean package

# Arresta il database locale
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Stop-Database.ps1
```

`Invoke-Maven.ps1` usa il Maven Wrapper e, se `JAVA_HOME` è errato, rileva il JDK
dal comando `java` presente nel `PATH`. La correzione vale solo per il processo avviato.
Con `JAVA_HOME` già corretto si può usare direttamente `./mvnw.cmd`.
Su Linux/macOS i comandi Maven equivalenti sono `bash ./mvnw clean package` e
`bash ./mvnw compile exec:java`; il database va predisposto separatamente.

## Database esistente e configurazione

Per usare un server MySQL/MariaDB già disponibile:

1. Creare un database vuoto `ecommerce` con codifica `utf8mb4`.
2. Importare `src/main/db/ecommerce.sql` e concedere all'utente applicativo i permessi su quel database.
3. Copiare `dev.properties.example` in `dev.properties` e impostare URL JDBC, utente e password.
4. Selezionare **BeautyBlind: database gia disponibile** in VS Code e premere F5.

Il launcher legge `DB_URL`, `DB_USER`, `DB_PASSWORD` e `HTTP_PORT` da `dev.properties`.
Le variabili d'ambiente con gli stessi nomi hanno precedenza; i valori mancanti sono presi da
`.local-db/credentials.properties`. Nessuna credenziale ha un valore predefinito nel codice:
se utente o password mancano, l'avvio si interrompe con un messaggio esplicito.
Il file locale è escluso da Git.
Se la porta HTTP 8080 è occupata, impostare ad esempio `HTTP_PORT=8081` e aprire la nuova porta nel browser.
Il database portabile dei task usa sempre la porta 3307.

## Risoluzione dei problemi

- **F5 non disponibile:** verificare l'estensione Java e attendere l'importazione Maven.
  Se il progetto è rimasto nello stato precedente, eseguire **Java: Clean Java Language Server Workspace**.
- **Database non disponibile:** eseguire il task di avvio del database o verificare `dev.properties`.
  Il log locale è `.local-db/mariadb.log`.
- **Porta 8080 occupata:** se i container Docker sono attivi, arrestarli con `docker compose down`
  prima di premere F5, oppure impostare un'altra `HTTP_PORT`.
- **Porta 3307 occupata:** arrestare l'istanza che occupa la porta oppure configurare un database esterno.
  Lo script non arresta servizi estranei.
- **Importazione incompleta:** lo script si ferma se trova tabelle senza il marcatore
  `.local-db/seed-imported`; verificare i dati prima di intervenire. Non sovrascrive un database già popolato.
