# BeautyBlind

[![CI/CD Pipeline](https://github.com/giusopo/E-commerce/actions/workflows/ci.yml/badge.svg)](https://github.com/giusopo/E-commerce/actions/workflows/ci.yml)
[![Docker Build](https://github.com/giusopo/E-commerce/actions/workflows/docker-build.yml/badge.svg)](https://github.com/giusopo/E-commerce/actions/workflows/docker-build.yml)
[![codecov](https://codecov.io/gh/giusopo/E-commerce/branch/main/graph/badge.svg)](https://codecov.io/gh/giusopo/E-commerce)
[![Mutation score](https://img.shields.io/badge/mutation%20score-80%25-brightgreen)](mutation.md)

Applicazione e-commerce Java con Servlet, JSP, JDBC e database MySQL/MariaDB.
Gestisce catalogo, immagini, ricerca, recensioni, utenti, carrello, ordini e area amministrativa.

## Integrazione continua

Ogni push e ogni pull request verso `main` vengono compilati e testati automaticamente con GitHub Actions,
che produce il pacchetto `ROOT.war` come artefatto.
Fasi della pipeline, riproducibilità della build e controlli di qualità sono descritti in [CICD.md](CICD.md).

## Test

I test di unità usano JUnit 6 e Mockito 5 e si trovano in `src/test/java`. Il database non viene usato:
connessioni, query e risultati sono simulati con Mockito, quindi i test sono veloci, indipendenti e
ripetibili. Coprono carrello, ordini, catalogo, utenti, recensioni e azioni dell'amministratore, con
attenzione ai casi limite e agli errori. La pipeline li esegue a ogni push e pull request e mostra nella
pagina della run una tabella dei risultati.

```powershell
./mvnw test
```

Strategia, principi FIRST, difetti trovati e limiti sono descritti in [testing.md](testing.md).
L'elenco dei singoli test è in [pianoTest.md](pianoTest.md).

### Copertura e mutation testing

JaCoCo misura la copertura a ogni push: la pipeline la invia a Codecov e fallisce se le soglie non sono
raggiunte. PIT misura il mutation score ed è lento, quindi non gira a ogni push: si avvia a mano dal
workflow *Mutation Testing* nella scheda *Actions* (vedi [CICD.md](CICD.md)), oppure in locale.

- [coverage.md](coverage.md): configurazione di JaCoCo e Codecov, perimetro misurato, analisi e piano di miglioramento.
- [mutation.md](mutation.md): configurazione di PIT, analisi dei mutanti sopravvissuti e piano di miglioramento.
- [metriche.md](metriche.md): andamento di copertura e mutation score nelle varie fasi del lavoro.

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
