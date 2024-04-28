package Control;

import Model.Bean.UserBean;
import Model.UserModel;
import Model.UserModel_impl;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


@WebServlet(name = "User", urlPatterns = "/User")
public class UserControl extends HttpServlet {

    static UserModel model = new UserModel_impl();

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
                    throw new RuntimeException(e);
                }
                java.sql.Date dataNascitaConvertita = new java.sql.Date(parsed.getTime());

                user.setNome(nome);
                user.setCognome(cognome);
                user.setUserName(username);
                user.setDataNascita(dataNascitaConvertita);
                user.setEmail(email);
                user.setPassword(password);

                model.AddUser(user);

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

                UserBean user = model.getUserBy(username);

                // ricorda che va implementata la funzione di hashing per la password
                if (user != null && password.equals(user.getPassword())) {

                    // L'utente esiste e la password è corretta
                    // quindi setto i valori di sessione utili
                    session.setAttribute("username", user.getUserName());
                    session.setAttribute("UserID", user.getID());

                    // reindirizza l'utente alla pagina principale
                    response.sendRedirect("/");

                } else {
                    // L'utente non esiste o la password non è corretta
                    // mostra un messaggio di errore
                }

            }

            else if (action.equals("logout")) {

                // situazione in cui l'utente vuole sloggarsi
                // credo vada solo cancellata la sessione
            }

        } // non so se bisogna fare qualcosa se l'action è vuoto
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }
}
