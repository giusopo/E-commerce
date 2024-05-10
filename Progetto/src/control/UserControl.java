package control;


import Model.Bean.UserBean;
import Model.UserModel;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


//@WebServlet(name = "User", urlPatterns = "/User")
public class UserControl extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	static UserModel model = new UserModel();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter("action");
        HttpSession session = request.getSession();

        if (action != null) {

            if (action.equals("register")) {

                UserBean user = new UserBean();

                String nome = request.getParameter("nome");
                String cognome = request.getParameter("cognome");
                String username = request.getParameter("new_username");
                String dataNascita = request.getParameter("dataNascita");
                String email = request.getParameter("email");

                // già qua credo che la passwoed vada crittografata con una funzione di hash
                String password = request.getParameter("new_password");

                // la data di nascita viene passata come stringa e
                // va convertita in formato Date della libreria sql

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date parsed = null;

                try {
					parsed = format.parse(dataNascita);
				} catch (ParseException e) {
					e.printStackTrace();
				}
     
                java.sql.Date dataNascitaConvertita = new java.sql.Date(parsed.getTime());

                user.setNome(nome);
                user.setCognome(cognome);
                user.setUserName(username);
                user.setDataNascita(dataNascitaConvertita);
                user.setEmail(email);
                user.setPassword(password);

					try {
						model.AddUser(user);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                // una volta registrato l'utente in qualche modo
                // bisogna dirgli che deve effettuare il login

            }

            else if (action.equals("login")) {

                // Applica i dovuti controlli per verificare se l'utente è già loggato

                // Crea un pulsante per sloggarsi che è mostrato solo quando si è loggati

                // ricordati di impostare
                // (username) UNIQUE
                // (email) UNIQUE

                String username = request.getParameter("username");
                String password = request.getParameter("password");

                UserBean user;
					
                try {
					user = model.getUserTramiteCredenziali(username,password);

	                // ricorda che va implementata la funzione di hashing per la password
	                if (user != null && password.equals(user.getPassword())) {
	
	                    // L'utente esiste e la password è corretta
	                    // quindi setto i valori di sessione utili
	                    session.setAttribute("username", user.getUserName());
	                    session.setAttribute("userID", user.getID());
	                    session.setAttribute("cartID", user.getCartID());
		
	                } else {
	                    // L'utente non esiste o la password non è corretta
	                    // mostra un messaggio di errore
	                }
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

            }else if (action.equals("logout")) {

                // situazione in cui l'utente vuole sloggarsi
                // credo vada solo cancellata la sessione
            }

        } // non so se bisogna fare qualcosa se l'action è vuoto
        
        // reindirizza l'utente alla pagina principale
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }
}
