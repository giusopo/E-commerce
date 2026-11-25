package control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import testutil.ServletMocks.RichiestaFinta;
import testutil.ServletMocks.RispostaFinta;

/**
 * Test 59-67 di pianoTest.md: il filtro che protegge l'area /admin e la servlet /Admin.
 *
 * I test seguono la tabella di decisione del filtro: condizioni "area admin" (pagina /admin,
 * servlet /Admin, percorso nel path info), "utente loggato" e "ruolo admin"; azioni "richiesta
 * inoltrata" oppure "redirect al login". Per le pagine pubbliche login e ruolo sono indifferenti.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthFilter - accesso all'area admin")
class AuthFilterTest {

    private static final String PAGINA_ADMIN = "/admin/modificaProdotto.jsp";
    /** La servlet AdminControl, che esegue le azioni dell'amministratore. */
    private static final String SERVLET_ADMIN = "/Admin";

    @Mock private FilterChain catena;

    private final AuthFilter filtro = new AuthFilter();
    private final RispostaFinta risposta = new RispostaFinta();

    private void assertRedirectAlLogin(String destinazione) {
        assertAll(
                () -> verify(risposta.get(), description("Si va alla pagina di login")).sendRedirect(destinazione),
                () -> verify(catena, never().description("La pagina admin non deve essere servita"))
                        .doFilter(any(), any()));
    }

    private void assertRichiestaInoltrata(RichiestaFinta richiesta) {
        assertAll(
                () -> verify(catena, description("La richiesta deve proseguire")).doFilter(richiesta.get(), risposta.get()),
                () -> verify(risposta.get(), never().description("Non deve esserci un redirect"))
                        .sendRedirect(anyString()));
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

    // Classe "percorso admin nel path info": servlet path vuoto (servlet mappata su "/*") e area admin
    // indicata solo dal path info. Il filtro deve guardare il percorso completo, non solo il servlet path.
    @Test
    @DisplayName("63b - pagina admin indicata nel path info, senza login: redirect al login")
    void doFilter_adminNelPathInfoSenzaLogin_redirectAlLogin() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", PAGINA_ADMIN).percorso("", PAGINA_ADMIN);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRedirectAlLogin("/LoginRegistrazione.jsp");
    }

    // ---------------------------------------------------------------- servlet /Admin

    @Test
    @DisplayName("64 - servlet /Admin senza login: redirect al login")
    void doFilter_servletAdminSenzaLogin_redirectAlLogin() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", SERVLET_ADMIN);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRedirectAlLogin("/LoginRegistrazione.jsp");
    }

    @Test
    @DisplayName("65 - servlet /Admin con utente non admin: redirect al login")
    void doFilter_servletAdminUtenteNonAdmin_redirectAlLogin() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", SERVLET_ADMIN);
        richiesta.sessione().con("userID", 42).con("adminRoles", false);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRedirectAlLogin("/LoginRegistrazione.jsp");
    }

    @Test
    @DisplayName("66 - servlet /Admin con utente admin: la richiesta passa")
    void doFilter_servletAdminUtenteAdmin_richiestaInoltrata() throws Exception {
        RichiestaFinta richiesta = new RichiestaFinta("", SERVLET_ADMIN);
        richiesta.sessione().con("userID", 1).con("adminRoles", true);

        filtro.doFilter(richiesta.get(), risposta.get(), catena);

        assertRichiestaInoltrata(richiesta);
    }

    @Test
    @DisplayName("67 - web.xml: AuthFilter è applicato a ogni URL della servlet Admin")
    void webXml_servletAdmin_coperteDaAuthFilter() throws Exception {
        Document webXml = leggiWebXml();

        Set<String> urlFiltro = urlPattern(webXml, "filter-mapping", "filter-name", "AuthFilter");
        Set<String> urlServletAdmin = urlPattern(webXml, "servlet-mapping", "servlet-name", "Admin");

        // Guardia: senza la mappatura della servlet il confronto non avrebbe senso.
        assertFalse(urlServletAdmin.isEmpty(), "web.xml dovrebbe mappare la servlet Admin");
        assertTrue(urlFiltro.containsAll(urlServletAdmin),
                "AuthFilter copre " + urlFiltro + " ma la servlet Admin risponde anche su " + urlServletAdmin);
    }

    /** Legge src/main/webapp/WEB-INF/web.xml, senza risolvere DTD o entità esterne. */
    private static Document leggiWebXml() throws Exception {
        DocumentBuilderFactory fabbrica = DocumentBuilderFactory.newInstance();
        fabbrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabbrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return fabbrica.newDocumentBuilder().parse(Path.of("src", "main", "webapp", "WEB-INF", "web.xml").toFile());
    }

    /** Gli url-pattern dei blocchi (filter-mapping o servlet-mapping) con il nome indicato. */
    private static Set<String> urlPattern(Document webXml, String blocco, String tagNome, String nome) {
        Set<String> pattern = new HashSet<>();
        NodeList mappature = webXml.getElementsByTagName(blocco);
        for (int i = 0; i < mappature.getLength(); i++) {
            Element mappatura = (Element) mappature.item(i);
            if (nome.equals(mappatura.getElementsByTagName(tagNome).item(0).getTextContent().trim())) {
                NodeList url = mappatura.getElementsByTagName("url-pattern");
                for (int j = 0; j < url.getLength(); j++) {
                    pattern.add(url.item(j).getTextContent().trim());
                }
            }
        }
        return pattern;
    }
}
