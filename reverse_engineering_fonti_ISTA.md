# Descrivere un sistema software privo di documentazione — estratto dalle fonti del corso ISTA

> **Per Claude Code.** Questo file raccoglie, dal materiale del corso *Ingegneria del Software – Tecniche Avanzate (ISTA)*, tutto ciò che serve per **descrivere (ridocumentare) un sistema software esistente che non ha documentazione**, tramite comprensione del codice e reverse engineering.
> Ogni affermazione è marcata con un identificativo di fonte `[Sx, posizione]` (vedi tabella in §0). Quando produci la descrizione del sistema:
> 1. segui il processo in §4 e la checklist in §11;
> 2. ricava i fatti **solo dal codice del repository** (e da eventuale README/commenti), non inventare comportamenti;
> 3. quando giustifichi una scelta metodologica nel report, cita la fonte con l'ID indicato qui;
> 4. segnala esplicitamente ciò che non è stato possibile verificare dal codice.

---

## 0. Fonti

| ID | File | Tipo | Parti usate |
|----|------|------|-------------|
| S1 | `11.Software Reengineering.pptx` | Slide (Tripathy & Naik, *Software Evolution and Maintenance*, cap. 4) | slide 4–13, 21–26, 36, 38–60, 64–71 |
| S2 | `03a.Std IEEE 1219-1998 Software Maintenance.pdf` | Standard | §3.1.11, §4.2.2, A.4.2, A.12, A.13, Annex B (B.1–B.3, Tab. B.1–B.3) |
| S3 | `03c.Std ISO-IEC-IEEE 14764-2022 Software Maintenance.pdf` | Standard | §8.9 "Records and information items" (p. 29) |
| S4 | `03b.Std ISO-IEC-IEEE 14764-2006 Software Maintenance old.pdf` | Standard (ed. precedente) | §6.10 "Documentation" (p. 31) |
| S5 | `Presentazione Progetti 2026_27 2.pdf` | Slide del corso | "Regole di Ingaggio", pp. 3–19 |
| S6 | `01.Istruzioni formazione gruppi di progetto 1.pdf` | Istruzioni progetto | punto 1) della consegna |
| S7 | `08b.Program_slicing_methods_and_applications paper.pdf` | Paper (De Lucia) | §1–§7 |
| S8 | `10e.Recovering_Traceability_Links_Between_Code_and_Documentation_A_Retrospective.pdf` | Paper (Antoniol et al., IEEE TSE 2025) | §I–§II, pp. 825–828 |
| S9 | `09a.Impact Analysis.ppt` | Slide (Tripathy & Naik, cap. 6) | slide 3, 13–16, 29–33, 59–60 |
| S10 | `01.Introduction to software maintenance and evolution.ppt` | Slide (Tripathy & Naik, cap. 1) | slide 18, 24–25 |
| S11 | `12.Legacy Systems.ppt` | Slide (Tripathy & Naik, cap. 5) | slide 3, 33 |
| S12 | `13c.Migrating COBOL legacy systems to the web paper.pdf` | Paper (De Lucia et al., SP&E 2008) | §1 (p. 1334), §3.1 (p. 1339) |
| S13 | `13b.Cobol Software Migration slides.pdf` | Slide | slide 3, 7–8 |
| S14 | `10c.IR-based traceability recovery.pdf` / `09b.Traceability Management for Impact Analysis.pdf` | Slide (De Lucia & Oliveto) | 10c p. 7, p. 83; 09b p. 10 |

Nota: S9, S10, S11 sono file `.ppt` convertiti in testo; i numeri di slide si riferiscono all'ordine nel file.

---

## 1. Cosa chiede il corso (vincoli sul deliverable)

