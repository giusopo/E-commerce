package control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
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

/**
 * Test 39-48 di pianoTest.md: registrazione.
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
}
