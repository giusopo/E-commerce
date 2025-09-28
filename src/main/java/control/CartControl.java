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
    
    //static CartModel_intf cartModel = new CartModel();
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
        // System.out.println("Il carrello nella sessione è grande " + ((CartBean) request.getSession().getAttribute("cart")).getProducts().size());
        
        if (action.equalsIgnoreCase("visualizza")) {
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Cart.jsp");
            dispatcher.forward(request, response);
        } else if (action.equalsIgnoreCase("aggiungi")) {
            int id = Integer.parseInt(request.getParameter("id"));
            ProductBean prodotto;
            
            // Prelevo i dati del prodotto da inserire per poi verificare se già presente
            try {
                prodotto = productModel.doRetrieveByKey(id);

                if (login) {
                    // true inserisce, false incrementa la quantità
                    if (cart.getProducts().contains(prodotto)) 
                        cartModel.addProduct(cartID, prodotto, true, 1); 
                    else 
                        cartModel.addProduct(cartID, prodotto, false, 1);                        
                } else {
                    if (!cart.getProducts().contains(prodotto)) 
                        cart.addProduct(prodotto);

                    cart.incrementaQuantitàOrdinata(prodotto);
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
                
                try {
                    cartModel.confermaOrdine((String) request.getSession().getAttribute("indirizzo"), cartID, userID, cart.getProducts());
                    request.removeAttribute("cart");
                    
                    cart = new CartBean();            
                    request.getSession().setAttribute("cart", cart);
                    
                    request.getSession().setAttribute("cartID", userModel.getIdCartByUserId(userID));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                response.sendRedirect("/Cart?action=getOrdineStorico&id=" + oldCartId);
            }else {
                response.sendRedirect("/LoginRegistrazione.jsp");
            }
        } else if (action.equalsIgnoreCase("getOrdineStorico")) {
            if (login) {
                try {
                	Integer idOrdine_temp = Integer.parseInt(request.getParameter("id"));
                	int idOrdine = -1;
                	
                	System.out.println(idOrdine_temp.intValue());
                	if(idOrdine_temp != null)
                    	idOrdine = idOrdine_temp.intValue();
                	else
                		System.out.println("Vale null");

                    OrdineBean b = userModel.getOrdineById(userID, idOrdine);
                    request.setAttribute("ordine", b);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
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
                
                if (login) {
                    cartModel.addProduct(cartID, prodotto, true, 1);
                } else {
                    prodotto = productModel.doRetrieveByKey(id);
                    cart.incrementaQuantitàOrdinata(prodotto);
                }   
            } catch (SQLException e) {
                e.printStackTrace();
            }
            response.sendRedirect("/Cart?action=visualizza");

            //RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Cart?action=visualizza");
            //dispatcher.forward(request, response);
        } else if (action.equalsIgnoreCase("elimina")) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                ProductBean prodotto = productModel.doRetrieveByKey(id);

                if (login) 
                    cartModel.deleteProduct(cartID, prodotto, true);
                else {
                    cart.deleteProduct(productModel.doRetrieveByKey(id));
                    System.out.println("ELIMINATO : " + productModel.doRetrieveByKey(id));
                }    
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            request.getSession().setAttribute("cart", cart);
            request.setAttribute("cart", cart);
            
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Cart?action=visualizza");
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
