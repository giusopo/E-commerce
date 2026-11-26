package control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.CartBean;
import Model.Bean.OrdineBean;
import Model.Bean.ProductBean;
import Model.Interface.CartModel_intf;
import Model.Interface.ProductModel_intf;
import Model.Interface.UserModel_intf;
import testutil.ServletMocks.RichiestaFinta;
import testutil.ServletMocks.RispostaFinta;
import testutil.ServletMocks.SessioneFinta;

/**
 * Test 72-77b di pianoTest.md: azioni del carrello e degli ordini (CartControl).
 *
 * Partizione per categorie: utente ospite (carrello solo in sessione) o loggato (carrello nel
 * database); prodotto nuovo o già nel carrello; quantità 1 o maggiore di 1, il valore limite che
 * decide se decrementare o togliere il prodotto; carrello vuoto o pieno per la conferma dell'ordine.
 * Le scelte di errore (azione mancante, id non numerico, database non raggiungibile) si provano una
 * volta per azione.
 *
 * I DAO sono mock che sostituiscono i campi statici della servlet; richiesta, risposta e sessione
 * sono oggetti finti. I prodotti letti dal catalogo sono oggetti diversi da quelli già nel carrello,
 * come nell'applicazione reale.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartControl - carrello e ordini")
class CartControlTest {

    private static final int ID_UTENTE = 42;
    private static final int ID_CARRELLO = 77;
    private static final int ID_NUOVO_CARRELLO = 78;
    private static final int ID_PRODOTTO = 5;
    private static final int ID_ALTRO_PRODOTTO = 6;
    private static final String INDIRIZZO = "Via Roma 1";
    private static final String PAGINA_CARRELLO = "/Cart?action=visualizza";

    @Mock private ProductModel_intf productModel;
    @Mock private CartModel_intf cartModel;
    @Mock private UserModel_intf userModel;
    @Mock private ServletConfig config;
    @Mock private ServletContext contesto;
    @Mock private RequestDispatcher dispatcher;

    private ProductModel_intf productModelOriginale;
    private CartModel_intf cartModelOriginale;
    private UserModel_intf userModelOriginale;
    private CartControl servlet;
    private final RispostaFinta risposta = new RispostaFinta();

    @BeforeEach
    void init() throws ServletException {
        productModelOriginale = CartControl.productModel;
        cartModelOriginale = CartControl.cartModel;
        userModelOriginale = CartControl.userModel;
        CartControl.productModel = productModel;
        CartControl.cartModel = cartModel;
        CartControl.userModel = userModel;

        lenient().when(config.getServletContext()).thenReturn(contesto);
        lenient().when(contesto.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        servlet = new CartControl();
        servlet.init(config);
    }

    @AfterEach
    void ripristinaModel() {
        CartControl.productModel = productModelOriginale;
        CartControl.cartModel = cartModelOriginale;
        CartControl.userModel = userModelOriginale;
    }

    private static RichiestaFinta azione(String azione) {
        return new RichiestaFinta("", "/Cart").parametro("action", azione);
    }

    private static RichiestaFinta azioneSulProdotto(String azione) {
        return azione(azione).parametro("id", String.valueOf(ID_PRODOTTO));
    }

    private static ProductBean prodotto(int id, int quantita) {
        ProductBean p = new ProductBean();
        p.setID(id);
        p.setNome("Rossetto " + id);
        p.setPrezzo(12.5);
        p.setQTA_ordinata(quantita);
        return p;
    }

    /** Prodotto letto dal catalogo con doRetrieveByKey: un oggetto nuovo, senza quantità ordinata. */
    private ProductBean prodottoNelCatalogo() throws SQLException {
        ProductBean p = prodotto(ID_PRODOTTO, 0);
        when(productModel.doRetrieveByKey(ID_PRODOTTO)).thenReturn(p);
        return p;
    }

    /** Carrello dell'ospite, salvato solo nella sessione. */
    private static CartBean carrelloOspite(RichiestaFinta richiesta, ProductBean... prodotti) {
        CartBean carrello = new CartBean();
        for (ProductBean p : prodotti) {
            carrello.addProduct(p);
        }
        richiesta.sessione().con("cart", carrello);
        return carrello;
    }

    /** Utente loggato: a ogni richiesta la servlet ricarica dal database i prodotti del suo carrello. */
    private void utenteLoggato(RichiestaFinta richiesta, ProductBean... prodottiNelDatabase) throws SQLException {
        richiesta.sessione()
                .con("username", "giulia01")
                .con("userID", ID_UTENTE)
                .con("cartID", ID_CARRELLO)
                .con("indirizzo", INDIRIZZO);
        when(cartModel.getProducts(ID_UTENTE)).thenReturn(new ArrayList<>(List.of(prodottiNelDatabase)));
    }

    private static CartBean carrelloInSessione(RichiestaFinta richiesta) {
        Object carrello = richiesta.sessioneCorrente().attributo("cart");
        // Guardia: senza un carrello in sessione i controlli successivi non hanno senso.
        return assertInstanceOf(CartBean.class, carrello, "La sessione deve contenere un carrello");
    }

    private void assertForward(RichiestaFinta richiesta, String pagina) throws Exception {
        verify(contesto, description("Pagina mostrata")).getRequestDispatcher(pagina);
        verify(dispatcher).forward(richiesta.get(), risposta.get());
    }

    // --- Utente ospite ---

    @Test
    @DisplayName("72 - visualizza senza carrello: carrello vuoto creato in sessione e mostrato")
    void visualizza_ospiteSenzaCarrello_carrelloVuotoCreatoEMostrato() throws Exception {
        RichiestaFinta richiesta = azione("visualizza");

        servlet.doGet(richiesta.get(), risposta.get());

        CartBean carrello = carrelloInSessione(richiesta);
        assertAll(
                () -> assertTrue(carrello.getProducts().isEmpty(), "Il nuovo carrello deve essere vuoto"),
                () -> verifyNoInteractions(cartModel, userModel, productModel),
                () -> assertForward(richiesta, "/Cart.jsp"));
    }

    @ParameterizedTest(name = "[{index}] azione \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"sconosciuta"})
    @DisplayName("72b - azione mancante, vuota o sconosciuta: nessuna eccezione e carrello mostrato")
    void azioneMancanteOSconosciuta_carrelloMostrato(String azione) throws Exception {
        RichiestaFinta richiesta = azione(azione);

        servlet.doGet(richiesta.get(), risposta.get());

        assertForward(richiesta, "/Cart.jsp");
    }

    @Test
    @DisplayName("73 - aggiungi un prodotto nuovo (ospite): nel carrello con quantità 1")
    void aggiungi_ospiteProdottoNuovo_aggiuntoConQuantitaUno() throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto("aggiungi");
        CartBean carrello = carrelloOspite(richiesta);
        prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        // Guardia: il prodotto deve essere nel carrello prima di controllarne la quantità.
        assertEquals(1, carrello.getProducts().size(), "Il carrello deve contenere il prodotto aggiunto");
        assertAll(
                () -> assertEquals(ID_PRODOTTO, carrello.getProducts().get(0).getID(), "Prodotto aggiunto"),
                () -> assertEquals(1, carrello.getProducts().get(0).getQTA_ordinata(), "Quantità iniziale"),
                () -> verifyNoInteractions(cartModel),
                () -> verify(risposta.get(), description("Si torna alla home")).sendRedirect("/"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource({"aggiungi, /", "aumentaQTA, " + PAGINA_CARRELLO})
    @DisplayName("73b - aggiungi o aumenta un prodotto già nel carrello (ospite): quantità +1, nessun duplicato")
    void aggiungiEAumenta_ospiteProdottoPresente_quantitaIncrementata(String azione, String redirect) throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto(azione);
        CartBean carrello = carrelloOspite(richiesta, prodotto(ID_PRODOTTO, 2));
        prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        // Guardia: lo stesso prodotto non deve comparire due volte.
        assertEquals(1, carrello.getProducts().size(), "Il prodotto non deve essere duplicato");
        assertAll(
                () -> assertEquals(3, carrello.getProducts().get(0).getQTA_ordinata(), "Quantità dopo l'aggiunta"),
                () -> verifyNoInteractions(cartModel),
                () -> verify(risposta.get()).sendRedirect(redirect));
    }

    @ParameterizedTest(name = "[{index}] quantità {0} → {1}")
    @CsvSource({"2, 1", "1, -1"})
    @DisplayName("73c - decrementa (ospite, valore limite 1): quantità -1, oppure prodotto tolto (-1)")
    void decrementaQTA_ospite_quantitaDiminuitaOProdottoTolto(int quantitaIniziale, int quantitaAttesa) throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto("decrementaQTA");
        CartBean carrello = carrelloOspite(richiesta, prodotto(ID_PRODOTTO, quantitaIniziale));
        ProductBean dalCatalogo = prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                // getQuantitàOrdinata restituisce -1 se il prodotto non è più nel carrello.
                () -> assertEquals(quantitaAttesa, carrello.getQuantitàOrdinata(dalCatalogo), "Quantità nel carrello"),
                () -> verifyNoInteractions(cartModel),
                () -> verify(risposta.get()).sendRedirect(PAGINA_CARRELLO));
    }

    @Test
    @DisplayName("73d - elimina (ospite): tolto solo quel prodotto, gli altri restano")
    void elimina_ospite_soloQuelProdottoTolto() throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto("elimina");
        CartBean carrello = carrelloOspite(richiesta, prodotto(ID_PRODOTTO, 3), prodotto(ID_ALTRO_PRODOTTO, 1));
        prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        // Guardia: deve restare esattamente un prodotto.
        assertEquals(1, carrello.getProducts().size(), "Deve restare solo l'altro prodotto");
        assertAll(
                () -> assertEquals(ID_ALTRO_PRODOTTO, carrello.getProducts().get(0).getID(), "Prodotto rimasto"),
                () -> verifyNoInteractions(cartModel),
                () -> assertForward(richiesta, PAGINA_CARRELLO));
    }

    // --- Utente loggato ---

    @Test
    @DisplayName("74 - visualizza (utente loggato): carrello caricato dal database e mostrato")
    void visualizza_utenteLoggato_carrelloCaricatoDalDatabase() throws Exception {
        RichiestaFinta richiesta = azione("visualizza");
        utenteLoggato(richiesta, prodotto(ID_PRODOTTO, 2));

        servlet.doGet(richiesta.get(), risposta.get());

        CartBean carrello = carrelloInSessione(richiesta);
        assertAll(
                () -> assertEquals(1, carrello.getProducts().size(), "Prodotti letti dal database"),
                () -> assertEquals(ID_PRODOTTO, carrello.getProducts().get(0).getID(), "Prodotto letto dal database"),
                () -> assertForward(richiesta, "/Cart.jsp"));
    }

    @ParameterizedTest(name = "[{index}] già nel carrello: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("74b - aggiungi (utente loggato): prodotto inserito o incrementato nel carrello del database")
    void aggiungi_utenteLoggato_inseritoOIncrementatoNelDatabase(boolean giaPresente) throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto("aggiungi");
        if (giaPresente) {
            utenteLoggato(richiesta, prodotto(ID_PRODOTTO, 1));
        } else {
            utenteLoggato(richiesta);
        }
        ProductBean dalCatalogo = prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(cartModel, description("true incrementa la quantità, false inserisce il prodotto"))
                        .addProduct(ID_CARRELLO, dalCatalogo, giaPresente, 1),
                () -> verify(risposta.get()).sendRedirect("/"));
    }

    @Test
    @DisplayName("74c - aumenta (utente loggato): quantità incrementata nel database")
    void aumentaQTA_utenteLoggato_quantitaIncrementataNelDatabase() throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto("aumentaQTA");
        utenteLoggato(richiesta, prodotto(ID_PRODOTTO, 1));
        ProductBean dalCatalogo = prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(cartModel, description("Il prodotto è già nel carrello: va incrementato"))
                        .addProduct(ID_CARRELLO, dalCatalogo, true, 1),
                () -> verify(risposta.get()).sendRedirect(PAGINA_CARRELLO));
    }

    @ParameterizedTest(name = "[{index}] quantità {0}, prodotto tolto: {1}")
    @CsvSource({"2, false", "1, true"})
    @DisplayName("74d - decrementa (utente loggato, valore limite 1): quantità -1 o prodotto tolto nel database")
    void decrementaQTA_utenteLoggato_quantitaDiminuitaOProdottoTolto(int quantita, boolean tolto) throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto("decrementaQTA");
        utenteLoggato(richiesta, prodotto(ID_PRODOTTO, quantita));
        ProductBean dalCatalogo = prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(cartModel, description("true toglie il prodotto, false decrementa la quantità"))
                        .deleteProduct(ID_CARRELLO, dalCatalogo, tolto),
                () -> verify(risposta.get()).sendRedirect(PAGINA_CARRELLO));
    }

    @Test
    @DisplayName("74e - elimina (utente loggato): prodotto tolto dal carrello del database")
    void elimina_utenteLoggato_prodottoToltoDalDatabase() throws Exception {
        RichiestaFinta richiesta = azioneSulProdotto("elimina");
        utenteLoggato(richiesta, prodotto(ID_PRODOTTO, 3));
        ProductBean dalCatalogo = prodottoNelCatalogo();

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(cartModel, description("Il prodotto va tolto qualunque sia la quantità"))
                        .deleteProduct(ID_CARRELLO, dalCatalogo, true),
                () -> assertForward(richiesta, PAGINA_CARRELLO));
    }

    static Stream<Arguments> idNonValidi() {
        return Stream.of("aggiungi", "aumentaQTA", "decrementaQTA", "elimina", "getOrdineStorico")
                .flatMap(azione -> Stream.of(Arguments.of(azione, "abc"), Arguments.of(azione, null)));
    }

    @ParameterizedTest(name = "[{index}] {0}, id {1}")
    @MethodSource("idNonValidi")
    @DisplayName("75 - id non numerico o mancante: risposta 400 e carrello non modificato")
    void azioniConId_idNonValido_rispostaBadRequest(String azione, String id) throws Exception {
        RichiestaFinta richiesta = azione(azione).parametro("id", id);
        utenteLoggato(richiesta, prodotto(ID_PRODOTTO, 2));

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(risposta.get(), description("Una richiesta malformata va respinta con 400"))
                        .sendError(HttpServletResponse.SC_BAD_REQUEST),
                () -> verify(cartModel, never().description("Il carrello non va modificato"))
                        .addProduct(anyInt(), any(), anyBoolean(), anyInt()),
                () -> verify(cartModel, never().description("Il carrello non va modificato"))
                        .deleteProduct(anyInt(), any(), anyBoolean()),
                () -> verifyNoInteractions(productModel, userModel));
    }

    // --- Ordini ---

    @Test
    @DisplayName("76 - conferma ordine: ordine confermato, nuovo carrello vuoto e storico dell'ordine")
    void confermaOrdine_utenteLoggato_ordineConfermatoENuovoCarrello() throws Exception {
        RichiestaFinta richiesta = azione("confermaOrdine");
        ProductBean nelCarrello = prodotto(ID_PRODOTTO, 2);
        utenteLoggato(richiesta, nelCarrello);
        when(userModel.getIdCartByUserId(ID_UTENTE)).thenReturn(ID_NUOVO_CARRELLO);

        servlet.doGet(richiesta.get(), risposta.get());

        SessioneFinta sessione = richiesta.sessioneCorrente();
        assertAll(
                () -> verify(cartModel, description("Ordine confermato con indirizzo e prodotti del carrello"))
                        .confermaOrdine(INDIRIZZO, ID_CARRELLO, ID_UTENTE, List.of(nelCarrello)),
                () -> assertTrue(carrelloInSessione(richiesta).getProducts().isEmpty(),
                        "Dopo la conferma il carrello in sessione deve essere vuoto"),
                () -> assertEquals(ID_NUOVO_CARRELLO, sessione.attributo("cartID"), "Id del nuovo carrello"),
                () -> verify(risposta.get(), description("Si mostra l'ordine appena confermato"))
                        .sendRedirect("/Cart?action=getOrdineStorico&id=" + ID_CARRELLO));
    }

    @Test
    @DisplayName("76b - conferma ordine con carrello vuoto: nessun ordine confermato")
    void confermaOrdine_carrelloVuoto_ordineNonConfermato() throws Exception {
        RichiestaFinta richiesta = azione("confermaOrdine");
        utenteLoggato(richiesta);

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(cartModel, never().description("Un carrello vuoto non diventa un ordine"))
                        .confermaOrdine(anyString(), anyInt(), anyInt(), any()),
                () -> verifyNoInteractions(userModel),
                () -> verify(risposta.get(), description("Si torna al carrello")).sendRedirect(PAGINA_CARRELLO));
    }

    @Test
    @DisplayName("76c - conferma ordine con errore del database: carrello invariato e pagina di errore")
    void confermaOrdine_erroreDatabase_carrelloInvariatoEPaginaErrore() throws Exception {
        RichiestaFinta richiesta = azione("confermaOrdine");
        utenteLoggato(richiesta, prodotto(ID_PRODOTTO, 2));
        doThrow(new SQLException("database non raggiungibile"))
                .when(cartModel).confermaOrdine(anyString(), anyInt(), anyInt(), any());

        servlet.doGet(richiesta.get(), risposta.get());

        SessioneFinta sessione = richiesta.sessioneCorrente();
        assertAll(
                () -> assertEquals(1, carrelloInSessione(richiesta).getProducts().size(),
                        "Il carrello non confermato deve restare com'era"),
                () -> assertEquals(ID_CARRELLO, sessione.attributo("cartID"), "Il carrello resta lo stesso"),
                () -> verify(risposta.get(), never().description("L'ordine non va mostrato come confermato"))
                        .sendRedirect("/Cart?action=getOrdineStorico&id=" + ID_CARRELLO),
                () -> verify(risposta.get(), description("L'utente deve sapere che c'è stato un errore"))
                        .sendRedirect("/Error.jsp"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"confermaOrdine", "getOrdineStorico", "StoricoCompletoOrdini"})
    @DisplayName("76d - azioni sugli ordini senza login: nessun accesso al database e redirect al login")
    void azioniOrdini_utenteNonLoggato_redirectAlLogin(String azione) throws Exception {
        RichiestaFinta richiesta = azione(azione).parametro("id", String.valueOf(ID_CARRELLO));
        carrelloOspite(richiesta, prodotto(ID_PRODOTTO, 1));

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verifyNoInteractions(cartModel, userModel),
                () -> verify(risposta.get(), description("Chi non è loggato va al login"))
                        .sendRedirect("/LoginRegistrazione.jsp"));
    }

    @Test
    @DisplayName("77 - storico di un ordine: letto con l'id dell'utente loggato e mostrato")
    void getOrdineStorico_utenteLoggato_ordineDellUtenteMostrato() throws Exception {
        RichiestaFinta richiesta = azione("getOrdineStorico").parametro("id", String.valueOf(ID_CARRELLO));
        utenteLoggato(richiesta);
        OrdineBean ordine = new OrdineBean();
        // La query filtra per utente: passare l'id della sessione impedisce di leggere ordini altrui.
        when(userModel.getOrdineById(ID_UTENTE, ID_CARRELLO)).thenReturn(ordine);

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(richiesta.get(), description("Ordine passato alla pagina")).setAttribute("ordine", ordine),
                () -> assertForward(richiesta, "/ordineStorico.jsp"));
    }

    @Test
    @DisplayName("77b - storico completo: ordini dell'utente loggato mostrati")
    void storicoCompletoOrdini_utenteLoggato_ordiniDellUtenteMostrati() throws Exception {
        RichiestaFinta richiesta = azione("StoricoCompletoOrdini");
        utenteLoggato(richiesta);
        ArrayList<Integer> ordini = new ArrayList<>(List.of(70, 74));
        when(userModel.getAllOrdine(ID_UTENTE)).thenReturn(ordini);

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(richiesta.get(), description("Ordini passati alla pagina")).setAttribute("ordini", ordini),
                () -> assertForward(richiesta, "/StoricoOrdini.jsp"));
    }
}
