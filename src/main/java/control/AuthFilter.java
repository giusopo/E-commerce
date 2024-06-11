package control;


import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthFilter implements Filter {

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hrequest = (HttpServletRequest) request;
		HttpServletResponse hresponse = (HttpServletResponse) response;
		
		String loginURI = hrequest.getContextPath() + "/admin";
		boolean loginRequest = hrequest.getRequestURI().startsWith(loginURI);
		boolean isAdmin;
		
		if(hrequest.getSession().getAttribute("adminRoles") != null)
			isAdmin = (boolean) hrequest.getSession().getAttribute("adminRoles");
		else
			isAdmin = false;
		
		System.out.println("ADMIN : " + isAdmin);
		
		if(loginRequest) {
			HttpSession session = hrequest.getSession(false);
			boolean loggedIn = session != null && session.getAttribute("adminRoles") != null;

			if(!loggedIn || isAdmin == false) {
				System.out.println("Redirect to login form");
				hresponse.sendRedirect(hrequest.getContextPath()+ "/User.jsp");
			} else{
				// admin resource
				chain.doFilter(request, response);
			}
		} else {
			// accessible resource
			chain.doFilter(request, response);
		}
	}

	public void init(FilterConfig fConfig) throws ServletException {
		System.out.println("Init the filter");
	}

}
