package control;

import java.io.IOException; 
import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Model.ProductModel;
import Model.Interface.ProductModel_intf;


//@WebServlet(name = "Home", urlPatterns = "/")
public class ProductControl extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	static ProductModel_intf model = new ProductModel();
	
	public ProductControl() {
		super();
	}

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = req.getParameter("action");

        try {

            if (action != null) {

                // visualizzare un singolo prodotto
                if (action.equalsIgnoreCase("read")) {

                    int id = Integer.parseInt(req.getParameter("id"));
                    req.removeAttribute("product");
                    req.setAttribute("product", model.doRetrieveByKey(id));

                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/productView.jsp");
                    dispatcher.forward(req, resp); 
                }
                

            }
        } catch (SQLException e) {
            System.out.println("Error:" + e.getMessage());
            throw new RuntimeException(e);
        }

//        String sort = req.getParameter("sort");
//
//        try {
//            req.removeAttribute("products");
//            req.setAttribute("products", model.doRetrieveAll(sort));
//        } catch (SQLException e) {
//            System.out.println("Error:" + e.getMessage());
//        }
        
    	req.removeAttribute("products");
        try {
			req.setAttribute("products", model.doRetrieveAll("ID"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
                        // mostra tutti i prodotti nel db
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
        dispatcher.forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }
}
