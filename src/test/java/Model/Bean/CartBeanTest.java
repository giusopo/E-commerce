package Model.Bean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test 1-9 di pianoTest.md: il carrello dell'utente non loggato (CartBean).
 *
 * Il servlet, prima di incrementare o decrementare, ricarica il prodotto dal database,
 * quindi il prodotto passato a CartBean è un oggetto nuovo e non quello già nel carrello.
 * I test lo simulano creando un nuovo ProductBean con lo stesso ID.
 *
 * I test 7, 8 e 9 descrivono il comportamento corretto e oggi falliscono: fanno emergere bug del carrello.
 */
@DisplayName("CartBean - carrello ospite")
class CartBeanTest {

    private CartBean cart;

    @BeforeEach
    void init() {
        cart = new CartBean();
    }

    /** Crea un prodotto con dati fissi, così i test sono ripetibili. */
    private static ProductBean prodotto(int id, int qtaOrdinata) {
        ProductBean p = new ProductBean();
        p.setID(id);
        p.setNome("Prodotto " + id);
        p.setPrezzo(10.0);
        p.setPercentualeSconto(0);
        p.setQTA_magazzino(50);
        p.setQTA_ordinata(qtaOrdinata);
        return p;
    }

    /** Lo stesso prodotto come lo restituirebbe doRetrieveByKey: stessi dati, quantità ordinata 0. */
    private static ProductBean ricaricatoDalDb(int id) {
        return prodotto(id, 0);
    }

    @Test
    @DisplayName("1 - addProduct aggiunge il prodotto al carrello")
    void addProduct_prodottoNuovo_vieneAggiuntoAllaLista() {
        ProductBean p = prodotto(1, 1);

        cart.addProduct(p);

        assertEquals(1, cart.getProducts().size(), "Il carrello dovrebbe contenere 1 prodotto");
        assertSame(p, cart.getProducts().get(0), "Il prodotto nel carrello dovrebbe essere quello aggiunto");
    }

    @Test
    @DisplayName("2 - deleteProduct rimuove per ID e ignora un ID assente")
    void deleteProduct_prodottoPresenteOAssente_rimuovePerIdOppureNonCambiaNulla() {
        cart.addProduct(prodotto(1, 1));
        cart.addProduct(prodotto(2, 1));

        cart.deleteProduct(prodotto(99, 1));
        assertEquals(2, cart.getProducts().size(), "Un ID assente non dovrebbe modificare il carrello");

        ProductBean stessoIdPrezzoDiverso = prodotto(1, 1);
        stessoIdPrezzoDiverso.setPrezzo(99.0);
        cart.deleteProduct(stessoIdPrezzoDiverso);
        assertEquals(1, cart.getProducts().size(), "Il prodotto con ID 1 dovrebbe essere rimosso");
        assertEquals(2, cart.getProducts().get(0).getID(), "Nel carrello dovrebbe restare solo il prodotto 2");
    }

    @Test
    @DisplayName("3 - getQuantitàOrdinata restituisce la quantità oppure -1")
    void getQuantitaOrdinata_prodottoPresenteOAssente_restituisceQuantitaOppureMenoUno() {
        cart.addProduct(prodotto(1, 4));

        assertAll(
                () -> assertEquals(4, cart.getQuantitàOrdinata(ricaricatoDalDb(1)),
                        "Dovrebbe restituire la quantità ordinata (4)"),
                () -> assertEquals(-1, cart.getQuantitàOrdinata(ricaricatoDalDb(2)),
                        "Per un prodotto assente dovrebbe restituire -1"));
    }

    @Test
    @DisplayName("4 - incremento di un prodotto identico: quantità da 1 a 2")
    void incrementa_prodottoIdentico_quantitaDaUnoADue() {
        cart.addProduct(prodotto(1, 1));

        cart.incrementaQuantitàOrdinata(ricaricatoDalDb(1));

        assertEquals(2, cart.getQuantitàOrdinata(ricaricatoDalDb(1)), "La quantità dovrebbe passare da 1 a 2");
    }

    @Test
    @DisplayName("5 - decremento da 3: quantità diventa 2")
    void decrementa_quantitaTre_quantitaDiventaDue() {
        cart.addProduct(prodotto(1, 3));

        cart.rimuoviSingolaQuantitàOrdinata(ricaricatoDalDb(1));

        assertEquals(2, cart.getQuantitàOrdinata(ricaricatoDalDb(1)), "La quantità dovrebbe passare da 3 a 2");
    }

    @Test
    @DisplayName("6 - decremento da 1: il prodotto viene rimosso")
    void decrementa_quantitaUno_prodottoRimosso() {
        cart.addProduct(prodotto(1, 1));

        cart.rimuoviSingolaQuantitàOrdinata(ricaricatoDalDb(1));

        assertTrue(cart.getProducts().isEmpty(), "Il carrello dovrebbe essere vuoto");
    }

    @Test
    @DisplayName("7 - incremento dopo un cambio di sconto: la quantità aumenta")
    void incrementa_stessoIdMaScontoOMagazzinoCambiati_quantitaAumenta() {
        cart.addProduct(prodotto(1, 1));
        ProductBean ricaricato = ricaricatoDalDb(1);
        ricaricato.setPercentualeSconto(20); // l'admin ha applicato uno sconto

        cart.incrementaQuantitàOrdinata(ricaricato);

        assertEquals(2, cart.getQuantitàOrdinata(ricaricato),
                "Il prodotto ha lo stesso ID: la quantità dovrebbe passare da 1 a 2");
    }

    @Test
    @DisplayName("8 - decremento dopo un cambio del magazzino: la quantità diminuisce")
    void decrementa_stessoIdMaScontoOMagazzinoCambiati_quantitaDiminuisce() {
        cart.addProduct(prodotto(1, 3));
        ProductBean ricaricato = ricaricatoDalDb(1);
        ricaricato.setQTA_magazzino(49); // un altro cliente ne ha acquistato uno

        cart.rimuoviSingolaQuantitàOrdinata(ricaricato);

        assertEquals(2, cart.getQuantitàOrdinata(ricaricato),
                "Il prodotto ha lo stesso ID: la quantità dovrebbe passare da 3 a 2");
    }

    @Test
    @DisplayName("9 - decremento da 1: il prodotto rimosso non viene modificato")
    void decrementa_quantitaUno_prodottoRimossoNonVieneModificato() {
        ProductBean nelCarrello = prodotto(1, 1);
        cart.addProduct(nelCarrello);

        cart.rimuoviSingolaQuantitàOrdinata(ricaricatoDalDb(1));

        assertAll(
                () -> assertTrue(cart.getProducts().isEmpty(), "Il prodotto dovrebbe essere rimosso"),
                () -> assertEquals(1, nelCarrello.getQTA_ordinata(),
                        "Il prodotto già rimosso non dovrebbe essere modificato (quantità 1)"));
    }
}
