package control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test 79 di pianoTest.md: escape HTML del testo stampato nelle pagine e dei commenti delle recensioni.
 * Una classe per ciascun carattere speciale, più il testo senza caratteri speciali, null e una entità già
 * scritta, che va sottoposta a escape a sua volta (la & per prima).
 */
@DisplayName("Html - escape del testo per le pagine")
class HtmlTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" → \"{1}\"")
    @CsvSource(value = {
            "a & b, a &amp; b",
            "<script>, &lt;script&gt;",
            // in @CsvSource l'apostrofo delimita i valori: '''' è un apostrofo, '"' una virgoletta doppia
            "'''', &#039;",
            "'\"', &quot;",
            "&lt;, &amp;lt;",
            "Via Roma 1, Via Roma 1",
            "NULL, ''"
    }, nullValues = "NULL")
    @DisplayName("79 - escape sostituisce & < > \" ' con le entità HTML; null diventa la stringa vuota")
    void escape_caratteriSpeciali_entita(String testo, String atteso) {
        assertEquals(atteso, Html.escape(testo), "Escape di " + testo);
    }
}
