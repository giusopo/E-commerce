package control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.stream.Stream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.RecensioneBean;
import Model.Interface.ProductModel_intf;
import Model.Interface.RecensioneModel_intf;
import testutil.ServletMocks.RichiestaFinta;
import testutil.ServletMocks.RispostaFinta;

/**
 * Test 70-71b di pianoTest.md: inserimento e modifica delle recensioni (doPost di ProductControl).
 *
 * La risposta JSON {"val":"1"} dice al client che la recensione è stata salvata, {"val":"0"} che non lo
 * è stata. Classi di equivalenza del punteggio: valido (valori limite 1 e 5), sotto l'intervallo (0),
 * sopra (6), non numerico. Le scelte di errore (utente non loggato, database non raggiungibile) si
 * provano una volta per ognuna delle due azioni.
 *
 * Una recensione può essere modificata solo da chi l'ha scritta: l'id inviato dal client non basta.
 *
 * I DAO sono mock che sostituiscono i campi statici della servlet; richiesta, risposta e sessione
 * sono oggetti finti.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductControl - recensioni dei prodotti")
class ProductControlTest {

    private static final int ID_UTENTE = 42;
    private static final int ID_PRODOTTO = 12;
    private static final int ID_RECENSIONE_PROPRIA = 7;
    /** Recensione scritta da un altro utente sullo stesso prodotto. */
    private static final int ID_RECENSIONE_ALTRUI = 99;
    private static final String COMMENTO = "Ottima tenuta anche dopo otto ore";

    @Mock private ProductModel_intf productModel;
    @Mock private RecensioneModel_intf recensioni;
    @Mock private ServletConfig config;
    @Mock private ServletContext contesto;
    @Mock private RequestDispatcher dispatcher;

    private ProductModel_intf productModelOriginale;
    private RecensioneModel_intf recensioniOriginale;
    private ProductControl servlet;
    private final RispostaFinta risposta = new RispostaFinta();

    @BeforeEach
    void init() throws ServletException {
        productModelOriginale = ProductControl.model;
        recensioniOriginale = ProductControl.modelRecensione;
        ProductControl.model = productModel;
        ProductControl.modelRecensione = recensioni;

        lenient().when(config.getServletContext()).thenReturn(contesto);
        lenient().when(contesto.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        servlet = new ProductControl();
        servlet.init(config);
    }

    @AfterEach
    void ripristinaModel() {
        ProductControl.model = productModelOriginale;
        ProductControl.modelRecensione = recensioniOriginale;
    }

    /**
     * Form della recensione come lo invia recensioniProdotto.js: per una recensione nuova
     * recensioneID vale -1, per una modifica è l'id della recensione mostrata nella pagina.
     */
    private static RichiestaFinta recensione(String azione, String punteggio, int recensioneID) {
        return new RichiestaFinta("", "/")
                .parametro("action", azione)
                .parametro("productID", String.valueOf(ID_PRODOTTO))
                .parametro("punteggio", punteggio)
                .parametro("commento", COMMENTO)
                .parametro("recensioneID", String.valueOf(recensioneID));
    }

    private static RichiestaFinta recensione(String azione, String punteggio) {
        int recensioneID = azione.equals("modificaRecensione") ? ID_RECENSIONE_PROPRIA : -1;
        return recensione(azione, punteggio, recensioneID);
    }

    private static RichiestaFinta daUtenteLoggato(RichiestaFinta richiesta) {
        richiesta.sessione().con("userID", ID_UTENTE);
        return richiesta;
    }

    /** Recensione salvata nel database, come la restituisce getRecensione. */
    private static RecensioneBean recensioneSalvata(int id, int autore) {
        RecensioneBean r = new RecensioneBean();
        r.setID(id);
        r.setUserID(autore);
        r.setPRODOTTO_ID(ID_PRODOTTO);
        return r;
    }

    /** La recensione che l'utente loggato ha già scritto sul prodotto. */
    private void utenteHaRecensitoIlProdotto() throws SQLException {
        lenient().when(recensioni.getRecensione(ID_UTENTE, ID_PRODOTTO))
                .thenReturn(recensioneSalvata(ID_RECENSIONE_PROPRIA, ID_UTENTE));
    }

    private void assertNienteSalvato() {
        assertAll(
                () -> verify(recensioni, never().description("Nessuna recensione va inserita"))
                        .addRecensione(anyInt(), anyInt(), anyInt(), any()),
                () -> verify(recensioni, never().description("Nessuna recensione va modificata"))
                        .modificaRecensione(anyInt(), anyInt(), any()),
                () -> assertEquals("0", risposta.val(), "Se la recensione non è salvata la risposta deve essere val 0"));
    }

    @ParameterizedTest(name = "[{index}] punteggio {0}")
    @ValueSource(ints = {1, 5})
    @DisplayName("70 - recensione con punteggio valido (valori limite 1 e 5): salvata e risposta val 1")
    void recensione_punteggioValido_salvataERispostaPositiva(int punteggio) throws Exception {
        servlet.doPost(daUtenteLoggato(recensione("recensione", String.valueOf(punteggio))).get(), risposta.get());

        assertAll(
                () -> verify(recensioni, description("La recensione va salvata a nome dell'utente loggato"))
                        .addRecensione(ID_UTENTE, ID_PRODOTTO, punteggio, COMMENTO),
                () -> assertEquals("1", risposta.val(), "Una recensione salvata deve dare val 1"));
    }

    static Stream<Arguments> punteggiNonValidi() {
        return Stream.of("recensione", "modificaRecensione").flatMap(azione -> Stream.of(
                Arguments.of(azione, Named.of("0 (sotto l'intervallo)", "0")),
                Arguments.of(azione, Named.of("6 (sopra l'intervallo)", "6")),
                Arguments.of(azione, Named.of("non numerico", "abc"))));
    }

    @ParameterizedTest(name = "[{index}] {0}, punteggio {1}")
    @MethodSource("punteggiNonValidi")
    @DisplayName("70b - punteggio non valido: recensione né inserita né modificata e risposta val 0")
    void azioniRecensione_punteggioNonValido_nienteSalvatoERispostaNegativa(String azione, String punteggio)
            throws Exception {
        // Per la modifica l'utente ha già una recensione sul prodotto: l'unico dato sbagliato è il punteggio.
        utenteHaRecensitoIlProdotto();

        servlet.doPost(daUtenteLoggato(recensione(azione, punteggio)).get(), risposta.get());

        assertNienteSalvato();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"recensione", "modificaRecensione"})
    @DisplayName("70c - errore del database: risposta val 0 e non val 1")
    void azioniRecensione_erroreDatabase_rispostaNegativa(String azione) throws Exception {
        utenteHaRecensitoIlProdotto();
        SQLException errore = new SQLException("database non raggiungibile");
        lenient().doThrow(errore).when(recensioni).addRecensione(anyInt(), anyInt(), anyInt(), any());
        lenient().doThrow(errore).when(recensioni).modificaRecensione(anyInt(), anyInt(), any());

        servlet.doPost(daUtenteLoggato(recensione(azione, "4")).get(), risposta.get());

        assertEquals("0", risposta.val(), "Se il database fallisce il client non deve ricevere val 1");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"recensione", "modificaRecensione"})
    @DisplayName("70d - utente non loggato: nessun accesso al database e risposta val 0")
    void azioniRecensione_utenteNonLoggato_nienteSalvatoERispostaNegativa(String azione) throws Exception {
        servlet.doPost(recensione(azione, "4").get(), risposta.get());

        assertAll(
                () -> verifyNoInteractions(recensioni),
                () -> assertEquals("0", risposta.val(), "Senza login la recensione non può essere salvata"));
    }

    @Test
    @DisplayName("71 - modifica della propria recensione: modificata e risposta val 1")
    void modificaRecensione_recensioneDellUtente_modificataERispostaPositiva() throws Exception {
        utenteHaRecensitoIlProdotto();

        servlet.doPost(daUtenteLoggato(recensione("modificaRecensione", "3")).get(), risposta.get());

        assertAll(
                () -> verify(recensioni, description("La recensione dell'utente va modificata"))
                        .modificaRecensione(ID_RECENSIONE_PROPRIA, 3, COMMENTO),
                () -> assertEquals("1", risposta.val(), "Una modifica riuscita deve dare val 1"));
    }

    static Stream<Arguments> recensioniDellUtente() {
        return Stream.of(
                Arguments.of(Named.of("l'utente non ha recensito il prodotto", new RecensioneBean())),
                Arguments.of(Named.of("l'utente ha un'altra recensione sul prodotto",
                        recensioneSalvata(ID_RECENSIONE_PROPRIA, ID_UTENTE))));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("recensioniDellUtente")
    @DisplayName("71b - modifica con l'id della recensione di un altro utente: nessuna modifica e risposta val 0")
    void modificaRecensione_recensioneDiUnAltroUtente_nonModificataERispostaNegativa(RecensioneBean recensioneDellUtente)
            throws Exception {
        // Il client invia l'id della recensione di un altro utente: la richiesta va respinta.
        when(recensioni.getRecensione(ID_UTENTE, ID_PRODOTTO)).thenReturn(recensioneDellUtente);
        RichiestaFinta richiesta = daUtenteLoggato(recensione("modificaRecensione", "1", ID_RECENSIONE_ALTRUI));

        servlet.doPost(richiesta.get(), risposta.get());

        assertNienteSalvato();
    }
}
