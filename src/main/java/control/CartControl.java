package control;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Model.*;
import Model.Bean.CartBean;
import Model.Bean.OrdineBean;
import Model.Bean.ProductBean;
import Model.Interface.*;

public class CartControl extends HttpServlet {
    private static final long serialVersionUID = 1L;

    static ProductModel_intf productModel = new ProductModel();
    static CartModel_intf cartModel = new CartModel();
    static UserModel_intf userModel = new UserModel();

    public CartControl() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CartBean cart = (CartBean) request.getSession().getAttribute("cart");
        boolean login = false;

        int userID = -1;
        int cartID = -1;

        String username = (String) request.getSession().getAttribute("username");

        // Un intero non può essere null, darebbe un'eccezione
        Integer temp_userId = (Integer) request.getSession().getAttribute("userID");
        Integer temp_cartId = (Integer) request.getSession().getAttribute("cartID");

        if (temp_userId != null)
            userID = temp_userId.intValue();
        if (temp_cartId != null)
            cartID = temp_cartId.intValue();

        if (cart == null) {
            cart = new CartBean();
            request.getSession().setAttribute("cart", cart);
        }

        if (username != null && userID != -1) {
            login = true;

            try {
                cart.setProducts((ArrayList<ProductBean>) cartModel.getProducts(userID));
            } catch (SQLException e) {
                e.printStackTrace();
            }

            request.getSession().setAttribute("cart", cart);
        }

        String action = request.getParameter("action");
        if (action == null)
            action = "";

        // Le azioni sui prodotti e sugli ordini leggono un id numerico: se manca o non è un numero
        // la richiesta è malformata e viene respinta prima di modificare il carrello.
        try {
            eseguiAzione(action, request, response, cart, login, userID, cartID);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /** La quantità nel carrello può crescere solo se resta entro la giacenza del prodotto appena riletto. */
    private static boolean sottoGiacenza(CartBean cart, ProductBean prodotto) {
        int nelCarrello = Math.max(0, cart.getQuantitàOrdinata(prodotto));
        return nelCarrello < prodotto.getQTA_magazzino();
    }

    private void eseguiAzione(String action, HttpServletRequest request, HttpServletResponse response,
            CartBean cart, boolean login, int userID, int cartID) throws ServletException, IOException {

        if (action.equalsIgnoreCase("aggiungi")) {
            int id = Integer.parseInt(request.getParameter("id"));

            // Prelevo i dati del prodotto da inserire per poi verificare se già presente
            try {
                ProductBean prodotto = productModel.doRetrieveByKey(id);

                // Un prodotto che non esiste non entra nel carrello.
                if (prodotto.isEmpty()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                if (sottoGiacenza(cart, prodotto)) {
                    if (login) {
                        // true incrementa la quantità, false inserisce il prodotto
                        cartModel.addProduct(cartID, prodotto, cart.contiene(prodotto), 1);
                    } else {
                        if (!cart.contiene(prodotto))
                            cart.addProduct(prodotto);

                        cart.incrementaQuantitàOrdinata(prodotto);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            response.sendRedirect("/");
        } else if (action.equalsIgnoreCase("decrementaQTA")) {
            int id = Integer.parseInt(request.getParameter("id"));
            ProductBean prodotto;

            try {
                prodotto = productModel.doRetrieveByKey(id);

                if (login) {
                    if (cart.getQuantitàOrdinata(prodotto) <= 1)
                        cartModel.deleteProduct(cartID, prodotto, true);
                    else
                        cartModel.deleteProduct(cartID, prodotto, false);
                } else {
                    cart.rimuoviSingolaQuantitàOrdinata(prodotto);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            response.sendRedirect("/Cart?action=visualizza");


        } else if (action.equalsIgnoreCase("confermaOrdine")) {
            if (login) {
                int oldCartId = cartID;

                // Il pulsante di conferma compare solo con il carrello pieno, ma la richiesta
                // può arrivare anche direttamente: un carrello vuoto non diventa un ordine.
                if (cart.getProducts().isEmpty()) {
                    response.sendRedirect("/Cart?action=visualizza");
                    return;
                }

                try {
                    // L'id del nuovo carrello viene dalla stessa transazione che conferma l'ordine:
                    // se la conferma riesce, la sessione punta sempre al carrello giusto.
                    int nuovoCarrello = cartModel.confermaOrdine((String) request.getSession().getAttribute("indirizzo"), cartID, userID, cart.getProducts());

                    request.getSession().setAttribute("cart", new CartBean());
                    request.getSession().setAttribute("cartID", nuovoCarrello);
                } catch (SQLException e) {
                    // L'ordine non è stato confermato (anche per giacenza insufficiente): non va mostrato
                    // come se lo fosse.
                    e.printStackTrace();
                    response.sendRedirect("/Error.jsp");
                    return;
                }
                response.sendRedirect("/Cart?action=getOrdineStorico&id=" + oldCartId);
            }else {
                response.sendRedirect("/LoginRegistrazione.jsp");
            }
        } else if (action.equalsIgnoreCase("getOrdineStorico")) {
            if (login) {
                int idOrdine = Integer.parseInt(request.getParameter("id"));
                OrdineBean ordine;
                try {
                    ordine = userModel.getOrdineById(userID, idOrdine);
                } catch (SQLException e) {
                    e.printStackTrace();
                    response.sendRedirect("/Error.jsp");
                    return;
                }

                // Ordine inesistente, non concluso o di un altro utente: non c'è una fattura da mostrare.
                if (ordine.getIdOrdine() == 0) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                request.setAttribute("ordine", ordine);
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/ordineStorico.jsp");
                dispatcher.forward(request, response);
            } else {
                response.sendRedirect("/LoginRegistrazione.jsp");
            }
        }else if (action.equalsIgnoreCase("StoricoCompletoOrdini")) {
            if (login) {
                try {
                    request.setAttribute("ordini", userModel.getAllOrdine(userID));
                } catch (SQLException e) {
                    e.printStackTrace();
                    response.sendRedirect("/Error.jsp");
                    return;
                }

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/StoricoOrdini.jsp");
                dispatcher.forward(request, response);
            } else {
                response.sendRedirect("/LoginRegistrazione.jsp");
            }
            } else if (action.equalsIgnoreCase("aumentaQTA")) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                ProductBean prodotto = productModel.doRetrieveByKey(id);

                if (prodotto.isEmpty()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                if (sottoGiacenza(cart, prodotto)) {
                    if (login) {
                        cartModel.addProduct(cartID, prodotto, true, 1);
                    } else {
                        cart.incrementaQuantitàOrdinata(prodotto);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            response.sendRedirect("/Cart?action=visualizza");
        } else if (action.equalsIgnoreCase("elimina")) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                ProductBean prodotto = productModel.doRetrieveByKey(id);

                if (login)
                    cartModel.deleteProduct(cartID, prodotto, true);
                else
                    cart.deleteProduct(prodotto);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            request.getSession().setAttribute("cart", cart);
            request.setAttribute("cart", cart);

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Cart?action=visualizza");
            dispatcher.forward(request, response);
        } else {
            // "visualizza", e anche un'azione mancante o sconosciuta: si mostra il carrello.
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Cart.jsp");
            dispatcher.forward(request, response);
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
