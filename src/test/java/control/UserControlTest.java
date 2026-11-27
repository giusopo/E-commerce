package control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
 * Test 39-58 di pianoTest.md, con i sotto-numeri: registrazione, login, dati utente e logout.
 *
 * I DAO sono mock che sostituiscono i campi statici della servlet; richiesta,
 * risposta e sessione sono oggetti finti. I validatori privati vengono verificati
 * attraverso le richieste, come farebbe un client che invia il form senza JavaScript.
 *
 * Per la registrazione ogni caso contiene una sola classe non valida (un campo sbagliato, gli altri
 * validi). Per lunghezza della password, telefono e data di nascita si provano i valori limite:
 * l'ultimo valore accettato e il primo rifiutato.
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

    private void assertRegistrazioneRifiutata(String motivo) {
        assertAll(
                () -> assertEquals("0", risposta.val(), "La registrazione con " + motivo + " dovrebbe essere rifiutata"),
                () -> verify(userModel, never().description("Con " + motivo + " l'utente non deve essere salvato"))
                        .AddUser(any()));
    }

    private void assertRegistrazioneAccettata(String motivo) {
        assertAll(
                () -> assertEquals("1", risposta.val(), "La registrazione con " + motivo + " dovrebbe riuscire"),
                () -> verify(userModel, description("Con " + motivo + " l'utente deve essere salvato"))
                        .AddUser(any()));
    }

    // ---------------------------------------------------------------- registrazione

    @Test
    @DisplayName("39 - registrazione con dati validi: utente salvato con la password cifrata")
    void register_datiValidi_utenteSalvatoConPasswordCifrata() throws Exception {
        servlet.doPost(registrazione().get(), risposta.get());

        ArgumentCaptor<UserBean> salvato = ArgumentCaptor.forClass(UserBean.class);
        // Guardia: serve l'utente passato al DAO per controllarne i campi.
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

        assertAll(
                () -> verify(cartModel, description("Il rossetto va copiato con quantità 2"))
                        .addProduct(ID_CARRELLO, rossetto, false, 2),
                () -> verify(cartModel, description("Il mascara va copiato con quantità 1"))
                        .addProduct(ID_CARRELLO, mascara, false, 1),
                () -> assertEquals("1", risposta.val(), "La registrazione dovrebbe riuscire"));
    }

    static Stream<Arguments> carrelliNonCopiabili() {
        CartBean conUnProdotto = new CartBean();
        conUnProdotto.addProduct(prodotto(1, 2));
        return Stream.of(
                Arguments.of(Named.of("carrello ospite vuoto", new CartBean()), ID_CARRELLO),
                Arguments.of(Named.of("carrello del nuovo utente non trovato", conUnProdotto), -1));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("carrelliNonCopiabili")
    @DisplayName("40b - registrazione con carrello ospite vuoto o senza carrello del nuovo utente: nessuna copia")
    void register_carrelloNonCopiabile_nessunProdottoCopiato(CartBean carrelloOspite, int idCarrelloNuovoUtente)
            throws Exception {
        RichiestaFinta richiesta = registrazione();
        richiesta.sessione().con("cart", carrelloOspite);
        when(userModel.getIdCart(USERNAME, HASH_PASSWORD)).thenReturn(idCarrelloNuovoUtente);

        servlet.doPost(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(cartModel, never().description("Non ci sono prodotti da copiare in un carrello esistente"))
                        .addProduct(anyInt(), any(), anyBoolean(), anyInt()),
                () -> assertEquals("1", risposta.val(), "La registrazione dovrebbe comunque riuscire"));
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

    // Valori limite validi della lunghezza: il test 41 prova quelli appena fuori (7 e 21 caratteri).
    @ParameterizedTest(name = "[{index}] \"{0}\" ({1})")
    @CsvSource({
            "Pass1!ab, 8 caratteri",
            "Password1!Password1!, 20 caratteri"
    })
    @DisplayName("41b - registrazione con password di 8 o 20 caratteri (valori limite): accettata")
    void register_passwordAiLimitiDiLunghezza_registrazioneAccettata(String password, String lunghezza)
            throws Exception {
        servlet.doPost(registrazione().parametro("new_password", password).get(), risposta.get());

        assertRegistrazioneAccettata("una password di " + lunghezza);
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

    // Il formato valido ha 10 cifre (es. 3312345678): 9 e 11 cifre sono i valori limite appena fuori.
    @ParameterizedTest(name = "[{index}] \"{0}\" ({1})")
    @CsvSource({
            "abc, lettere al posto delle cifre",
            "331234567, 9 cifre",
            "33123456789, 11 cifre"
    })
    @DisplayName("44 - registrazione con telefono non valido (anche con 9 o 11 cifre): rifiutata")
    void register_telefonoNonValido_registrazioneRifiutata(String telefono, String motivo) throws Exception {
        servlet.doPost(registrazione().parametro("phoneNumber", telefono).get(), risposta.get());

        assertRegistrazioneRifiutata("un telefono con " + motivo);
    }

    // Le date limite sono calcolate rispetto a oggi: domani è la prima data futura, esattamente 100 anni fa
    // è la più recente tra quelle troppo vecchie. Le date valide accanto ai limiti sono nel test 45b.
    static Stream<Arguments> dateNonValideAlLimite() {
        LocalDate oggi = LocalDate.now();
        return Stream.of(
                Arguments.of(oggi.plusDays(1).toString(), "domani"),
                Arguments.of(oggi.minusYears(100).toString(), "esattamente 100 anni fa"));
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("dateNonValideAlLimite")
    @DisplayName("45 - registrazione con data di nascita di domani o di 100 anni fa (valori limite): rifiutata")
    void register_dataNascitaFuturaOOltreCentoAnni_registrazioneRifiutata(String data, String quando)
            throws Exception {
        servlet.doPost(registrazione().parametro("dataNascita", data).get(), risposta.get());

        assertRegistrazioneRifiutata("la data di nascita di " + quando + " (" + data + ")");
    }

    static Stream<Arguments> dateValideAlLimite() {
        LocalDate oggi = LocalDate.now();
        return Stream.of(
                Arguments.of(oggi.toString(), "oggi"),
                Arguments.of(oggi.minusYears(100).plusDays(1).toString(), "100 anni fa più un giorno"));
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("dateValideAlLimite")
    @DisplayName("45b - registrazione con data di nascita di oggi o di 100 anni fa più un giorno (valori limite): accettata")
    void register_dataNascitaAlLimite_registrazioneAccettata(String data, String quando) throws Exception {
        servlet.doPost(registrazione().parametro("dataNascita", data).get(), risposta.get());

        assertRegistrazioneAccettata("la data di nascita di " + quando + " (" + data + ")");
    }

    @Test
    @DisplayName("46 - registrazione con data in formato sbagliato: val 0 senza eccezioni")
    void register_dataNascitaNonValida_rispostaDiErrore() throws Exception {
        RichiestaFinta richiesta = registrazione().parametro("dataNascita", "10/05/2000");

        // Guardia: la risposta si può controllare solo se la servlet non è fallita.
        assertDoesNotThrow(() -> servlet.doPost(richiesta.get(), risposta.get()),
                "Una data in formato sbagliato non deve far fallire la richiesta con un'eccezione");
        assertRegistrazioneRifiutata("una data in formato sbagliato");
    }

    // Date nel formato giusto ma inesistenti, o con altro testo dopo la data: vanno rifiutate, non normalizzate.
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"2000-13-45", "2001-02-29", "2000-05-10abc"})
    @DisplayName("46b - registrazione con una data inesistente o seguita da altro testo: rifiutata")
    void register_dataInesistenteOConTestoInPiu_registrazioneRifiutata(String data) throws Exception {
        servlet.doPost(registrazione().parametro("dataNascita", data).get(), risposta.get());

        assertRegistrazioneRifiutata("la data " + data);
    }

    // Una classe non valida per caso: ogni campo obbligatorio vuoto (la password vuota è il test 42),
    // più due valori speciali sul nome: parametro assente (null) e soli spazi.
    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] {0} = \"{1}\"")
    @CsvSource(value = {
            "nome, ''",
            "cognome, ''",
            "new_username, ''",
            "phoneNumber, ''",
            "dataNascita, ''",
            "email, ''",
            "indirizzo, ''",
            "nome, NULL",
            "nome, '   '"
    }, nullValues = "NULL")
    @DisplayName("47 - registrazione con un campo obbligatorio vuoto, assente o di soli spazi: rifiutata")
    void register_campoObbligatorioVuoto_registrazioneRifiutata(String campo, String valore) throws Exception {
        servlet.doPost(registrazione().parametro(campo, valore).get(), risposta.get());

        assertRegistrazioneRifiutata("il campo " + campo + " uguale a \"" + valore + "\"");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvSource({
            "nome, Giulia2, il nome con una cifra",
            "cognome, Bianchi3, il cognome con una cifra",
            "new_username, giulia_01, lo username con un trattino basso"
    })
    @DisplayName("47b - registrazione con nome, cognome o username in formato non valido: rifiutata")
    void register_formatoNomeCognomeOUsernameNonValido_registrazioneRifiutata(String campo, String valore,
            String motivo) throws Exception {
        servlet.doPost(registrazione().parametro(campo, valore).get(), risposta.get());

        assertRegistrazioneRifiutata(motivo);
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

        assertAll(
                () -> verify(userModel, never().description("Con " + motivo + " il DB non va interrogato"))
                        .getUserTramiteCredenziali(anyString(), anyString()),
                () -> verify(contesto, description("Si torna alla pagina di login"))
                        .getRequestDispatcher("/LoginRegistrazione.jsp"),
                () -> verify(dispatcher).forward(richiesta.get(), risposta.get()));
    }

    @Test
    @DisplayName("52 - login senza username e password: nessuna eccezione")
    void login_parametriMancanti_nessunaEccezione() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "login");

        // Guardia: le altre verifiche hanno senso solo se la servlet non è fallita.
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

    // Ospite con due prodotti in sessione: il primo è già nel carrello salvato, il secondo no.
    @Test
    @DisplayName("53b - login da ospite con carrello: prodotti uniti al carrello salvato, quantità sommate")
    void login_ospiteConCarrello_prodottiUnitiAlCarrelloSalvato() throws Exception {
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD)).thenReturn(utente(false));
        ProductBean rossetto = prodotto(1, 2);
        ProductBean mascara = prodotto(2, 1);
        CartBean carrelloOspite = new CartBean();
        carrelloOspite.addProduct(rossetto);
        carrelloOspite.addProduct(mascara);
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);
        richiesta.sessione().con("cart", carrelloOspite);
        when(cartModel.getProducts(ID_UTENTE)).thenReturn(new ArrayList<>(List.of(prodotto(1, 3))));

        servlet.doPost(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(cartModel, description("Il rossetto c'è già: la quantità aumenta di 2"))
                        .addProduct(ID_CARRELLO, rossetto, true, 2),
                () -> verify(cartModel, description("Il mascara è nuovo: va inserito con quantità 1"))
                        .addProduct(ID_CARRELLO, mascara, false, 1),
                () -> assertNull(richiesta.sessioneCorrente().attributo("cart"),
                        "Il carrello dell'ospite non resta in sessione: CartControl carica quello salvato"),
                () -> assertEquals("1", risposta.val(), "Il login dovrebbe riuscire"));
    }

    @Test
    @DisplayName("53c - login riuscito di un ospite: l'identificativo di sessione viene rinnovato")
    void login_ospite_idSessioneRinnovato() throws Exception {
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD)).thenReturn(utente(false));
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);

        servlet.doPost(richiesta.get(), risposta.get());

        verify(richiesta.get(), description("Chi conosceva l'id della sessione dell'ospite non deve diventare l'utente"))
                .changeSessionId();
    }

    @Test
    @DisplayName("53d - login fallito: l'identificativo di sessione resta lo stesso e il carrello dell'ospite non si tocca")
    void login_credenzialiErrate_sessioneECarrelloInvariati() throws Exception {
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD)).thenReturn(new UserBean());
        CartBean carrelloOspite = new CartBean();
        carrelloOspite.addProduct(prodotto(1, 2));
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);
        richiesta.sessione().con("cart", carrelloOspite);

        servlet.doPost(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(richiesta.get(), never().description("Senza login la sessione non cambia")).changeSessionId(),
                () -> verifyNoInteractions(cartModel),
                () -> assertEquals(carrelloOspite, richiesta.sessioneCorrente().attributo("cart"),
                        "Il carrello dell'ospite resta in sessione"));
    }

    @Test
    @DisplayName("54 - login con errore del database: redirect alla pagina di errore")
    void login_erroreDatabase_redirectPaginaErrore() throws Exception {
        when(userModel.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD))
                .thenThrow(new SQLException("database non raggiungibile"));
        RichiestaFinta richiesta = login(USERNAME, PASSWORD);

        servlet.doPost(richiesta.get(), risposta.get());

        verify(risposta.get(), description("L'utente va rimandato alla pagina di errore")).sendRedirect("/Error.jsp");
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

        assertAll(
                () -> verify(richiesta.get(), description("I dati letti dal DB vanno passati alla JSP"))
                        .setAttribute("datiUtente", utente),
                () -> verify(contesto, description("Si va all'area utente")).getRequestDispatcher("/AreaUtente.jsp"),
                () -> verify(dispatcher).forward(richiesta.get(), risposta.get()));
    }

    @Test
    @DisplayName("55b - dati utente con errore del database: redirect alla pagina di errore")
    void datiUtente_erroreDatabase_redirectPaginaErrore() throws Exception {
        when(userModel.getUserTramiteID(ID_UTENTE)).thenThrow(new SQLException("database non raggiungibile"));
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "DatiUtente");
        richiesta.sessione().con("userID", ID_UTENTE);

        servlet.doPost(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(risposta.get(), description("Senza dati da mostrare si va alla pagina di errore"))
                        .sendRedirect("/Error.jsp"),
                () -> verify(dispatcher, never().description("L'area utente non va mostrata")).forward(any(), any()));
    }

    @Test
    @DisplayName("56 - dati utente senza login: nessuna eccezione")
    void datiUtente_utenteNonLoggato_nessunaEccezione() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "DatiUtente");

        // Guardia: la verifica sul DB ha senso solo se la servlet non è fallita.
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

        assertAll(
                () -> assertTrue(sessione.isInvalidata(), "La sessione dovrebbe essere invalidata"),
                () -> verify(contesto, description("Si torna alla pagina di login"))
                        .getRequestDispatcher("/LoginRegistrazione.jsp"),
                () -> verify(dispatcher).forward(richiesta.get(), risposta.get()));
    }

    @Test
    @DisplayName("58 - azione sconosciuta: forward alla home")
    void azioneSconosciuta_forwardHome() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/User").parametro("action", "sconosciuta");

        servlet.doPost(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(contesto, description("Si va alla home")).getRequestDispatcher("/Home.jsp"),
                () -> verify(dispatcher).forward(richiesta.get(), risposta.get()));
    }
}
