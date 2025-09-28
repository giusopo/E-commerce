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
import Model.Interface.*;


//@WebServlet(name = "Home", urlPatterns = "/")
public class ProductControl extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	static ProductModel_intf model = new ProductModel();
	static RecensioneModel_intf modelRecensione = new RecensioneModel();

	public ProductControl() {
		super();
	}
	
	private boolean punteggioValido(int value) {
        return value >= 1 && value <= 5;
	}
	
	private String commentoValido(String txt) {
	    txt = txt.replace("&", "&amp;");
	    txt = txt.replace("<", "&lt;");
	    txt = txt.replace(">", "&gt;");
	    txt = txt.replace("\"", "&quot;");
	    txt = txt.replace("'", "&#039;");
	    return txt;
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
                    
                    String prodIDStr = req.getParameter("ProductID");
                    int prodID = -1;
                    
                    if (userIDobj != null) {
                    	userID = userIDobj.intValue();
                    }
                    
                    // se l'utente è loggato e ha lasciato una recensione la mostra
                    if (prodIDStr != null && !prodIDStr.isEmpty()) {
                    	
                        prodID = Integer.parseInt(prodIDStr);
                        req.setAttribute("recensione", modelRecensione.getRecensione(userID, prodID));
                    } else {
                        // caso in cui ProductID non è fornito o è vuoto
                    }
                    
                    req.removeAttribute("product");
                    req.setAttribute("product", model.doRetrieveByKey(prodID));
                    
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
            	String pageParam = req.getParameter("page");
            	String limitParam = req.getParameter("limit");
            	int limit;
            	int page = 1; // Impostato a 1 come default se non specificato o non valido
            	try {
            	    page = Integer.parseInt(pageParam);
            	} catch (NumberFormatException e) {
            	    System.err.println("Errore durante il parsing del parametro 'page': " + e.getMessage());
            	}

            	if(limitParam == null)
            		limit = 15; // Limite di prodotti per pagina
            	else
            		limit = Integer.parseInt(limitParam);
            	
            	int offset = (page - 1) * limit;

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
        HttpSession session = req.getSession();
        
        int userID = (int)session.getAttribute("userID");
        int prodID = Integer.parseInt(req.getParameter("productID"));
        
        int punteggio = Integer.parseInt(req.getParameter("punteggio"));
    	String commento = req.getParameter("commento");
    	
    	commento = this.commentoValido(commento);
        
        if(action != null) {
        	if (action.equalsIgnoreCase("recensione")) {
        		// riceve il form lo valida e inserisce la recensione,
        		// restituisce una risposta positiva se ok, altrimenti negativa
        		
            	resp.setContentType("application/json");
            	resp.setCharacterEncoding("UTF-8");
            	
            	if(this.punteggioValido(punteggio)) {
            		
            		System.out.println("dati validi");
            		
            		try {
						modelRecensione.addRecensione(userID, prodID, punteggio, commento);
					} catch (SQLException e) {
						
					}
            		resp.getWriter().write("{\"val\":\"1\"}");
            	} else {
            		System.out.println("dati non validi");
            		resp.getWriter().write("{\"val\":\"1\"}");
            	}
            } else if(action.equalsIgnoreCase("modificaRecensione")) {
            	
            	System.out.println("stai modificando la recensione");
            	
            	int recensioneID = Integer.parseInt(req.getParameter("recensioneID"));
            	
            	try {
					modelRecensione.modificaRecensione(recensioneID, punteggio, commento);
				} catch (SQLException e) {
				}
            }
        }
    }
}
