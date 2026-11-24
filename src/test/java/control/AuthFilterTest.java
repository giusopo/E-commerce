package control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import javax.servlet.FilterChain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import testutil.ServletMocks.RichiestaFinta;
import testutil.ServletMocks.RispostaFinta;

/**
 * Test 59-63 di pianoTest.md: il filtro che protegge l'area /admin.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthFilter - accesso all'area admin")
class AuthFilterTest {

    private static final String PAGINA_ADMIN = "/admin/modificaProdotto.jsp";

    @Mock private FilterChain catena;

    private final AuthFilter filtro = new AuthFilter();
    private final RispostaFinta risposta = new RispostaFinta();

    private void assertRedirectAlLogin(String destinazione) throws Exception {
        verify(risposta.get()).sendRedirect(destinazione);
        verify(catena, never()).doFilter(any(), any());
    }

    private void assertRichiestaInoltrata(RichiestaFinta richiesta) throws Exception {
        verify(catena).doFilter(richiesta.get(), risposta.get());
        verify(risposta.get(), never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("59 - pagina pubblica: la richiesta passa")
    void doFilter_paginaPubblica_richiestaInoltrata() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", "/Home.jsp");

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRichiestaInoltrata(richiesta);
    }

    @Test
    @DisplayName("60 - pagina admin senza login: redirect al login")
    void doFilter_adminSenzaLogin_redirectAlLogin() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", PAGINA_ADMIN);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRedirectAlLogin("/LoginRegistrazione.jsp");
    }

    @Test
    @DisplayName("61 - pagina admin con utente non admin: redirect al login")
    void doFilter_utenteNonAdmin_redirectAlLogin() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", PAGINA_ADMIN);
        richiesta.sessione().con("userID", 42).con("adminRoles", false);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRedirectAlLogin("/LoginRegistrazione.jsp");
    }

    @Test
    @DisplayName("62 - pagina admin con utente admin: la richiesta passa")
    void doFilter_utenteAdmin_richiestaInoltrata() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", PAGINA_ADMIN);
        richiesta.sessione().con("userID", 1).con("adminRoles", true);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRichiestaInoltrata(richiesta);
    }

    @Test
    @DisplayName("63 - app sotto /shop: il redirect al login mantiene il percorso")
    void doFilter_contextPathNonVuoto_redirectConContextPath() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("/shop", "/shop" + PAGINA_ADMIN);
        richiesta.sessione().con("userID", 42).con("adminRoles", false);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRedirectAlLogin("/shop/LoginRegistrazione.jsp");
    }
}
