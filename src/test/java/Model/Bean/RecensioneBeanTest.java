package Model.Bean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test 10e-10f di pianoTest.md: contratto equals/hashCode di RecensioneBean, che prima ridefiniva equals
 * senza hashCode e senza confrontare l'autore.
 */
@DisplayName("RecensioneBean - contratto equals/hashCode")
class RecensioneBeanTest {

    private static RecensioneBean recensione(int autore) {
        RecensioneBean r = new RecensioneBean();
        r.setID(7);
        r.setDataInserimento(Date.valueOf("2026-10-15"));
        r.setVotazione(4);
        r.setCommento("Ottima tenuta");
        r.setPRODOTTO_ID(12);
        r.setUserID(autore);
        return r;
    }

    @Test
    @DisplayName("10e - recensioni uguali hanno lo stesso hashCode")
    void recensioniUguali_stessoHashCode() {
        RecensioneBean a = recensione(30);
        RecensioneBean b = recensione(30);
        Set<RecensioneBean> insieme = new HashSet<>(Set.of(a));

        // Guardia: se a e b non sono uguali, confrontare gli hashCode non ha senso.
        assertEquals(a, b, "Precondizione: le due recensioni sono uguali per equals()");
        assertAll(
                () -> assertEquals(a.hashCode(), b.hashCode(),
                        "Oggetti uguali per equals() devono avere lo stesso hashCode()"),
                () -> assertTrue(insieme.contains(b), "Un HashSet che contiene a dovrebbe trovare anche b"));
    }

    @Test
    @DisplayName("10f - recensioni che differiscono solo per l'autore non sono uguali")
    void autoreDiverso_recensioniDiverse() {
        assertNotEquals(recensione(30), recensione(31), "L'autore fa parte della recensione");
    }
}
