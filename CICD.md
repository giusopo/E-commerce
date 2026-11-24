# Pipeline CI/CD

La pipeline è definita in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) ed eseguita da GitHub Actions.
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
6. **Build application** (`./mvnw -B -ntp package -DskipTests`): genera `target/ROOT.war` ( Non riesegue i test perché sono già stati eseguiti nel passo precedente).
7. **Upload WAR artifact** (`actions/upload-artifact@v4`): se tutti i passi precedenti non falliscono,
   pubblica il WAR come artefatto `beautyblind-<sha del commit>`.

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