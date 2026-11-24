package Model.Bean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test 10 di pianoTest.md: contratto equals/hashCode di ProductBean.
 */
@DisplayName("ProductBean - contratto equals/hashCode")
class ProductBeanTest {

    private static ProductBean rossetto() {
        ProductBean p = new ProductBean();
        p.setID(1);
        p.setNome("Rossetto");
        p.setPrezzo(12.5);
        p.setCategoria("labbra");
        return p;
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
}
