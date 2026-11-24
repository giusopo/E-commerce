"""Riepilogo dei test JUnit per la pagina della run di GitHub Actions.

Legge i report XML di Surefire e scrive una tabella Markdown in $GITHUB_STEP_SUMMARY
(in locale, senza quella variabile, la stampa a video). I test sono ordinati per
numero del piano (pianoTest.md), cioe' il numero all'inizio del @DisplayName.

Uso: python3 .github/scripts/test_summary.py target/surefire-reports
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

ESITI = {
    "superato": "✅ superato",
    "fallito": "❌ fallito",
    "errore": "⚠️ errore",
    "saltato": "⏭️ saltato",
}


def numero_del_piano(nome):
    """Chiave di ordinamento: "9b - ..." viene dopo "9 - ..." e prima di "10 - ..."."""
    corrispondenza = re.match(r"\s*(\d+)([a-z]?)\b", nome)
    if corrispondenza is None:
        return (sys.maxsize, "")
    return (int(corrispondenza.group(1)), corrispondenza.group(2))


def cella(testo, lunghezza_massima=200):
    """Rende il testo sicuro dentro una cella Markdown."""
    testo = " ".join(testo.split())
    if len(testo) > lunghezza_massima:
        testo = testo[: lunghezza_massima - 1] + "…"
    return testo.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("|", "\\|")


def leggi_test(cartella):
    test = []
    for percorso in sorted(glob.glob(os.path.join(cartella, "TEST-*.xml"))):
        radice = ET.parse(percorso).getroot()
        suite = [radice] if radice.tag == "testsuite" else radice.findall("testsuite")
        for s in suite:
            for caso in s.findall("testcase"):
                esito, dettaglio = "superato", None
                for tag, valore in (("failure", "fallito"), ("error", "errore"), ("skipped", "saltato")):
                    elemento = caso.find(tag)
                    if elemento is not None:
                        esito, dettaglio = valore, elemento
                        break
                messaggio = ""
                if dettaglio is not None:
                    righe = (dettaglio.get("message") or dettaglio.get("type") or "").strip().splitlines()
                    messaggio = righe[0] if righe else ""
                # Surefire unisce il nome del test e quello della variante: "...valido[1] 0 risultati".
                nome = re.sub(r"(\S)\[(\d+)\]", r"\1 [\2]", caso.get("name", ""))
                test.append({
                    "nome": nome,
                    "classe": caso.get("classname", ""),
                    "tempo": float(caso.get("time") or 0),
                    "esito": esito,
                    "messaggio": messaggio,
                })
    test.sort(key=lambda t: (numero_del_piano(t["nome"]), t["nome"]))
    return test


def componi_riepilogo(test):
    if not test:
        return ("## Test JUnit\n\nNessun report trovato: probabilmente la compilazione "
                "è fallita prima dell'esecuzione dei test.\n")

    conteggi = {esito: sum(1 for t in test if t["esito"] == esito) for esito in ESITI}
    righe = [
        "## Test JUnit",
        "",
        f"**{len(test)} test**: ✅ {conteggi['superato']} superati · ❌ {conteggi['fallito']} falliti"
        f" · ⚠️ {conteggi['errore']} errori · ⏭️ {conteggi['saltato']} saltati",
        "",
    ]

    non_superati = [t for t in test if t["esito"] in ("fallito", "errore")]
    if non_superati:
        righe += ["### Test non superati", "", "| Test | Classe | Messaggio |", "|---|---|---|"]
        righe += [f"| {cella(t['nome'])} | {cella(t['classe'])} | {cella(t['messaggio'])} |" for t in non_superati]
        righe.append("")

    righe += ["### Tutti i test, in ordine di piano", "", "| Esito | Test | Classe | Tempo (s) |", "|---|---|---|---:|"]
    righe += [f"| {ESITI[t['esito']]} | {cella(t['nome'])} | {cella(t['classe'])} | {t['tempo']:.3f} |" for t in test]
    return "\n".join(righe) + "\n"


def main():
    cartella = sys.argv[1] if len(sys.argv) > 1 else "target/surefire-reports"
    riepilogo = componi_riepilogo(leggi_test(cartella))
    destinazione = os.environ.get("GITHUB_STEP_SUMMARY")
    if destinazione:
        with open(destinazione, "a", encoding="utf-8") as file:
            file.write(riepilogo)
    else:
        sys.stdout.reconfigure(encoding="utf-8")
        print(riepilogo)


if __name__ == "__main__":
    main()
