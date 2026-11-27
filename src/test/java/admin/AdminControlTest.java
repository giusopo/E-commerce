package admin;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Interface.ProductModel_intf;
import Model.Interface.UserModel_intf;
import testutil.ServletMocks.RichiestaFinta;
import testutil.ServletMocks.RispostaFinta;

/**
 * Test 68a-69d di pianoTest.md: la servlet delle azioni dell'amministratore verifica da sola il ruolo,
 * come difesa in profondità nel caso in cui AuthFilter non venga applicato.
 *
 * Tabella di decisione: utente anonimo (68a) o non admin (68b) → accesso negato; utente admin (69)
 * → azione eseguita. Per l'admin: sconto non salvato (69b, 69c) o azione mancante o sconosciuta (69d)
 * → pagina di errore.
 *
 * I DAO sono mock che sostituiscono i campi statici della servlet; richiesta, risposta e sessione
 * sono oggetti finti.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminControl - azioni dell'amministratore")
class AdminControlTest {

    @Mock private AzioniAdminModel_intf azioniModel;
    @Mock private UserModel_intf userModel;
    @Mock private ProductModel_intf productModel;
    @Mock private ServletConfig config;
    @Mock private ServletContext contesto;
    @Mock private RequestDispatcher dispatcher;

    private AzioniAdminModel_intf azioniModelOriginale;
    private UserModel_intf userModelOriginale;
    private ProductModel_intf productModelOriginale;
    private AdminControl servlet;
    private final RispostaFinta risposta = new RispostaFinta();

    @BeforeEach
    void init() throws ServletException {
        azioniModelOriginale = AdminControl.model;
        userModelOriginale = AdminControl.userModel;
        productModelOriginale = AdminControl.productModel;
        AdminControl.model = azioniModel;
        AdminControl.userModel = userModel;
        AdminControl.productModel = productModel;

        lenient().when(config.getServletContext()).thenReturn(contesto);
        lenient().when(contesto.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        servlet = new AdminControl();
        servlet.init(config);
    }

    @AfterEach
    void ripristinaModel() {
        AdminControl.model = azioniModelOriginale;
        AdminControl.userModel = userModelOriginale;
        AdminControl.productModel = productModelOriginale;
    }

    /** Richiesta a /Admin con i parametri di tutte le azioni provate. */
    private static RichiestaFinta azione(String azione) {
        return new RichiestaFinta("", "/Admin")
                .parametro("action", azione)
                .parametro("id", "5")
                .parametro("sconto", "50");
    }

    /** Richiesta di un utente admin. */
    private static RichiestaFinta azioneDaAdmin(String azione) {
        RichiestaFinta richiesta = azione(azione);
        richiesta.sessione().con("userID", 1).con("adminRoles", true);
        return richiesta;
    }

    private void assertAccessoNegato() {
        assertAll(
                () -> verifyNoInteractions(azioniModel, userModel, productModel),
                () -> verify(risposta.get(), description("Chi non è admin va rimandato al login"))
                        .sendRedirect("/LoginRegistrazione.jsp"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"impostaSconto", "visualizzaUtenti", "readallEditPrice"})
    @DisplayName("68a - azione admin senza login: nessun accesso al DB e redirect al login")
    void azioneAdmin_utenteAnonimo_nessunaModificaERedirect(String azione) throws Exception {
        RichiestaFinta richiesta = azione(azione);

        servlet.doGet(richiesta.get(), risposta.get());

        assertAccessoNegato();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"impostaSconto", "visualizzaUtenti", "readallEditPrice"})
    @DisplayName("68b - azione admin con utente loggato non admin: nessun accesso al DB e redirect al login")
    void azioneAdmin_utenteNonAdmin_nessunaModificaERedirect(String azione) throws Exception {
        RichiestaFinta richiesta = azione(azione);
        richiesta.sessione().con("userID", 42).con("adminRoles", false);

        servlet.doGet(richiesta.get(), risposta.get());

        assertAccessoNegato();
    }

    // Il ruolo viene controllato prima di leggere l'azione: nella tabella di decisione l'azione è
    // indifferente, quindi basta un'azione per verificare che un admin superi il controllo.
    @Test
    @DisplayName("69 - impostaSconto con ruolo admin: lo sconto viene salvato")
    void azioneAdmin_utenteAdmin_azioneEseguita() throws Exception {
        when(azioniModel.modificaSconto(5, 50)).thenReturn(true);
        RichiestaFinta richiesta = azioneDaAdmin("impostaSconto");

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(azioniModel, description("Lo sconto deve essere salvato")).modificaSconto(5, 50),
                () -> verify(contesto, description("Si torna alla pagina dei prodotti"))
                        .getRequestDispatcher("/admin/modificaProdotto.jsp"),
                () -> verify(dispatcher).forward(richiesta.get(), risposta.get()));
    }

    @Test
    @DisplayName("69b - impostaSconto su un prodotto inesistente: pagina di errore, non la pagina dei prodotti")
    void impostaSconto_prodottoInesistente_paginaDiErrore() throws Exception {
        when(azioniModel.modificaSconto(5, 50)).thenReturn(false);
        RichiestaFinta richiesta = azioneDaAdmin("impostaSconto");

        servlet.doGet(richiesta.get(), risposta.get());

        assertScontoNonMostratoComeSalvato();
    }

    @Test
    @DisplayName("69c - impostaSconto con errore del database: pagina di errore, non la pagina dei prodotti")
    void impostaSconto_erroreDatabase_paginaDiErrore() throws Exception {
        when(azioniModel.modificaSconto(5, 50)).thenThrow(new SQLException("database non raggiungibile"));
        RichiestaFinta richiesta = azioneDaAdmin("impostaSconto");

        servlet.doGet(richiesta.get(), risposta.get());

        assertScontoNonMostratoComeSalvato();
    }

    @ParameterizedTest(name = "[{index}] azione \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"azioneInesistente"})
    @DisplayName("69d - azione mancante, vuota o sconosciuta: redirect alla pagina di errore Error.jsp")
    void azioneMancanteOSconosciuta_utenteAdmin_paginaDiErrore(String azione) throws Exception {
        RichiestaFinta richiesta = azioneDaAdmin(azione);

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(risposta.get(), description("La pagina di errore si chiama Error.jsp"))
                        .sendRedirect("/Error.jsp"),
                () -> verifyNoInteractions(azioniModel, userModel, productModel));
    }

    private void assertScontoNonMostratoComeSalvato() {
        assertAll(
                () -> verify(risposta.get(), description("Uno sconto non salvato porta alla pagina di errore"))
                        .sendRedirect("/Error.jsp"),
                () -> verify(dispatcher, never().description("La pagina dei prodotti non va mostrata"))
                        .forward(any(), any()));
    }
}
