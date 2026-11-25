package testutil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Richiesta, risposta e sessione HTTP finte per i test di servlet e filtri.
 * Gli oggetti sono mock di Mockito: si possono anche verificare con verify().
 */
public final class ServletMocks {

    private ServletMocks() {
    }

    /**
     * Sessione che conserva gli attributi. Come in Tomcat, dopo invalidate()
     * ogni lettura o scrittura lancia IllegalStateException.
     */
    public static final class SessioneFinta {

        private final HttpSession sessione = mock(HttpSession.class);
        private final Map<String, Object> attributi = new HashMap<>();
        private boolean invalidata;

        SessioneFinta() {
            lenient().when(sessione.getAttribute(anyString())).thenAnswer(inv -> {
                verificaValida();
                return attributi.get(inv.<String>getArgument(0));
            });
            lenient().doAnswer(inv -> {
                verificaValida();
                attributi.put(inv.getArgument(0), inv.getArgument(1));
                return null;
            }).when(sessione).setAttribute(anyString(), any());
            lenient().doAnswer(inv -> {
                verificaValida();
                attributi.remove(inv.<String>getArgument(0));
                return null;
            }).when(sessione).removeAttribute(anyString());
            lenient().doAnswer(inv -> {
                verificaValida();
                invalidata = true;
                attributi.clear();
                return null;
            }).when(sessione).invalidate();
        }

        private void verificaValida() {
            if (invalidata) {
                throw new IllegalStateException("La sessione è già stata invalidata");
            }
        }

        public HttpSession get() {
            return sessione;
        }

        public boolean isInvalidata() {
            return invalidata;
        }

        /** Legge un attributo per le verifiche del test, senza controllare se la sessione è valida. */
        public Object attributo(String nome) {
            return attributi.get(nome);
        }

        /** Imposta un attributo nella preparazione del test. */
        public SessioneFinta con(String nome, Object valore) {
            attributi.put(nome, valore);
            return this;
        }
    }

    /**
     * Richiesta con parametri e sessione. getSession() restituisce la sessione corrente
     * oppure, se manca o è stata invalidata, ne crea una nuova, come fa il container.
     */
    public static final class RichiestaFinta {

        private final HttpServletRequest richiesta = mock(HttpServletRequest.class);
        private final Map<String, String> parametri = new HashMap<>();
        private SessioneFinta sessione;

        public RichiestaFinta(String contextPath, String uri) {
            lenient().when(richiesta.getParameter(anyString()))
                    .thenAnswer(inv -> parametri.get(inv.<String>getArgument(0)));
            lenient().when(richiesta.getSession()).thenAnswer(inv -> sessioneValidaOppureNuova().get());
            lenient().when(richiesta.getSession(anyBoolean())).thenAnswer(inv -> {
                boolean crea = inv.getArgument(0);
                if (sessione != null && !sessione.isInvalidata()) {
                    return sessione.get();
                }
                return crea ? sessioneValidaOppureNuova().get() : null;
            });
            lenient().when(richiesta.getContextPath()).thenReturn(contextPath);
            lenient().when(richiesta.getRequestURI()).thenReturn(uri);
            // Come in Tomcat: il percorso della richiesta senza il context path.
            lenient().when(richiesta.getServletPath()).thenReturn(uri.substring(contextPath.length()));
        }

        private SessioneFinta sessioneValidaOppureNuova() {
            if (sessione == null || sessione.isInvalidata()) {
                sessione = new SessioneFinta();
            }
            return sessione;
        }

        public RichiestaFinta parametro(String nome, String valore) {
            parametri.put(nome, valore);
            return this;
        }

        /**
         * Imposta servlet path e path info separati, come fa il container quando la richiesta
         * arriva a una servlet mappata con un prefisso o su "/*" (servlet path vuoto).
         */
        public RichiestaFinta percorso(String servletPath, String pathInfo) {
            lenient().when(richiesta.getServletPath()).thenReturn(servletPath);
            lenient().when(richiesta.getPathInfo()).thenReturn(pathInfo);
            return this;
        }

        /** Sessione già esistente prima della richiesta (la crea se non c'è). */
        public SessioneFinta sessione() {
            return sessioneValidaOppureNuova();
        }

        /** Sessione corrente dopo la richiesta: può essere diversa da quella iniziale. */
        public SessioneFinta sessioneCorrente() {
            return sessione;
        }

        public HttpServletRequest get() {
            return richiesta;
        }
    }

    /** Risposta che raccoglie il testo scritto con getWriter(). */
    public static final class RispostaFinta {

        private static final Pattern VAL = Pattern.compile("\"val\"\\s*:\\s*\"(\\d)\"");

        private final HttpServletResponse risposta = mock(HttpServletResponse.class);
        private final StringWriter testo = new StringWriter();

        public RispostaFinta() {
            try {
                lenient().when(risposta.getWriter()).thenReturn(new PrintWriter(testo, true));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public HttpServletResponse get() {
            return risposta;
        }

        public String testo() {
            return testo.toString();
        }

        /** Valore del campo "val" della risposta JSON ("1" o "0"), oppure null se non è stato scritto. */
        public String val() {
            Matcher m = VAL.matcher(testo.toString());
            return m.find() ? m.group(1) : null;
        }
    }
}
