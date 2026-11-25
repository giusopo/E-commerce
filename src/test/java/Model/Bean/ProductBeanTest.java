package Model.Bean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test 10-10d di pianoTest.md: contratto equals/hashCode e isEmpty di ProductBean.
 *
 * equals e isEmpty confrontano gli stessi 10 campi. Per ciascuno c'è una classe non valida
 * ("solo questo campo è diverso"), provata in un caso a sé: così ogni condizione dei due metodi
 * diventa falsa almeno una volta e un campo dimenticato nel confronto verrebbe scoperto.
 */
@DisplayName("ProductBean - contratto equals/hashCode e isEmpty")
class ProductBeanTest {

    private static ProductBean rossetto() {
        ProductBean p = new ProductBean();
        p.setID(1);
        p.setNome("Rossetto");
        p.setPrezzo(12.5);
        p.setCategoria("labbra");
        return p;
    }

    /**
     * Modifica di un solo campo, con un valore diverso sia da quello del rossetto
     * sia da quello di un bean appena creato.
     */
    static Stream<Arguments> unSoloCampoCambiato() {
        return Stream.of(
                campo("ID", p -> p.setID(99)),
                campo("nome", p -> p.setNome("Mascara")),
                campo("descrizione", p -> p.setDescrizione("Waterproof")),
                campo("prezzo", p -> p.setPrezzo(9.99)),
                campo("grammatura", p -> p.setGrammatura(7.5)),
                campo("unità", p -> p.setUnita("ml")),
                campo("categoria", p -> p.setCategoria("occhi")),
                campo("quantità in magazzino", p -> p.setQTA_magazzino(3)),
                campo("percentuale di sconto", p -> p.setPercentualeSconto(15)),
                campo("colorazione", p -> p.setColorazione("nero")));
    }

    private static Arguments campo(String nome, Consumer<ProductBean> modifica) {
        return Arguments.of(Named.of(nome, modifica));
    }

    @Test
    @DisplayName("10 - oggetti uguali hanno lo stesso hashCode")
    void productBean_oggettiUguali_stessoHashCode() {
        ProductBean a = rossetto();
        ProductBean b = rossetto();
        Set<ProductBean> insieme = new HashSet<>(Set.of(a));

        // Guardia: se a e b non sono uguali, confrontare gli hashCode non ha senso.
        assertEquals(a, b, "Precondizione: i due prodotti sono uguali per equals()");
        assertAll(
                () -> assertEquals(a.hashCode(), b.hashCode(),
                        "Oggetti uguali per equals() devono avere lo stesso hashCode()"),
                () -> assertTrue(insieme.contains(b),
                        "Un HashSet che contiene a dovrebbe trovare anche b, uguale ad a"));
    }

    @ParameterizedTest(name = "[{index}] {0} diverso")
    @MethodSource("unSoloCampoCambiato")
    @DisplayName("10b - equals: prodotti che differiscono in un solo campo non sono uguali")
    void equals_unSoloCampoDiverso_prodottiDiversi(Consumer<ProductBean> modifica) {
        ProductBean diverso = rossetto();
        modifica.accept(diverso);

        assertNotEquals(rossetto(), diverso, "Un campo diverso dovrebbe rendere i prodotti diversi per equals()");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @NullSource
    @ValueSource(strings = "Rossetto")
    @DisplayName("10c - equals: un prodotto è diverso da null e da un oggetto di un'altra classe")
    void equals_nullOAltraClasse_false(Object altro) {
        assertFalse(rossetto().equals(altro), "equals() dovrebbe restituire false con " + altro);
    }

    @ParameterizedTest(name = "[{index}] {0} valorizzato")
    @MethodSource("unSoloCampoCambiato")
    @DisplayName("10d - isEmpty: un bean nuovo è vuoto, con un solo campo valorizzato non lo è più")
    void isEmpty_unSoloCampoValorizzato_false(Consumer<ProductBean> modifica) {
        ProductBean bean = new ProductBean();
        // Guardia: il bean appena creato deve essere vuoto, altrimenti il controllo successivo non dice nulla.
        assertTrue(bean.isEmpty(), "Un ProductBean appena creato dovrebbe essere vuoto");

        modifica.accept(bean);

        assertFalse(bean.isEmpty(), "Con un campo valorizzato il bean non dovrebbe risultare vuoto");
    }
}
