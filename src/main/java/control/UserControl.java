package control;

import Model.Bean.CartBean;
import Model.Bean.ProductBean;
import Model.Bean.UserBean;
import Model.CartModel;
import Model.UserModel;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;


//@WebServlet(name = "User", urlPatterns = "/User")
public class UserControl extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static UserModel model = new UserModel();
	static CartModel cartModel = new CartModel();


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter("action");
        HttpSession session = request.getSession();

        if (action != null) {

            if (action.equals("register")) {
            	int cartId = -1;
                UserBean user = new UserBean();

                String nome = request.getParameter("nome");
                String cognome = request.getParameter("cognome");
                String username = request.getParameter("new_username");
                String phoneNumber = request.getParameter("phoneNumber");
                String dataNascita = request.getParameter("dataNascita");
                String email = request.getParameter("email");
                String indirizzo = request.getParameter("indirizzo");
                // la password arriva in chiaro: viene controllata così e salvata solo come hash SHA-256 (hashPassword)
                String password = request.getParameter("new_password");

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                // I controlli del form vanno ripetuti sul server: una richiesta HTTP diretta
                // salta il JavaScript. La password si controlla in chiaro, prima dell'hash.
                if (!datiRegistrazioneValidi(nome, cognome, username, phoneNumber, dataNascita, email, indirizzo, password)) {
                    System.out.println("utente inserito non valido");
                    response.getWriter().write("{\"val\": \"0\"}");
                    return;
                }

                user.setNome(nome);
                user.setCognome(cognome);
                user.setUserName(username);
                user.setPhoneNumber(phoneNumber);
                // la data è già stata controllata da birthDateIsValid: è nel formato aaaa-mm-gg
                user.setDataNascita(java.sql.Date.valueOf(LocalDate.parse(dataNascita)));
                user.setEmail(email);
                user.setIndirizzoBase(indirizzo);
                user.setPassword(hashPassword(password));

                try {
                	model.AddUser(user);
                } catch (SQLException e) {
                	// utente non salvato (es. username già esistente): la registrazione non è riuscita
                	e.printStackTrace();
                	response.getWriter().write("{\"val\": \"0\"}");
                	return;
                }

       		 	// controllo se ci sono carrelli pieni in sessione
            	CartBean cart = (CartBean) request.getSession().getAttribute("cart");
            	System.out.println("il carrello è " + cart);

            	if(cart != null) {
                	List<ProductBean> prodottiCarrello = cart.getProducts();
                	try {
						cartId = model.getIdCart(username, hashPassword(password));
					} catch (SQLException e) {
						e.printStackTrace();
					}
                	if(prodottiCarrello.size() > 0 && cartId != -1)
                		for(ProductBean p : prodottiCarrello)
							try {
								cartModel.addProduct(cartId, p, false,p.getQTA_ordinata());
							} catch (SQLException e) {
								e.printStackTrace();
							}
            	}
        		response.getWriter().write("{\"val\": \"1\"}");
            }

            else if (action.equals("login")) {
            	// Se c'era già un utente la sua sessione si invalida, così i dati del nuovo login non si
            	// mescolano con i suoi. Se invece c'era un ospite, il suo carrello si unisce a quello salvato.
            	CartBean carrelloOspite = null;
            	if(!(session.getAttribute("userID") == null)) {
            		session.invalidate();
            		session = request.getSession();
            	} else {
            		carrelloOspite = (CartBean) session.getAttribute("cart");
            	}

                // USER_NAME è UNIQUE nello schema (ecommerce.sql): username e password identificano al più un utente.
                // L'email non è unica: due utenti possono avere la stessa.

                String username = request.getParameter("username");
                String password = request.getParameter("password");

                if(username != null && password != null && this.isAlphanumeric(username) && this.passwordIsValid(password)) {

                	UserBean user;

                	try {
    					user = model.getUserTramiteCredenziali(username,hashPassword(password));

    					response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");

    					// nel database c'è solo l'hash: si confronta con l'hash della password inserita
    	                if (user != null && hashPassword(password).equals(user.getPassword())) {

    	                	// Nuovo identificativo di sessione: chi conosceva quello dell'ospite non diventa
    	                	// l'utente appena entrato.
    	                	request.changeSessionId();

    	                    // L'utente esiste e la password è corretta
    	                    // quindi setto i valori di sessione utili
    	                    session.setAttribute("username", user.getUserName());
    	                    session.setAttribute("userID", user.getID());
    	                    session.setAttribute("cartID", user.getCartID());
							session.setAttribute("indirizzo", user.getIndirizzoBase());

    	                    // distingue se admin o meno con valore di sessione
    	                    if(user.getPermessiAdmin()) {
    	        				request.getSession().setAttribute("adminRoles", true);
    	                    }
    	                    else {
    	        				request.getSession().setAttribute("adminRoles", false);
    	                    }

    	                    unisciCarrelloOspite(carrelloOspite, user);
    	                    // Il carrello in sessione ora è quello salvato: CartControl lo ricarica dal database.
    	                    session.removeAttribute("cart");

	                    	response.getWriter().write("{\"val\":\"1\"}");
    	                }else {
	                		response.getWriter().write("{\"val\":\"0\"}");
    	                }
    				} catch (Exception e) {
    					System.out.println("exception");
    					e.printStackTrace();
    			        response.sendRedirect(request.getContextPath() + "/Error.jsp");
    				}
                } else {
                	// gli input non sono conformi alle regole
                	RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/LoginRegistrazione.jsp");
                    dispatcher.forward(request, response);
                }
            }
            else if (action.equals("DatiUtente")) {
                UserBean user;
            	Integer ID;

            	ID = (Integer) request.getSession().getAttribute("userID");

            	//contollo che effettivamente sia loggato (prima di usare ID, che senza login è null)
            	if(!(ID == null)) {
            		System.out.println("Id recuperato: " + ID);
            		try {
						user = model.getUserTramiteID(ID.intValue());
					} catch (SQLException e) {
						e.printStackTrace();
						response.sendRedirect(request.getContextPath() + "/Error.jsp");
						return;
					}
					request.setAttribute("datiUtente", user);
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/AreaUtente.jsp");
                    dispatcher.forward(request, response);
            	} else {
            		// senza login non ci sono dati da mostrare: si torna alla pagina di accesso
            		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/LoginRegistrazione.jsp");
            		dispatcher.forward(request, response);
            	}

            }
            else if (action.equals("logout")) {

                // il logout cancella la sessione, e con lei utente e carrello
            	request.getSession().invalidate();
            	System.out.println("Sessione invalidata");

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/LoginRegistrazione.jsp");
                dispatcher.forward(request, response);
            }
	         else {
	            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
	            dispatcher.forward(request, response);
            }
        } // senza azione non c'è niente da fare
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

	// Copia nel carrello salvato i prodotti scelti da ospite: se un prodotto c'è già, le quantità si sommano.
	// Un prodotto non copiato non annulla il login: l'errore viene stampato, come nella registrazione.
	private void unisciCarrelloOspite(CartBean ospite, UserBean user) {
		if (ospite == null || ospite.getProducts().isEmpty()) {
			return;
		}
		CartBean salvato = new CartBean();
		try {
			salvato.setProducts(cartModel.getProducts(user.getID()));
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		for (ProductBean p : ospite.getProducts()) {
			try {
				cartModel.addProduct(user.getCartID(), p, salvato.contiene(p), p.getQTA_ordinata());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// Stesse regole del form di registrazione (FormValidation.js): campi obbligatori, nome e cognome
	// solo lettere, username alfanumerico, telefono, data di nascita, email e password validi.
	// L'indirizzo è solo obbligatorio: nel browser è composto da regione, provincia e comune.
	private boolean datiRegistrazioneValidi(String nome, String cognome, String username, String telefono,
			String dataNascita, String email, String indirizzo, String password) {

		if (vuoto(nome) || vuoto(cognome) || vuoto(username) || vuoto(telefono) || vuoto(dataNascita)
				|| vuoto(email) || vuoto(indirizzo) || vuoto(password)) {
			return false;
		}

		return isAllLetter(nome) && isAllLetter(cognome) && isAlphanumeric(username)
				&& phoneNumberIsValid(telefono) && birthDateIsValid(dataNascita)
				&& emailIsValid(email) && passwordIsValid(password);
	}

	private static boolean vuoto(String valore) {
		return valore == null || valore.trim().isEmpty();
	}

	private boolean passwordIsValid(String password) {
        // Controlla se la password ha almeno 8 caratteri e non più di 20
        boolean lengthCheck = password.length() >= 8 && password.length() <= 20;

        // Controlla se la password contiene almeno una lettera maiuscola
        boolean hasUppercase = Pattern.matches(".*[A-Z].*", password);

        // Controlla se la password contiene almeno una lettera minuscola
        boolean hasLowercase = Pattern.matches(".*[a-z].*", password);

        // Controlla se la password contiene almeno un numero
        boolean hasNumber = Pattern.matches(".*[0-9].*", password);

        // Controlla se la password contiene almeno un carattere speciale
        boolean hasSpecialChar = Pattern.matches(".*[^a-zA-Z0-9 ].*", password);

        // Restituisce true se la password soddisfa tutti i criteri
        return lengthCheck && hasUppercase && hasLowercase && hasNumber && hasSpecialChar;
    }

 	private boolean isAllLetter(String inputtxt) {
        return Pattern.matches("^[A-Za-z]*$", inputtxt);
    }

 	private boolean isAlphanumeric(String inputtxt) {
        return Pattern.matches("^[0-9a-zA-Z]*$", inputtxt);
    }

 	private boolean emailIsValid(String inputtxt) {
        return Pattern.matches("^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$", inputtxt);
    }

 	private boolean phoneNumberIsValid(String inputtxt) {
        return Pattern.matches("^\\+?([0-9]{2})\\)?[-. ]?([0-9]{4})[-. ]?([0-9]{4})$", inputtxt);
    }

 	// La data deve essere una data esistente nel formato aaaa-mm-gg, senza altro testo, non futura e più
 	// recente di 100 anni fa: oggi è accettata, esattamente 100 anni fa no.
 	private boolean birthDateIsValid(String birthDate) {
        try {
            LocalDate nascita = LocalDate.parse(birthDate);
            LocalDate oggi = LocalDate.now();

            return !nascita.isAfter(oggi) && nascita.isAfter(oggi.minusYears(100));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

 	private String hashPassword(String password) {
        String hashedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            hashedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashedPassword;
    }
}
