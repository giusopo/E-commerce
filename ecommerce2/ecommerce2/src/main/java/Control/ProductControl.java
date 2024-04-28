package Control;

import Model.ProductModel;
import Model.ProductModel_impl;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "Home", urlPatterns = "/")
public class ProductControl extends HttpServlet {

    static ProductModel model = new ProductModel_impl();

    public ProductControl() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        System.out.println("Inizio della richiesta doGet");

        String action = req.getParameter("action");

        try {

            if (action != null) {

                // visualizzare un singolo prodotto
                if (action.equalsIgnoreCase("read")) {

                    int id = Integer.parseInt(req.getParameter("ProductID"));
                    req.removeAttribute("product");
                    req.setAttribute("product", model.doRetrieveByKey(id));

                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/productView.jsp");
                    dispatcher.forward(req, resp);

                } else if (action.equalsIgnoreCase("delete")) {

                    // da implementare

                } else if (action.equalsIgnoreCase("insert")) {

                    // da implementare

                }
            } else {

                // mostra tutti i prodotti nel db
                req.removeAttribute("products");
                req.setAttribute("products", model.doRetrieveAll("ID"));

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

        System.out.println("Fine della richiesta doGet");

        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
        dispatcher.forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //doGet(req, resp);
    }
}

