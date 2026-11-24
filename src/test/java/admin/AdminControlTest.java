package admin;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Interface.ProductModel_intf;
import Model.Interface.UserModel_intf;
import testutil.ServletMocks.RichiestaFinta;
import testutil.ServletMocks.RispostaFinta;

/**
 * Test 68-69 di pianoTest.md: la servlet delle azioni dell'amministratore verifica da sola il ruolo,
 * come difesa in profondità nel caso in cui AuthFilter non venga applicato.
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

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] {0}, utente loggato={1}")
    @CsvSource({
            "impostaSconto,    false",
            "impostaSconto,    true",
            "visualizzaUtenti, false",
            "visualizzaUtenti, true",
            "readallEditPrice, false",
            "readallEditPrice, true"
    })
    @DisplayName("68 - azione admin senza ruolo admin: nessun accesso al DB e redirect al login")
    void azioneAdmin_utenteNonAdmin_nessunaModificaERedirect(String azione, boolean utenteLoggato) throws Exception {
        RichiestaFinta richiesta = azione(azione);
        if (utenteLoggato) {
            richiesta.sessione().con("userID", 42).con("adminRoles", false);
        }

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verifyNoInteractions(azioniModel, userModel, productModel),
                () -> verify(risposta.get(), description("Chi non è admin va rimandato al login"))
                        .sendRedirect("/LoginRegistrazione.jsp"));
    }

    @Test
    @DisplayName("69 - azione admin con ruolo admin: l'azione viene eseguita")
    void azioneAdmin_utenteAdmin_azioneEseguita() throws Exception {
        RichiestaFinta richiesta = azione("impostaSconto");
        richiesta.sessione().con("userID", 1).con("adminRoles", true);

        servlet.doGet(richiesta.get(), risposta.get());

        assertAll(
                () -> verify(azioniModel, description("Lo sconto deve essere salvato")).modificaSconto(5, 50),
                () -> verify(contesto, description("Si torna alla pagina dei prodotti"))
                        .getRequestDispatcher("/admin/modificaProdotto.jsp"),
                () -> verify(dispatcher).forward(richiesta.get(), risposta.get()));
    }
}
