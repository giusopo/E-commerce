# Containerizzazione

BeautyBlind gira in due container orchestrati da [`docker-compose.yml`](docker-compose.yml): `app` (Tomcat 9)
e `db` (MariaDB). L'immagine dell'app viene pubblicata su Docker Hub da
[`docker-build.yml`](.github/workflows/docker-build.yml).

## Dockerfile

- Base `tomcat:9.0.124-jre21-temurin-noble`: Tomcat 9, richiesto da `javax.servlet`, e solo JRE.
- Copia `ROOT.war` (app su `/`) e il driver MySQL in `tomcat/lib`, perché il datasource JNDI lo crea Tomcat.
- Utente `beautyblind` senza shell (UID 10001), con scrittura solo su `webapps`, `work`, `temp`, `logs`.
- `ENTRYPOINT` legge i secrets e termina con `exec`: Tomcat è il PID 1 e riceve `SIGTERM`.
- `.dockerignore` a lista consentita: entrano solo WAR, driver ed entrypoint.

**Anti-pattern evitati:** tag `:latest`; `ADD` per file locali; esecuzione come root; segreti
nell'immagine o nel codice; `CMD` in forma shell; contesto di build con `.git` o file locali.

## docker-compose.yml

| Servizio | Healthcheck | Ripristino | Dati |
|---|---|---|---|
| `db` `mariadb:11.4.13` | `healthcheck.sh --connect --innodb_initialized` | `restart: on-failure` | volume `db_data` |
| `app` | `curl` sulla home, che legge il catalogo dal DB | `restart: on-failure` | - |

- `app` parte solo con `db` sano (`depends_on: condition: service_healthy`).
- Al primo avvio il volume vuoto importa `ecommerce.sql`; `02-privileges.sql` riduce l'utente
  `beautyblind_app` a `SELECT/INSERT/UPDATE/DELETE`. `root` accede solo da `localhost` nel container.
- La porta del database non è pubblicata.

### Gestione dei segreti
`scripts/New-DockerSecrets.ps1` genera password casuali in `secrets/`, esclusa da Git. Compose le monta
in `/run/secrets`. MariaDB le legge con `MARIADB_*_PASSWORD_FILE`. Per Tomcat, `docker/app/entrypoint.sh`
scrive la password in una copia privata (`0600`) di `catalina.properties`, passata con `-Dcatalina.config`.
La password non compare in `docker inspect`, nelle variabili d'ambiente o negli argomenti dei processi.

## Esperimento di fault injection

1. `docker compose stop db`: la home risponde 500, il log mostra `Communications link failure`.
   `app` resta attivo e dopo circa 45 s diventa `unhealthy`.
2. `docker compose start db`: con `db` di nuovo `healthy`, l'app si riconnette da sola entro 5 s,
   senza riavvio. Lo permettono `testOnBorrow` e `validationQuery` del pool in `context.xml`.
3. `docker compose down` e `up -d`: account e dati creati prima sono ancora presenti grazie al volume.

**Limite osservato:** `restart: on-failure` agisce solo se il processo termina. Un'app `unhealthy` ma
attiva non viene riavviata da Docker Compose: il ripristino dipende dalla riconnessione del pool.

**Difetto emerso:** nel container il login falliva. Le query usavano `Ordine` per la tabella `ordine`.
MariaDB su Windows ignora le maiuscole nei nomi delle tabelle, su Linux no
(`lower_case_table_names=0`). Il container ha mostrato in sviluppo un errore che sarebbe
comparso su qualsiasi server Linux.

## Workflow Docker Build

1. **Trigger:** `workflow_run` su `CI/CD Pipeline`, branch `main`, con
   `if: conclusion == 'success'`. Pubblica solo codice compilato e testato. Costruisce lo stesso
   commit verificato (`head_sha`). Avvio manuale con `workflow_dispatch`.
2. **Credenziali:** `docker/login-action` usa i secrets `DOCKER_USERNAME` e `DOCKER_PASSWORD`, un
   access token revocabile Read & Write, non la password. Nei log sono mascherati e il `GITHUB_TOKEN`
   ha solo `contents: read`.
3. **Piattaforme:** `linux/amd64` e `linux/arm64`, così la stessa immagine gira su PC e server x86,
   Mac Apple Silicon e server ARM. QEMU emula arm64 per le istruzioni `RUN`.
4. **Cache:** Buildx salva i livelli nella cache di GitHub Actions (`type=gha`, `mode=max`). Si
   ricostruiscono solo i livelli cambiati, di solito il WAR.
5. **Tag:** `docker/metadata-action` crea `latest` e `sha-<commit>`. Il secondo è immutabile e lega
   l'immagine al codice sorgente.

## Comandi

```powershell
powershell -File scripts/New-DockerSecrets.ps1
./mvnw package -DskipTests
docker compose up -d --build
```
