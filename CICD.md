# Pipeline CI/CD

La pipeline è definita in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) ed eseguita da GitHub Actions.
Il mutation testing ha un workflow a parte, avviato a mano: vedi [Mutation testing](#mutation-testing-a-comando).
Il compito è verificare automaticamente che ogni modifica al codice superi i test, offrendo un livello di verifica ulteriore prima che il programma arrivi al deploy.
## Quando parte

- **`push`** sui branch `main` o `master`;
- **`pull_request`** verso `main` o `master`;

## Fasi del job `build-and-test`

Il job gira su un runner `ubuntu-latest`grazie a GitHub.

1. **Checkout code** (`actions/checkout@v5`): scarica il commit da verificare.

2. **Set up JDK 21** (`actions/setup-java@v4`): installa il JDK Temurin 21. Con `cache: maven`, quindi le dipendenze scaricate vengono riutilizzate con l'ausilio di una chiave calcolata sul `pom.xml`.

3. **Run tests** (`./mvnw -B -ntp clean test`): parte da una cartella `target/` ripulita, compila il codice e successivamente esegue
   la test suite. In caso di errore il job si ferma e l'esecuzione fallisce.
4. **Upload test results** (`actions/upload-artifact@v4`, `if: always()`): pubblica i report JUnit di
   `target/surefire-reports/` come artefatto `test-results`, anche quando i test falliscono.
5. **Test summary** (`.github/scripts/test_summary.py`, `if: always()`): legge gli stessi report e scrive
   nella pagina della run il conteggio dei test, i test non superati con il loro messaggio e la tabella
   completa ordinata per numero del piano dei test. Non usa azioni esterne né permessi aggiuntivi.
6. **Generate test report** (`dorny/test-reporter@v2`): pubblica i risultati dei test come check
   "Maven Test Results". È l'unico passo che richiede un permesso in più (`checks: write`).
7. **Generate JaCoCo report** (`./mvnw jacoco:report`): produce `target/site/jacoco/jacoco.xml`.
8. **Upload coverage to Codecov** e **Upload test results to Codecov**: inviano copertura e risultati dei
   test a Codecov, con il token salvato nel secret `CODECOV_TOKEN`.
9. **Run JaCoCo quality gates** (`./mvnw jacoco:check@check`): applica le soglie di copertura del `pom.xml`
   ai dati dei test appena eseguiti; sotto soglia il job fallisce.
10. **Build application** (`./mvnw -B -ntp package -DskipTests`): genera `target/ROOT.war` ( Non riesegue i test perché sono già stati eseguiti nel passo precedente).
11. **Upload WAR artifact** (`actions/upload-artifact@v4`): se tutti i passi precedenti non falliscono,
   pubblica il WAR come artefatto `beautyblind-<sha del commit>`.

## Segnale e blocco sulla copertura

| | Chi lo dà | Quando | Effetto |
|---|---|---|---|
| **Segnale** | Codecov, con i controlli `codecov/project` e `codecov/patch` ([codecov.yml](codecov.yml)) | la copertura del perimetro scende rispetto al commit di base, oppure le righe nuove o modificate sono coperte meno dell'80% | una ✗ sul commit e sulla pull request, e il commento di Codecov con le differenze; l'unione resta possibile |
| **Blocco** | JaCoCo, nel passo *Run JaCoCo quality gates* | un package scende sotto righe 80%, branch 75% o classi 100% | il job `Build and Test with Java 21` fallisce; se il check è obbligatorio su `main`, la pull request non si può unire |

Il blocco non dipende da Codecov: le soglie sono applicate nella CI, quindi valgono anche se l'invio a
Codecov non riesce (`fail_ci_if_error: false`). Servono due impostazioni fuori dal codice:

- l'app GitHub di Codecov installata sul repository, che pubblica i controlli e il commento: è
  installata;
- una regola su `main` (*Settings* → *Rules* → *Rulesets*) che rende obbligatorio il check
  `Build and Test with Java 21`, senza aggiungere i controlli di Codecov. Non è ancora attiva: per ora
  una soglia non raggiunta rende rossa la CI ma non impedisce l'unione.

## Mutation testing a comando

Il mutation testing con PIT richiede alcuni minuti: eseguito a ogni push consumerebbe la quota di GitHub
Actions, come avverte il tutorial del Lab 4. Per questo non fa parte della pipeline: è definito in
[`.github/workflows/mutation.yml`](.github/workflows/mutation.yml) e parte solo a mano, dalla scheda
*Actions* → *Mutation Testing* → *Run workflow* (`workflow_dispatch`).

- **Campi del form** (facoltativi): classi da mutare e test da eseguire, separati da virgola, per
  analizzare solo una parte del codice. Vuoti, si usa il perimetro del `pom.xml`. Sono accettati solo
  caratteri da nome di classe.
- **Run PITest mutation testing with quality gates**: stesse soglie del `pom.xml` (mutation score e
  copertura delle righe all'80%); sotto soglia il job fallisce.
- **Mutation summary**: scrive nella pagina della run i mutanti uccisi sul totale.
- **Upload PIT report**: carica `target/pit-reports/` come artefatto `pit-report`, anche se la soglia non
  è raggiunta.

Il risultato non blocca la CI né la pubblicazione dell'immagine Docker, che dipendono solo da `ci.yml`.

## Riproducibilità della build

- **Maven Wrapper**: la pipeline usa `./mvnw`e scarica Maven 3.9.16 come indicato in
  `.mvn/wrapper/maven-wrapper.properties`. La build non dipende dal Maven installato sul runner o sul PC.
- **Versioni fissate**: nel `pom.xml` sono definite le versioni delle dipendenze (Tomcat 9.0.123,
  MySQL Connector/J 8.4.0) e dei plugin (compiler, war, exec). Il target di compilazione è Java 11
  (`maven.compiler.release`).
- **JDK fissato**: in CI usiamo sempre la distribuzione Temurin 21.
- **Runner effimeri**: ogni run parte da una nuova macchina virtuale, che viene distrutta alla fine, in questo modo non restano file, configurazioni o stato dai run precedenti.
- **Artefatti immutabili**: il WAR viene costruito una sola volta dalla pipeline e il nome dell'artefatto contiene lo SHA del commit.

## Qualità del codice e dependability

- **Verifica automatica e feedback rapido**: ogni push e ogni pull request vengono compilati e testati senza
  interventi manuali. Nella scheda *Actions* è visibile il risultato.
- **Fallimento esplicito**: Una versione che non supera i controlli non viene distribuita.
- **Privilegio minimo**: il workflow dichiara `permissions: contents: read`, quindi il `GITHUB_TOKEN` del job
  può solo leggere il repository.