- **Fase "Comprensione"**: studiare il materiale disponibile (repository, paper, ecc.) per familiarizzare con l'applicazione e **prendere nota di quanto si apprende per il report**. [S5, p. 5]
- **Fase "Reverse Engineering"**: estrarre dal codice **tutti i modelli necessari a descrivere l'applicazione** (package diagram, class diagram, sequence diagram, ecc.). L'estrazione può essere manuale o assistita da tool. **Non basta estrarre i modelli: vanno anche commentati.** [S5, p. 7]
- I diagrammi servono a far capire le relazioni tra componenti e vanno usati anche nella descrizione del processo di **impact analysis** del report; si estraggono via reverse engineering se non già presenti nella documentazione. **Non è obbligatorio usare tool**: l'estrazione può essere manuale, a valle di un processo di comprensione del codice. [S6, punto 1]
- **Fase "Fork"**: il repository deve avere un **README** che spieghi chiaramente come compilare, buildare, installare, testare ed eseguire l'applicazione. [S5, p. 6]
- **Fase "Progettazione Modifica"**: mostrare come evolve il design **a livello dei modelli estratti in precedenza** → i modelli prodotti in reverse engineering sono la base delle fasi successive. [S5, p. 13]
- **Fase "Test Plan + Assessment"**: se la test suite esiste va verificata/completata; se non esiste va creata una test suite di partenza. [S5, p. 8]
- **Chiusura**: non esistono "documenti standard" (il "documento di Impact Analysis" non esiste); non copiare progetti degli anni precedenti. [S5, p. 18]
- **Presentazione**: dare massima attenzione al **rationale** delle scelte. [S5, p. 19]

---

## 2. Definizioni chiave (da usare nel report)

- **Reverse engineering (IEEE 1219)**: processo di estrazione di informazioni sul sistema software (inclusa la documentazione) dal codice sorgente. [S2, §3.1.11]
- **Reverse engineering (Chikofsky & Cross II)**: processo per (i) identificare i componenti di un software operativo, (ii) identificare le relazioni tra componenti, (iii) rappresentare il sistema a un livello di astrazione più alto o in un'altra forma. [S1, slide 38]
- Il reverse engineering **non modifica il sistema**: ne fornisce una vista alternativa a un diverso livello di astrazione (tipicamente ridocumentando il codice come schematics, structure chart, flow diagram). [S2, B.2]; è un processo di esame, non di cambiamento. [S10, slide 18]
- **Due obiettivi del reverse engineering** [S1, slide 38]:
  - **Redocumentation**: rivedere la descrizione dei componenti o generare viste alternative **allo stesso livello di astrazione** (es. pretty printing, CFG).
  - **Design recovery**: creare **astrazioni di progetto** a partire da codice, conoscenza degli esperti e documentazione esistente.
- **Sei obiettivi** (Chikofsky & Cross II): generare viste alternative; recuperare informazioni perse; sintetizzare livelli di astrazione più alti; rilevare side effect; facilitare il riuso; gestire la complessità. [S1, slide 40]
- **Livelli di astrazione**: Conceptual → Requirements → Design → Implementation. Il reverse engineering risale la catena (*how? → what & how? → what? → why?*); il forward engineering la discende. [S1, slide 6–7]
- **Reengineering = Reverse engineering + Δ + Forward engineering** (Jacobson & Lindstrom). [S1, slide 13]
- **Program comprehension**: costruire modelli mentali del sistema a vari livelli di astrazione, dai modelli del codice fino al dominio applicativo, ai fini di manutenzione, evoluzione e reengineering. [S10, slide 24]
- **Legacy system**: tra le caratteristiche tipiche c'è la **mancanza di documentazione consistente**, oltre a struttura degradata da anni di modifiche e linguaggi obsoleti. [S11, slide 3]

---

## 3. Base normativa: cosa fare quando la documentazione manca

### 3.1 ISO/IEC/IEEE 14764:2022, §8.9 "Records and information items" [S3, p. 29]
Chi fa manutenzione si trova spesso a mantenere prodotti con poca o nessuna documentazione; in tal caso la documentazione necessaria va creata (prima della transizione alla manutenzione o mentre la si esegue), svolgendo questi compiti:

| # | Compito | Attività concrete |
|---|---------|-------------------|
| a | **Capire il dominio del problema** (tipo di applicazione) | leggere i documenti disponibili, parlare con gli sviluppatori (se disponibili), **usare/eseguire il prodotto** |
| b | **Imparare struttura e organizzazione** (control flow, data flow, strutture dati, call graph) | fare l'inventario del prodotto, metterlo sotto CM, ricostruirlo (build) dalle librerie di CM, analizzarne la struttura |
| c | **Determinare cosa fa il prodotto** | rivedere specifiche (se ci sono) e struttura complessiva, analizzare i call tree, leggere il codice, presentarlo oralmente ad altri, **aggiungere commenti al codice** dove importante |
| d | **Costruire fiducia gradualmente** | affrontare prima modifiche a basso rischio, poi via via più rischiose e complesse |

