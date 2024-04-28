package Control;

import Model.Bean.CartBean;
import Model.Bean.ProductBean;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "Cart", urlPatterns = "/Cart")
public class CartControl extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession();

        if (session.getAttribute("user") == null) {

            // L'utente non è loggato, reindirizza alla pagina di login
            response.sendRedirect("login.jsp");
            return;
        }

        String productId = request.getParameter("ProductID");

        // Trova il prodotto nel database...
        ProductBean product = null;

        try {
            product = ProductControl.model.doRetrieveByKey(Integer.parseInt(productId));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        CartBean cart = (CartBean) session.getAttribute("cart");

        if (cart == null) {

            // Se non esiste un carrello, creane uno nuovo
            cart = new CartBean();
            session.setAttribute("cart", cart);
        }

        // Aggiungi il prodotto al carrello
        cart.setProducts(product);

        response.sendRedirect("cart.jsp");
    }
}
