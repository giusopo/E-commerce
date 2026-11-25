package Model.Bean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test 1-9d di pianoTest.md: il carrello dell'utente non loggato (CartBean).
 *
 * Il servlet, prima di incrementare o decrementare, ricarica il prodotto dal database,
 * quindi il prodotto passato a CartBean è un oggetto nuovo e non quello già nel carrello.
 * I test lo simulano creando un nuovo ProductBean con lo stesso ID.
 *
 * Classi di equivalenza: posizione del prodotto nel carrello (unico, non in prima posizione,
 * assente), quantità ordinata (1, cioè rimozione, oppure almeno 2, con 2 come valore limite)
 * e dati ricaricati dal DB (uguali oppure con sconto o magazzino cambiati).
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
    @DisplayName("2a - deleteProduct con un ID assente non modifica il carrello")
    void deleteProduct_idAssente_carrelloInvariato() {
        cart.addProduct(prodotto(1, 1));
        cart.addProduct(prodotto(2, 1));

        cart.deleteProduct(prodotto(99, 1));

        assertEquals(2, cart.getProducts().size(), "Un ID assente non dovrebbe modificare il carrello");
    }

    @Test
    @DisplayName("2b - deleteProduct rimuove il prodotto con lo stesso ID anche se il prezzo è diverso")
    void deleteProduct_stessoIdPrezzoDiverso_prodottoRimosso() {
        cart.addProduct(prodotto(1, 1));
        cart.addProduct(prodotto(2, 1));
        ProductBean stessoIdPrezzoDiverso = prodotto(1, 1);
        stessoIdPrezzoDiverso.setPrezzo(99.0);

        cart.deleteProduct(stessoIdPrezzoDiverso);

        // Guardia: senza un solo prodotto rimasto, get(0) non avrebbe senso.
        assertEquals(1, cart.getProducts().size(), "Il prodotto con ID 1 dovrebbe essere rimosso");
        assertEquals(2, cart.getProducts().get(0).getID(), "Nel carrello dovrebbe restare solo il prodotto 2");
    }

    // Classe valida (prodotto presente) e classe non valida (prodotto assente) in due casi distinti.
    @ParameterizedTest(name = "[{index}] prodotto {0} → {1}")
    @CsvSource({"1, 4", "2, -1"})
    @DisplayName("3 - getQuantitàOrdinata restituisce la quantità oppure -1")
    void getQuantitaOrdinata_prodottoPresenteOAssente_restituisceQuantitaOppureMenoUno(int id, int quantitaAttesa) {
        cart.addProduct(prodotto(1, 4));

        assertEquals(quantitaAttesa, cart.getQuantitàOrdinata(ricaricatoDalDb(id)),
                "Quantità ordinata del prodotto " + id + " (-1 se non è nel carrello)");
    }

    @Test
    @DisplayName("4 - incremento di un prodotto identico: quantità da 1 a 2, gli altri prodotti non cambiano")
    void incrementa_prodottoIdentico_quantitaDaUnoADue() {
        // Il prodotto da incrementare è il secondo: un incremento applicato sempre al primo verrebbe scoperto.
        cart.addProduct(prodotto(2, 5));
        cart.addProduct(prodotto(1, 1));

        cart.incrementaQuantitàOrdinata(ricaricatoDalDb(1));

        assertAll(
                () -> assertEquals(2, cart.getQuantitàOrdinata(ricaricatoDalDb(1)),
                        "La quantità dovrebbe passare da 1 a 2"),
                () -> assertEquals(5, cart.getQuantitàOrdinata(ricaricatoDalDb(2)),
                        "L'altro prodotto non dovrebbe cambiare"));
    }

    // 2 è il valore limite: è la quantità più piccola che si decrementa invece di essere rimossa (test 6).
    @ParameterizedTest(name = "[{index}] da {0} a {1}")
    @CsvSource({"3, 2", "2, 1"})
    @DisplayName("5 - decremento di una quantità di almeno 2: diminuisce di 1, gli altri prodotti non cambiano")
    void decrementa_quantitaAlmenoDue_quantitaDiminuisceDiUno(int quantita, int quantitaAttesa) {
        cart.addProduct(prodotto(2, 5));
        cart.addProduct(prodotto(1, quantita));

        cart.rimuoviSingolaQuantitàOrdinata(ricaricatoDalDb(1));

        assertAll(
                () -> assertEquals(quantitaAttesa, cart.getQuantitàOrdinata(ricaricatoDalDb(1)),
                        "La quantità dovrebbe passare da " + quantita + " a " + quantitaAttesa),
                () -> assertEquals(5, cart.getQuantitàOrdinata(ricaricatoDalDb(2)),
                        "L'altro prodotto non dovrebbe cambiare"));
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

    @ParameterizedTest(name = "[{index}] prodotto {0} → {1}")
    @CsvSource({"1, true", "2, false"})
    @DisplayName("9b - contiene riconosce un prodotto per ID anche se lo sconto è cambiato, ma non un ID diverso")
    void contiene_scontoCambiato_veroSoloPerLoStessoId(int id, boolean atteso) {
        cart.addProduct(prodotto(1, 1));
        ProductBean ricaricato = ricaricatoDalDb(id);
        ricaricato.setPercentualeSconto(20);

        assertEquals(atteso, cart.contiene(ricaricato),
                "Nel carrello c'è solo il prodotto 1: contiene(" + id + ") dovrebbe essere " + atteso);
    }

    // Prodotto assente: la specifica non dice cosa deve succedere, il test fissa il comportamento
    // più prudente, cioè nessuna modifica al carrello.
    @Test
    @DisplayName("9c - incremento di un prodotto assente: il carrello non cambia")
    void incrementa_prodottoAssente_carrelloInvariato() {
        cart.addProduct(prodotto(1, 1));

        cart.incrementaQuantitàOrdinata(ricaricatoDalDb(2));

        assertAll(
                () -> assertEquals(1, cart.getProducts().size(), "Il prodotto assente non dovrebbe essere aggiunto"),
                () -> assertEquals(1, cart.getQuantitàOrdinata(ricaricatoDalDb(1)),
                        "Il prodotto nel carrello non dovrebbe cambiare"));
    }

    @Test
    @DisplayName("9d - decremento di un prodotto assente: il carrello non cambia")
    void decrementa_prodottoAssente_carrelloInvariato() {
        cart.addProduct(prodotto(1, 1));

        cart.rimuoviSingolaQuantitàOrdinata(ricaricatoDalDb(2));

        assertAll(
                () -> assertEquals(1, cart.getProducts().size(), "Il prodotto nel carrello non dovrebbe essere rimosso"),
                () -> assertEquals(1, cart.getQuantitàOrdinata(ricaricatoDalDb(1)),
                        "Il prodotto nel carrello non dovrebbe cambiare"));
    }
}
