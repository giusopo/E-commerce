# BeautyBlind

Applicazione e-commerce Java con Servlet, JSP, JDBC e database MySQL/MariaDB.
Gestisce catalogo, immagini, ricerca, recensioni, utenti, carrello, ordini e area amministrativa.

## Avvio da VS Code su Windows

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
MariaDB ascolta solo su `127.0.0.1:3307`; l'applicazione usa l'utente locale
`beautyblind` con password `beautyblind`. L'amministratore del database locale è `root`, password `root`.
Queste credenziali sono riservate allo sviluppo locale.

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
Le variabili d'ambiente con gli stessi nomi hanno precedenza. Il file locale è escluso da Git.
Se la porta HTTP 8080 è occupata, impostare ad esempio `HTTP_PORT=8081` e aprire la nuova porta nel browser.
Il database portabile dei task usa sempre la porta 3307.

## Struttura e distribuzione

- `src/main/java/control`, `admin`, `GestioneIMG`: controller Servlet e filtro di accesso.
- `src/main/java/Model`: accesso JDBC, bean e interfacce.
- `src/main/webapp`: JSP, CSS, JavaScript e configurazione JNDI.
- `src/main/db/ecommerce.sql`: schema e dati iniziali.
- `src/main/java/dev/LocalServer.java`: launcher Tomcat per sviluppo e debug, escluso dal WAR.
- `.vscode/`: configurazioni F5, task di build e gestione del database.
- `pom.xml`, `mvnw`, `mvnw.cmd`: build e dipendenze riproducibili.

Il progetto usa `javax.servlet` e richiede **Tomcat 9**. Il launcher include Tomcat 9.0.123;
Tomcat 10/11 richiederebbero una migrazione a `jakarta.servlet`.
Il driver MySQL è gestito da Maven; il vecchio JAR copiato manualmente è stato rimosso.

Per distribuire su un Tomcat 9 esterno, usare `target/ROOT.war` nella cartella `webapps`.
L'applicazione deve essere pubblicata alla radice `/`, perché molti collegamenti sono assoluti.
Rendere disponibile MySQL Connector/J anche in `TOMCAT_HOME/lib` per il datasource JNDI
(il JAR scaricato da Maven si trova anche in `target/ROOT/WEB-INF/lib`).
Impostare le proprietà JVM `-Ddb.url=...`, `-Ddb.user=...`, `-Ddb.password=...` per configurare
la risorsa `jdbc/ecommerce` in `META-INF/context.xml`.
Il file `dev.properties` è letto solo dal launcher locale.

## Risoluzione dei problemi

- **F5 non disponibile:** verificare l'estensione Java e attendere l'importazione Maven.
  Se il progetto è rimasto nello stato precedente, eseguire **Java: Clean Java Language Server Workspace**.
- **Database non disponibile:** eseguire il task di avvio del database o verificare `dev.properties`.
  Il log locale è `.local-db/mariadb.log`.
- **Porta 3307 occupata:** arrestare l'istanza che occupa la porta oppure configurare un database esterno.
  Lo script non arresta servizi estranei.
- **Importazione incompleta:** lo script si ferma se trova tabelle senza il marcatore
  `.local-db/seed-imported`; verificare i dati prima di intervenire. Non sovrascrive un database già popolato.

Il dump contiene account di esempio con password hash delle quali non sono disponibili le password originali.
Per provare l'accesso creare un nuovo account dalla pagina di registrazione.
La compilazione non comprende test automatici preesistenti; le verifiche di avvio sono descritte in `VERIFICA.md`.