Le informazioni vanno registrate man mano; specifiche, guide del manutentore, manuali utente e procedure di installazione vanno create o aggiornate. [S3, p. 29]
La versione 2006 (§6.10) è equivalente e aggiunge che la creazione della documentazione dipende da: accesso al sorgente, disponibilità di tool di analisi, possibilità di eseguire il prodotto, disponibilità di un ambiente di test. [S4, p. 31]

### 3.2 IEEE 1219-1998
- Nella fase di **Analysis**: se la documentazione manca o è insufficiente e **il codice sorgente è l'unica rappresentazione affidabile** del sistema, **si raccomanda il reverse engineering**. [S2, §4.2.2]
- Molti sistemi longevi hanno il codice come unica rappresentazione affidabile e documentazione mal aggiornata: qui il reverse engineering è la tecnica raccomandata per ridocumentare. [S2, B.3]
- La **software change impact analysis** dovrebbe essere svolta con l'aiuto di **documentazione astratta dal codice sorgente**. [S2, A.4.2]
- La documentazione mancante o carente è uno dei criteri della *software replacement policy*. [S2, A.13 f]

---

## 4. Processo di reverse engineering da seguire

### 4.1 I sei passi (IEEE 1219, Annex B.3 e Tab. B.2) [S2, pp. 35–37; ripresi in S1, slide 40]

