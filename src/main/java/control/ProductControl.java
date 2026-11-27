package control;

import java.io.IOException; 
import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import Model.ProductModel;
import Model.RecensioneModel;
import Model.Bean.ProductBean;
import Model.Bean.RecensioneBean;
import Model.Interface.*;


//@WebServlet(name = "Home", urlPatterns = "/")
public class ProductControl extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	static ProductModel_intf model = new ProductModel();
	static RecensioneModel_intf modelRecensione = new RecensioneModel();

	/** Prodotti per pagina nella home, se il parametro limit manca o non è valido. */
	private static final int LIMITE_PREDEFINITO = 15;

	public ProductControl() {
		super();
	}

	private boolean punteggioValido(int value) {
        return value >= 1 && value <= 5;
	}

	// Il commento viene salvato già sottoposto a escape, quindi le pagine lo stampano così com'è.
	private String commentoValido(String txt) {
	    return Html.escape(txt);
	}

	/** Il parametro come intero positivo, oppure predefinito se manca, non è un numero o è minore di 1. */
	private static int interoPositivo(String parametro, int predefinito) {
		try {
			int valore = Integer.parseInt(parametro);
			return valore >= 1 ? valore : predefinito;
		} catch (NumberFormatException e) {
			return predefinito;
		}
	}


	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = req.getParameter("action");
        HttpSession session = req.getSession();

        try {

            if (action != null) {
            	System.out.println("ProductControl - Action: " + action);

                // productview - visualizzare un singolo prodotto
                if (action.equalsIgnoreCase("read")) {

                	Integer userIDobj = (Integer)session.getAttribute("userID");
                    int userID = -1;

                    if (userIDobj != null) {
                    	userID = userIDobj.intValue();
                    }

                    // Senza un ProductID numerico, o con un prodotto che non esiste, non c'è una scheda da mostrare.
                    int prodID;
                    try {
                    	prodID = Integer.parseInt(req.getParameter("ProductID"));
                    } catch (NumberFormatException e) {
                    	resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    	return;
                    }
                    ProductBean prodotto = model.doRetrieveByKey(prodID);
                    if (prodotto.isEmpty()) {
                    	resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    	return;
                    }

                    req.setAttribute("product", prodotto);
                    // se l'utente è loggato e ha lasciato una recensione la mostra (per l'ospite è vuota)
                    req.setAttribute("recensione", modelRecensione.getRecensione(userID, prodID));
                    req.setAttribute("listaRecensioni", modelRecensione.getAllRecensioni(prodID));
                	System.out.println("sei nel control: "+ req.getAttribute("listaRecensioni"));

                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/productView.jsp");
                    dispatcher.forward(req, resp);
                }
                else if (action.equalsIgnoreCase("dettagliBarraRicercaJSON")) {
                    
                	String input = req.getParameter("input");
                	StringBuilder prodotti = model.doRetrieveByInput(input);

                    System.out.println(prodotti);
                     
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                     
                    // Invia la risposta JSON
                    resp.getWriter().write(prodotti.toString());
                 }
                // home - visualizza tutte le categorie
                else if (action.equalsIgnoreCase("readall")) {
                	req.removeAttribute("products");
          			req.setAttribute("products", model.doRetrieveByCat("all", "ID"));
                    
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
                    dispatcher.forward(req, resp);                	
                }
                // home - visualizza prodotti per categoria
                else if (action.equalsIgnoreCase("readbycat")) {
                	String category = req.getParameter("category");
                	System.out.println("ProductControl - Category: " + category);

                	req.removeAttribute("products");
          			req.setAttribute("products", model.doRetrieveByCat(category, "ID"));
                    
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
                    dispatcher.forward(req, resp);                	
                }
                // home - visualizza prodotti per categoria e sottocategoria
                else if (action.equalsIgnoreCase("readbysubcat")) {
                	String category = req.getParameter("category");
                	System.out.println("ProductControl - Category: " + category);

                	String subcategory = req.getParameter("subcategory");
                	System.out.println("ProductControl - SubCategory: " + subcategory);

                	req.removeAttribute("products");
          			req.setAttribute("products", model.doRetrieveBySubCat(category, subcategory, "ID"));
                    
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
                    dispatcher.forward(req, resp);                	
                } else {
                	System.out.println("ProductControl - ERRORE: Not handled!!!");
                }                
            // home - mostra tutti i prodotti nel db
            } else {
            	System.out.println("ProductControl - Action: no action");

            	req.removeAttribute("products");
            	// pagina e prodotti per pagina mancanti o non validi: prima pagina da 15 prodotti
            	int page = interoPositivo(req.getParameter("page"), 1);
            	int limit = interoPositivo(req.getParameter("limit"), LIMITE_PREDEFINITO);

            	// calcolato in long: con pagine molto alte il prodotto supererebbe il massimo di un int
            	int offset = (int) Math.min((long) (page - 1) * limit, Integer.MAX_VALUE);

            	try {
            	    req.setAttribute("products", model.doRetrieveAll("ID", offset, limit));
            	    req.setAttribute("currentPage", page);
            	    req.setAttribute("limit", limit);

            	    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
            	    dispatcher.forward(req, resp);

            	} catch (SQLException e) {
            	    System.err.println("Errore SQL durante il recupero dei prodotti: " + e.getMessage());
            	    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante il recupero dei prodotti.");
            	}

            }

        } catch (SQLException e) {
            System.out.println("Error:" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = req.getParameter("action");

        if ("recensione".equalsIgnoreCase(action) || "modificaRecensione".equalsIgnoreCase(action)) {
            // Inserisce o modifica la recensione e risponde {"val":"1"} solo se è stata salvata.
            boolean salvata = salvaRecensione(req, action.equalsIgnoreCase("modificaRecensione"));

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"val\":\"" + (salvata ? 1 : 0) + "\"}");
        }
    }

    private boolean salvaRecensione(HttpServletRequest req, boolean modifica) {
        HttpSession session = req.getSession(false);
        Integer userID = session == null ? null : (Integer) session.getAttribute("userID");
        if (userID == null) {
            return false;
        }

        int prodID;
        int punteggio;
        try {
            prodID = Integer.parseInt(req.getParameter("productID"));
            punteggio = Integer.parseInt(req.getParameter("punteggio"));
        } catch (NumberFormatException e) {
            return false;
        }
        if (!punteggioValido(punteggio)) {
            return false;
        }

        String commento = req.getParameter("commento");
        commento = commentoValido(commento == null ? "" : commento);

        try {
            if (!modifica) {
                modelRecensione.addRecensione(userID, prodID, punteggio, commento);
                return true;
            }

            // L'id della recensione arriva dal client: si modifica solo se è la recensione
            // che l'utente loggato ha scritto su questo prodotto.
            RecensioneBean propria = modelRecensione.getRecensione(userID, prodID);
            if (propria == null || propria.isEmpty()
                    || !String.valueOf(propria.getID()).equals(req.getParameter("recensioneID"))) {
                return false;
            }
            modelRecensione.modificaRecensione(propria.getID(), punteggio, commento);
            return true;
        } catch (SQLException e) {
            System.out.println("Errore nel salvataggio della recensione: " + e.getMessage());
            return false;
        }
    }
}
