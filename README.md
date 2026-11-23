# BeautyBlind

[![CI/CD Pipeline](https://github.com/giusopo/E-commerce/actions/workflows/ci.yml/badge.svg)](https://github.com/giusopo/E-commerce/actions/workflows/ci.yml)
[![Docker Build](https://github.com/giusopo/E-commerce/actions/workflows/docker-build.yml/badge.svg)](https://github.com/giusopo/E-commerce/actions/workflows/docker-build.yml)

Applicazione e-commerce Java con Servlet, JSP, JDBC e database MySQL/MariaDB.
Gestisce catalogo, immagini, ricerca, recensioni, utenti, carrello, ordini e area amministrativa.

## Integrazione continua

Ogni push e ogni pull request verso `main` vengono compilati e testati automaticamente con GitHub Actions,
che produce il pacchetto `ROOT.war` come artefatto.
Fasi della pipeline, riproducibilità della build e controlli di qualità sono descritti in [CICD.md](CICD.md).

## Avvio con Docker

L'applicazione e il database MariaDB si avviano con Docker Compose; le password sono gestite con i secrets
di Compose e non sono nel repository. Dalla radice del progetto:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/New-DockerSecrets.ps1
./mvnw package -DskipTests
docker compose up -d --build
```

L'applicazione è su http://localhost:8080/. Dopo ogni push su `main` che supera la pipeline, l'immagine viene
pubblicata su Docker Hub. Scelte, gestione dei segreti ed esperimento di fault injection sono descritti
in [Docker.md](Docker.md).

## Esecuzione in locale da VS Code (Non fa parte del corso)

Per lo sviluppo quotidiano, con debug (F5) e riavvii rapidi, il progetto si può avviare anche senza Docker,
con un database MariaDB portabile. Avvio, credenziali locali, comandi PowerShell, uso di un database
esistente e risoluzione dei problemi sono descritti in [esecuzioneLocaleVS.md](esecuzioneLocaleVS.md).

## Struttura

- `src/main/java/control`, `admin`, `GestioneIMG`: controller Servlet e filtro di accesso.
- `src/main/java/Model`: accesso JDBC, bean e interfacce.
- `src/main/webapp`: JSP, CSS, JavaScript e configurazione JNDI.
- `src/main/db/ecommerce.sql`: schema e dati iniziali.
- `src/main/java/dev/LocalServer.java`: launcher Tomcat per sviluppo e debug, escluso dal WAR.
- `.vscode/`: configurazioni F5, task di build e gestione del database.
- `pom.xml`, `mvnw`, `mvnw.cmd`: build e dipendenze riproducibili.

Il progetto usa `javax.servlet` e richiede **Tomcat 9**. Il launcher include Tomcat 9.0.123.
Il driver MySQL è gestito da Maven.