**"In-the-small"** (analisi locale, a livello di unità):
1. **Suddividere il codice in unità formali**, descrivendo tutti i collegamenti (trasferimenti di controllo). Tipi di unità: subroutine; *transiting* (1→1), *branching* (1→N), *rooting* (N→1), *starting* (0→1), *ending* (1→0).
2. **Descrivere semanticamente la funzione di ogni unità formale** e dichiarare le **unità funzionali** (una unità funzionale può comprendere più unità formali o esserne una parte).
3. **Creare lo schema di I/O di ogni unità** (tutti i trasferimenti di controllo da/verso l'unità).

**"In-the-large"** (analisi globale, a livello di sistema):
4. **Mappare tutte le unità** → *program linear circuit* (LC): sequenze di unità collegate, con unità FIRST e LAST.
5. **Mappare le unità FIRST/LAST** → *program applications* (famiglie di LC che svolgono un compito specifico).
6. **Elencare tutte le applicazioni con i relativi LC** → **program anatomy** (anatomia del sistema).

Strumenti associati per l'analisi (Tab. B.3): beautifier, diagrammer, mapper, metric analyzer, structure analyzer; per il design: flow-charting, data-flow diagramming, visualizzatori, documenter. [S2, p. 38]

> **Mappatura pratica per un sistema OO moderno** (interpretazione da usare nel report, non citazione): passi 1–3 ≈ analisi di classi/metodi e loro interfacce (class diagram + descrizione delle responsabilità); passi 4–6 ≈ flussi di esecuzione end-to-end per funzionalità (sequence diagram) e vista d'insieme per moduli (package/component diagram).

### 4.2 Paradigma Goals / Models / Tools (Benedusi et al.) [S1, slide 42–46]
- **Goals**: perché si fa reverse engineering; quali informazioni produrre e con quali formalismi (es. UML).
- **Models**: quali documenti generare; quali informazioni e relazioni estrarre dal codice; quali modelli usare per rappresentarle; quale algoritmo di astrazione applicare. Proprietà desiderate di un modello: potere espressivo, indipendenza dal linguaggio, compattezza, ricchezza informativa, granularità, trasformazioni che preservano l'informazione.
- **Tools**: tool di **estrazione** (ricostruiscono design di basso livello dal codice) e tool di **astrazione** (producono design di alto livello). I tool di estrazione spesso **non bastano** per il design di alto livello.

### 4.3 Ruolo dei tool [S1, slide 71]
Il reverse engineering è un processo complesso che i tool possono **supportare ma non automatizzare**: serve sempre intervento umano. Struttura tipica di un tool: analisi del sistema → information base → view composer che produce viste alternative.

### 4.4 Strategie di comprensione [S10, slide 24–25]
- La comprensione procede per **ipotesi**: si formula un'ipotesi e la si verifica leggendo il codice, aumentando via via il dettaglio.
- Strategie: **bottom-up** (dal codice), **top-down** (dall'obiettivo di alto livello), **opportunistica** (combinazione).
- Meccanismi: **chunking** (creare astrazioni di livello più alto da strutture di livello più basso) e **cross-referencing** (collegare elementi di livelli di astrazione diversi).

### 4.5 Quando non esistono né documenti né sviluppatori [S12, §3.1, p. 1339]
Caso reale: per mancanza di documentazione si è potuto analizzare solo il **codice sorgente (analisi statica)** e il **comportamento in esecuzione (analisi dinamica)**, ponendo domande ai manutentori che conoscevano il sistema; i tool commerciali coprivano solo in parte le esigenze e sono stati sviluppati tool ad hoc. Le risorse da considerare per valutare un sistema: documentazione, codice, storia dei guasti, profilo operativo, servizi forniti, utenti finali, manutentori, manager.

---

## 5. Tecniche di analisi per il reverse engineering [S1, slide 47–60]

| Tecnica | Cosa produce / a cosa serve |
|---------|-----------------------------|
| **Analisi lessicale** | scomposizione del sorgente in unità lessicali (regex; lex/flex). [slide 48] |
| **Analisi sintattica** | parse tree e **AST**; l'AST mantiene solo ciò che riguarda il significato del programma; si interroga per tipi di nodo. [slide 49] |
| **Control flow analysis – intraprocedurale** | **CFG** basato su basic block: ordine di esecuzione delle istruzioni in un sottoprogramma. [slide 50] |
| **Control flow analysis – interprocedurale** | **call graph** (nodo = procedura, arco f→g = f chiama g). Statico = tutte le esecuzioni possibili; dinamico = traccia esatta di una singola esecuzione. [slide 51] |
| **Data flow analysis** | flusso di def/use delle variabili; rileva anomalie (variabili usate prima di essere definite, codice mai eseguito) e risponde a "quali istruzioni sono influenzate da un assegnamento?". [slide 52] |
| **Program slicing** | backward slice = cosa influenza una computazione; forward slice = cosa è influenzato da una computazione. [slide 53–55] |
| **Visualizzazione** | rappresentazione visiva della struttura; requisiti: navigazione semplice, alta informazione, bassa complessità visiva, livelli di dettaglio variabili, integrazione con altre fonti. [slide 56–57] |
| **Metriche** | fan-in/fan-out e complessità di Henry–Kafura `Cp = fan-in × fan-out` (valori alti = sintomo di cattivo design); suite **CK**: WMC, RFC, LCOM, CBO, DIT, NOC; metriche per clone detection (fan-out, complessità ciclomatica, function point…). [slide 59–60] |

**Profilo di complessità** suggerito da IEEE 1219 (utile come tabella descrittiva del sistema): dimensione del programma, numero di moduli, numero di variabili e di variabili globali, dimensione media dei moduli, numero medio di confronti per modulo, moduli comuni, moduli che accedono a più variabili globali della media, moduli oltre 50 istruzioni o 10 confronti. [S2, A.12, p. 33]

**Aree in cui il reverse engineering è stato applicato** (utili per giustificare cosa estrarre): ridocumentare programmi, identificare asset riusabili, scoprire l'architettura, **recuperare design pattern**, tracciabilità codice↔documentazione, derivare modelli concettuali dei dati, rilevare duplicazioni/cloni, code smell, calcolo del change impact, estrazione di business rule, ecc. [S1, slide 41]

---

## 6. Program slicing come supporto alla comprensione [S7]

- **Static slice** (Weiser): dato un criterio `<p, V>` (punto p, variabili V), è il sottoinsieme di istruzioni che preserva il comportamento rispetto a V in p, **per qualunque input**. Calcolabile come attraversamento all'indietro del **PDG**; versione interprocedurale sul **SDG** (Horwitz et al.). [S7, §2]
- **Decomposition slice** (Gallagher & Lyle): unione degli slice su una variabile v per tutti i punti del programma → usato in manutenzione. [S7, §2]
- **Dynamic slice** (Korel & Laski): solo le istruzioni che influenzano le variabili di interesse **in una specifica esecuzione** → slice più piccoli. [S7, §3]
- **Simultaneous dynamic slicing** (Hall): slice su un insieme di test case → usato per **localizzare una funzionalità nel codice** (i test case fungono da specifica della funzionalità). [S7, §5]
- **Conditioned slicing**: slice rispetto a un insieme di esecuzioni definito da una condizione sull'input; consente di analizzare frammenti di codice da prospettive diverse → applicato al **program comprehension**. [S7, §6]
- Applicazioni elencate: debugging, testing, **program comprehension**, **reverse engineering**, restructuring, identificazione di funzioni riusabili. [S7, §1–§2]

---

## 7. Data reverse engineering (se il sistema ha un database o file di dati) [S1, slide 64–70]

- **DRE**: uso di tecniche strutturate per ricostituire gli asset di dati di un sistema esistente; scopi tra cui acquisizione di conoscenza e **documentazione**. [slide 64]
- **DBRE**: recupera gli schemi logico e concettuale a partire da quello fisico; aiuta a capire e ridocumentare database e file dell'applicazione. [slide 65]
- Fase 1 – **Data structure extraction**: analisi del DDL; analisi del codice per vincoli di integrità e strutture implicite; analisi dei dati (campi unici, dipendenze funzionali, possibili foreign key); integrazione degli schemi parziali. [slide 68]
- Fase 2 – **Data structure conceptualization**: pulizia dello schema, *untranslation* e *de-optimization*, poi normalizzazione concettuale (rendere esplicite le is-a, standardizzare i nomi…). [slide 69–70]

> Output utile per il report: schema ER / class diagram del modello dati, commentato.

---

## 8. Collegare il codice alla documentazione che c'è (README, commenti, issue)

### 8.1 IR-based traceability recovery [S8, pp. 825–827; S14]
- La documentazione è quasi sempre testo libero; premessa: i programmatori usano **nomi significativi** per funzioni, variabili, classi, metodi. [S8, Abstract]
- Pipeline (VSM): (1) parsing e pre-processing dei termini (rimozione token non testuali, split degli identificatori composti, stop word, stemming) → bag of words; (2) **matrice termini×documenti** con pesi (es. TF-IDF); (3) similarità (coseno) tra componenti di codice e documenti; (4) **lista ordinata di link candidati**. Il processo è **semi-automatico**: l'ingegnere conferma i link corretti e scarta i falsi positivi. [S8, §II]; rappresentazione dei metodi come term-by-document matrix [S14, 10c p. 7]
- Limite principale: **mismatch di vocabolario** tra documenti e codice; i link corretti nella parte bassa della lista sono difficili da recuperare. [S8, p. 827]
- Processo **incrementale** con soglie decrescenti: ci si ferma quando lo sforzo per scartare i falsi positivi supera il beneficio. [S8, p. 827; S14, 10c p. 83]
- Approcci euristici alternativi: name tracing (regex, edit distance), corrispondenze sintattiche requisiti↔modelli, **analisi dinamica** (componenti eseguiti negli scenari), co-change dalla storia del repository. [S14, 09b p. 10]
- Applicazione affine: **feature location** (individuare il codice che implementa un concetto espresso in linguaggio naturale). [S8, p. 827]

### 8.2 Localizzare le feature nel codice [S9, slide 14–16]
- **grep / ricerca testuale**: semplice ma fallisce se il concetto non corrisponde a un identificatore. [slide 14]
- **Software reconnaissance** (Wilde & Scully): eseguire il programma con e senza la feature e marcare il codice eseguito solo nel primo caso. [slide 15]
- **Ricerca sul grafo di dipendenze** (Chen & Rajlich): partendo da `main()`, visitare una funzione alla volta leggendo documentazione, codice e grafo prima di decidere se è legata alla feature. [slide 16]

---

## 9. Dalla descrizione all'impact analysis (collegamento richiesto dal report)

- L'impact analysis identifica i componenti impattati da una Change Request; serve anche a capire come gli elementi della modifica si relazionano alla **struttura del software**. [S9, slide 3]
- Lo **Starting Impact Set (SIS)** si trova analizzando specifica della CR, documentazione e **codice sorgente**. [S9, slide 13]
- Tecniche basate su dipendenze: le dipendenze sintattiche tendono a causare dipendenze semantiche. [S9, slide 29]
  - **Call graph**: i programmatori lo usano per capire i potenziali impatti di una modifica; limite: impact set impreciso, non cattura la propagazione tramite i return. [S9, slide 30–32]
  - **PDG**: archi di dipendenza dati e di controllo; base per gli slice statici e dinamici. [S9, slide 33–39]
- Relazioni statiche **CUD** (call, use, define) come euristica di propagazione; altre euristiche: co-change storici, layout del codice (sottosistemi/file/classi), sviluppatore, similarità dei nomi. [S9, slide 59–60]

> Quindi i diagrammi estratti (package/class/sequence + call graph) vanno costruiti in modo da poter essere **riusati** per giustificare il SIS e il Candidate Impact Set. [S5, p. 13; S6]

---

## 10. Lenti utili per descrivere l'architettura

- **Decomponibilità** (Brodie & Stonebraker): un sistema è *decomposable* (UI, logica applicativa e gestione dati separate e a loro volta partizionate in parti coese e debolmente accoppiate), *semi-decomposable* (solo la UI è separata) o *non-decomposable* (black box, l'architettura peggiore). [S13, slide 7–8; S12, p. 1334; S11, slide 42]
- La decomponibilità è il principale fattore tecnico che condiziona migrazione ed evoluzione. [S13, slide 7]

---

## 11. Checklist operativa per Claude Code

1. **Inventario e build** — elenca moduli/package, dipendenze esterne, entry point; verifica di poter fare build ed esecuzione; documenta i comandi nel README. [S3 b; S5 p. 6]
2. **Dominio ed esecuzione** — esegui l'applicazione (o i test esistenti) e descrivi cosa fa dal punto di vista dell'utente. [S3 a]
3. **Unità e responsabilità (in-the-small)** — per ogni package/classe principale: responsabilità, interfaccia pubblica, input/output, dipendenze. [S2 passi 1–3]
4. **Flussi (in-the-large)** — per ogni funzionalità principale ricostruisci il percorso di esecuzione dall'entry point (call graph statico; se possibile conferma con esecuzione o test → analisi dinamica). [S2 passi 4–6; S1 slide 51]
5. **Modelli UML** — produci: package diagram, class diagram (anche per sottosistema), sequence diagram per le funzionalità chiave; ER/modello dati se c'è persistenza. **Ogni diagramma va commentato** (cosa mostra, scelte di design, punti critici). [S5 p. 7; S1 slide 64–70]
6. **Metriche e qualità** — tabella con LOC, numero di classi/metodi, CK (WMC, CBO, DIT, NOC, RFC, LCOM), fan-in/fan-out dei moduli più accoppiati; segnala code smell e design pattern riconosciuti. [S1 slide 41, 59; S2 A.12]
7. **Collegamento alla documentazione esistente** — mappa README/commenti/docstring ai componenti (tracciabilità); evidenzia le discrepanze. [S8; S14]
8. **Architettura** — stile architetturale e grado di decomponibilità (UI / logica / dati). [S13]
9. **Commenti nel codice** — dove utile, aggiungi commenti alle parti più importanti. [S3 c]
10. **Limiti** — elenca ciò che resta incerto e il metodo usato (manuale vs tool, statico vs dinamico). [S1 slide 71; S6]

---

## 12. Struttura suggerita per la sezione "Descrizione del sistema" del report

1. Scopo e dominio dell'applicazione (§3.1 a)
2. Metodologia di reverse engineering adottata, con rationale (§2, §4: IEEE 1219 + GMT + strategie di comprensione) [S2, S1, S10]
3. Ambiente, build ed esecuzione (README)
4. Architettura e vista dei package (commentata)
5. Class diagram per sottosistema (commentati)
6. Sequence diagram delle funzionalità principali (commentati)
7. Modello dei dati (se presente)
8. Metriche e osservazioni sulla qualità (smell, pattern, accoppiamento)
9. Tracciabilità con la documentazione esistente e lacune
10. Limiti dell'analisi
