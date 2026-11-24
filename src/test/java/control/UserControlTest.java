package control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.CartModel;
import Model.UserModel;
import Model.Bean.CartBean;
import Model.Bean.ProductBean;
import Model.Bean.UserBean;
import testutil.ServletMocks.RichiestaFinta;
import testutil.ServletMocks.RispostaFinta;
import testutil.ServletMocks.SessioneFinta;

/**
 * Test 39-58 di pianoTest.md: registrazione, login, dati utente e logout.
 *
 * I DAO sono mock che sostituiscono i campi statici della servlet; richiesta,
 * risposta e sessione sono oggetti finti. I validatori privati vengono verificati
 * attraverso le richieste, come farebbe un client che invia il form senza JavaScript.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserControl - registrazione, login e sessione")
class UserControlTest {

    private static final String USERNAME = "giulia01";
    private static final String PASSWORD = "Password1!";
    /** SHA-256 di "Password1!", calcolato in modo indipendente dal codice testato. */
    private static final String HASH_PASSWORD = "1d707811988069ca760826861d6d63a10e8c3b7f171c4441a6472ea58c11711b";
    private static final int ID_UTENTE = 42;
    private static final int ID_CARRELLO = 77;

    @Mock private UserModel userModel;
    @Mock private CartModel cartModel;
    @Mock private ServletConfig config;
    @Mock private ServletContext contesto;
    @Mock private RequestDispatcher dispatcher;

    private UserModel userModelOriginale;
    private CartModel cartModelOriginale;
    private UserControl servlet;
    private final RispostaFinta risposta = new RispostaFinta();

    @BeforeEach
    void init() throws ServletException {
        userModelOriginale = UserControl.model;
        cartModelOriginale = UserControl.cartModel;
        UserControl.model = userModel;
        UserControl.cartModel = cartModel;

        lenient().when(config.getServletContext()).thenReturn(contesto);
        lenient().when(contesto.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        servlet = new UserControl();
        servlet.init(config);
    }

    @AfterEach
    void ripristinaModel() {
        UserControl.model = userModelOriginale;
        UserControl.cartModel = cartModelOriginale;
    }

    /** Form di registrazione con tutti i dati validi. */
    private static RichiestaFinta registrazione() {
        return new RichiestaFinta("", "/User")
                .parametro("action", "register")
                .parametro("nome", "Giulia")
                .parametro("cognome", "Bianchi")
                .parametro("new_username", USERNAME)
                .parametro("phoneNumber", "3312345678")
                .parametro("dataNascita", "2000-05-10")
                .parametro("email", "giulia@example.com")
                .parametro("indirizzo", "Via Roma 1")
                .parametro("new_password", PASSWORD);
    }

    private static RichiestaFinta login(String username, String password) {
        return new RichiestaFinta("", "/User")
                .parametro("action", "login")
                .parametro("username", username)
                .parametro("password", password);
    }

    /** Utente come lo restituirebbe il DB dopo un login corretto. */
    private static UserBean utente(boolean admin) {
        UserBean u = new UserBean();
        u.setID(ID_UTENTE);
        u.setUserName(USERNAME);
        u.setPassword(HASH_PASSWORD);
        u.setCartID(ID_CARRELLO);
        u.setIndirizzoBase("Via Roma 1");
        u.setPermessiAdmin(admin);
        return u;
    }

    private static ProductBean prodotto(int id, int quantita) {
        ProductBean p = new ProductBean();
        p.setID(id);
        p.setNome("Prodotto " + id);
        p.setQTA_ordinata(quantita);
        return p;
    }

    private void assertRegistrazioneRifiutata(String motivo) throws SQLException {
        assertEquals("0", risposta.val(), "La registrazione con " + motivo + " dovrebbe essere rifiutata");
        verify(userModel, never()).AddUser(any());
    }

    // ---------------------------------------------------------------- registrazione

    @Test
    @DisplayName("39 - registrazione con dati validi: utente salvato con la password cifrata")
    void register_datiValidi_utenteSalvatoConPasswordCifrata() throws Exception {
        servlet.doPost(registrazione().get(), risposta.get());

        ArgumentCaptor<UserBean> salvato = ArgumentCaptor.forClass(UserBean.class);
        verify(userModel).AddUser(salvato.capture());
        assertAll(
                () -> assertEquals(HASH_PASSWORD, salvato.getValue().getPassword(),
                        "La password dovrebbe essere salvata come hash SHA-256"),
                () -> assertNotEquals(PASSWORD, salvato.getValue().getPassword(),
                        "La password non deve essere salvata in chiaro"),
                () -> assertEquals(USERNAME, salvato.getValue().getUserName(), "username"),
                () -> assertEquals("1", risposta.val(), "La registrazione dovrebbe riuscire"));
    }

    @Test
    @DisplayName("40 - registrazione con carrello ospite: prodotti copiati nel nuovo carrello")
    void register_carrelloOspitePieno_prodottiCopiatiNelNuovoCarrello() throws Exception {
        ProductBean rossetto = prodotto(1, 2);
        ProductBean mascara = prodotto(2, 1);
        CartBean carrelloOspite = new CartBean();
        carrelloOspite.addProduct(rossetto);
        carrelloOspite.addProduct(mascara);
        RichiestaFinta richiesta = registrazione();
        richiesta.sessione().con("cart", carrelloOspite);
        when(userModel.getIdCart(USERNAME, HASH_PASSWORD)).thenReturn(ID_CARRELLO);

        servlet.doPost(richiesta.get(), risposta.get());

        verify(cartModel).addProduct(ID_CARRELLO, rossetto, false, 2);
        verify(cartModel).addProduct(ID_CARRELLO, mascara, false, 1);
        assertEquals("1", risposta.val(), "La registrazione dovrebbe riuscire");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" ({1})")
    @CsvSource({
            "Pa1!abc, 7 caratteri",
            "Password1!Password1!x, 21 caratteri",
            "password1!, nessuna maiuscola",
            "PASSWORD1!, nessuna minuscola",
            "Password!!, nessuna cifra",
            "Password12, nessun carattere speciale"
    })
    @DisplayName("41 - registrazione con password debole: rifiutata")
    void register_passwordDebole_registrazioneRifiutata(String password, String motivo) throws Exception {
        servlet.doPost(registrazione().parametro("new_password", password).get(), risposta.get());

        assertRegistrazioneRifiutata("una password con " + motivo);
    }

    @Test
    @DisplayName("42 - registrazione con password vuota: rifiutata")
    void register_passwordVuota_registrazioneRifiutata() throws Exception {
        servlet.doPost(registrazione().parametro("new_password", "").get(), risposta.get());

        assertRegistrazioneRifiutata("una password vuota");
    }

    @Test
    @DisplayName("43 - registrazione con email non valida: rifiutata")
    void register_emailNonValida_registrazioneRifiutata() throws Exception {
        servlet.doPost(registrazione().parametro("email", "non-una-email").get(), risposta.get());

        assertRegistrazioneRifiutata("un'email non valida");
    }

    @Test
    @DisplayName("44 - registrazione con telefono non valido: rifiutata")
    void register_telefonoNonValido_registrazioneRifiutata() throws Exception {
        servlet.doPost(registrazione().parametro("phoneNumber", "abc").get(), risposta.get());

        assertRegistrazioneRifiutata("un telefono non valido");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"2999-01-01", "1900-01-01"})
    @DisplayName("45 - registrazione con data di nascita futura o di oltre 100 anni fa: rifiutata")
    void register_dataNascitaFuturaOOltreCentoAnni_registrazioneRifiutata(String data) throws Exception {
        servlet.doPost(registrazione().parametro("dataNascita", data).get(), risposta.get());

        assertRegistrazioneRifiutata("la data di nascita " + data);
    }

    @Test
    @DisplayName("46 - registrazione con data in formato sbagliato: val 0 senza eccezioni")
    void register_dataNascitaNonValida_rispostaDiErrore() throws Exception {
        RichiestaFinta richiesta = registrazione().parametro("dataNascita", "10/05/2000");

        assertDoesNotThrow(() -> servlet.doPost(richiesta.get(), risposta.get()),
                "Una data in formato sbagliato non deve far fallire la richiesta con un'eccezione");
        assertRegistrazioneRifiutata("una data in formato sbagliato");
    }

    @Test
    @DisplayName("47 - registrazione con il nome vuoto: rifiutata")
    void register_campoObbligatorioVuoto_registrazioneRifiutata() throws Exception {
        servlet.doPost(registrazione().parametro("nome", "").get(), risposta.get());

        assertRegistrazioneRifiutata("il nome vuoto");
    }

    @Test
    @DisplayName("48 - registrazione con errore del database: val 0")
    void register_erroreDatabase_rispostaDiErrore() throws Exception {
        doThrow(new SQLException("username già esistente")).when(userModel).AddUser(any(UserBean.class));

        servlet.doPost(registrazione().get(), risposta.get());

        assertEquals("0", risposta.val(), "Se l'utente non viene salvato, la registrazione non deve risultare riuscita");
    }

    // ---------------------------------------------------------------- login

    @ParameterizedTest(name = "[{index}] admin = {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("49 - login con credenziali corrette: sessione impostata")
    void login_credenzialiCorrette_sessioneImpostata(boolean admin) throws Exception {
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD)).thenReturn(utente(admin));
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);

        servlet.doPost(richiesta.get(), risposta.get());

        SessioneFinta sessione = richiesta.sessioneCorrente();
        assertAll(
                () -> assertEquals("1", risposta.val(), "Il login dovrebbe riuscire"),
                () -> assertEquals(USERNAME, sessione.attributo("username"), "username"),
                () -> assertEquals(ID_UTENTE, sessione.attributo("userID"), "userID"),
                () -> assertEquals(ID_CARRELLO, sessione.attributo("cartID"), "cartID"),
                () -> assertEquals("Via Roma 1", sessione.attributo("indirizzo"), "indirizzo"),
                () -> assertEquals(admin, sessione.attributo("adminRoles"), "ruolo admin"));
    }

    @Test
    @DisplayName("50 - login con password errata: accesso negato")
    void login_passwordErrata_accessoNegato() throws Exception {
        // Credenziali errate: il DAO restituisce un utente vuoto.
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD)).thenReturn(new UserBean());
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);

        servlet.doPost(richiesta.get(), risposta.get());

        assertAll(
                () -> assertEquals("0", risposta.val(), "Il login dovrebbe essere negato"),
                () -> assertNull(richiesta.sessioneCorrente().attributo("userID"),
                        "La sessione non dovrebbe contenere un utente"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvSource({
            "giulia!, Password1!, username non alfanumerico",
            "giulia01, password, password non conforme"
    })
    @DisplayName("51 - login con formato non valido: DB non interrogato, ritorno al login")
    void login_formatoNonValido_nessunAccessoAlDatabase(String username, String password, String motivo)
            throws Exception {
        RichiestaFinta richiesta = login(username, password);

        servlet.doPost(richiesta.get(), risposta.get());

        verify(userModel, never()).getUserTramiteCredenziali(anyString(), anyString());
        verify(contesto).getRequestDispatcher("/LoginRegistrazione.jsp");
        verify(dispatcher).forward(richiesta.get(), risposta.get());
    }

    @Test
    @DisplayName("52 - login senza username e password: nessuna eccezione")
    void login_parametriMancanti_nessunaEccezione() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "login");

        assertDoesNotThrow(() -> servlet.doPost(richiesta.get(), risposta.get()),
                "Una richiesta di login senza parametri non deve far fallire la servlet con un'eccezione");
        verify(userModel, never()).getUserTramiteCredenziali(any(), any());
    }

    @Test
    @DisplayName("53 - login con un utente già loggato: dati nella nuova sessione")
    void login_sessioneGiaAttiva_datiNellaNuovaSessione() throws Exception {
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD)).thenReturn(utente(false));
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);
        SessioneFinta vecchia = richiesta.sessione().con("userID", 5).con("username", "altroUtente");

        servlet.doPost(richiesta.get(), risposta.get());

        SessioneFinta nuova = richiesta.sessioneCorrente();
        assertAll(
                () -> assertTrue(vecchia.isInvalidata(), "La sessione del vecchio utente dovrebbe essere invalidata"),
                () -> assertNotSame(vecchia, nuova, "Dovrebbe essere stata creata una nuova sessione"),
                () -> assertEquals(USERNAME, nuova.attributo("username"),
                        "I dati del nuovo login dovrebbero essere nella nuova sessione"),
                () -> assertEquals("1", risposta.val(), "Il login dovrebbe riuscire"));
    }

    @Test
    @DisplayName("54 - login con errore del database: redirect alla pagina di errore")
    void login_erroreDatabase_redirectPaginaErrore() throws Exception {
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD))
                .thenThrow(new SQLException("database non raggiungibile"));
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);

        servlet.doPost(richiesta.get(), risposta.get());

        verify(risposta.get()).sendRedirect("Error.jsp");
        assertEquals("database non raggiungibile", richiesta.sessioneCorrente().attributo("errorMessage"),
                "Il messaggio d'errore dovrebbe essere salvato in sessione");
    }

    // ---------------------------------------------------------------- dati utente, logout, altre azioni

    @Test
    @DisplayName("55 - dati utente con utente loggato: forward ad AreaUtente.jsp")
    void datiUtente_utenteLoggato_forwardAreaUtente() throws Exception {
        UserBean utente = utente(false);
        when(userModel.getUserTramiteID(ID_UTENTE)).thenReturn(utente);
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "DatiUtente");
        richiesta.sessione().con("userID", ID_UTENTE);

        servlet.doPost(richiesta.get(), risposta.get());

        verify(richiesta.get()).setAttribute("datiUtente", utente);
        verify(contesto).getRequestDispatcher("/AreaUtente.jsp");
        verify(dispatcher).forward(richiesta.get(), risposta.get());
    }

    @Test
    @DisplayName("56 - dati utente senza login: nessuna eccezione")
    void datiUtente_utenteNonLoggato_nessunaEccezione() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "DatiUtente");

        assertDoesNotThrow(() -> servlet.doPost(richiesta.get(), risposta.get()),
                "Senza login la richiesta non deve far fallire la servlet con un'eccezione");
        verify(userModel, never()).getUserTramiteID(anyInt());
    }

    @Test
    @DisplayName("57 - logout via GET: sessione invalidata e ritorno al login")
    void logout_sessioneAttiva_sessioneInvalidataEForwardLogin() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "logout");
        SessioneFinta sessione = richiesta.sessione().con("userID", ID_UTENTE);

        servlet.doGet(richiesta.get(), risposta.get());

        assertTrue(sessione.isInvalidata(), "La sessione dovrebbe essere invalidata");
        verify(contesto).getRequestDispatcher("/LoginRegistrazione.jsp");
        verify(dispatcher).forward(richiesta.get(), risposta.get());
    }

    @Test
    @DisplayName("58 - azione sconosciuta: forward alla home")
    void azioneSconosciuta_forwardHome() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "sconosciuta");

        servlet.doPost(richiesta.get(), risposta.get());

        verify(contesto).getRequestDispatcher("/Home.jsp");
        verify(dispatcher).forward(richiesta.get(), risposta.get());
    }
}
