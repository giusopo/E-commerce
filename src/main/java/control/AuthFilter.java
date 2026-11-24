package control;


import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Protegge l'area dell'amministratore: le pagine in /admin/ e la servlet /Admin che esegue le azioni.
 * Gli URL a cui il filtro si applica sono indicati in web.xml.
 */
public class AuthFilter implements Filter {

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hrequest = (HttpServletRequest) request;
		HttpServletResponse hresponse = (HttpServletResponse) response;

		if (!isAreaAdmin(hrequest)) {
			// accessible resource
			chain.doFilter(request, response);
			return;
		}

		// getSession(false): controllare i permessi non deve creare una sessione
		HttpSession session = hrequest.getSession(false);
		boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("adminRoles"));

		if (isAdmin) {
			// admin resource
			chain.doFilter(request, response);
		} else {
			hresponse.sendRedirect(hrequest.getContextPath() + "/LoginRegistrazione.jsp");
		}
	}

	/**
	 * Vero per le pagine /admin/... e per la servlet /Admin. Il confronto ignora le maiuscole e usa
	 * il percorso già normalizzato dal container (senza context path e senza parametri come ;jsessionid).
	 */
	private static boolean isAreaAdmin(HttpServletRequest request) {
		String percorso = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
		return percorso.toLowerCase(Locale.ROOT).startsWith("/admin");
	}

	public void init(FilterConfig fConfig) throws ServletException {
		System.out.println("Init the filter");
	}

}
