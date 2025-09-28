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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
                // già qua credo che la passwoed vada crittografata con una funzione di hash
                String password = request.getParameter("new_password");
                boolean valid = false;


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

                ////////////////////
                user.setNome(nome);
                user.setCognome(cognome);
                user.setUserName(username);
                user.setPhoneNumber(phoneNumber);
                user.setDataNascita(dataNascitaConvertita);
                user.setEmail(email);
                user.setIndirizzoBase(indirizzo);
                user.setPassword(hashPassword(password));
                ////////////////////////////
                System.out.println(hashPassword(password)); 
                
                // controlla se l'utente è valido
                valid = Validate(user);
                
                response.setContentType("application/json");
            	response.setCharacterEncoding("UTF-8");
            	
                if(valid) {
                	// se valido lo registro nel db
                	try {
                		model.AddUser(user);
                		
                	} catch (SQLException e) {
                		e.printStackTrace();
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

                } else {
                	
                	System.out.println("utente inserito non valido");
                	
                	// Se i dati non sono validi, invia un messaggio di errore come risposta alla richiesta AJAX
            		response.getWriter().write("{\"val\": \"0\"}");
                    return;
                }

            }

            else if (action.equals("login")) {
            	//per sicurezza , così se per situazioni losche qualcuno provi a forzare
            	//il login senza prima eseguire il logout
            	if(!(request.getSession().getAttribute("userID") == null))
            		request.getSession().invalidate();

                // Applica i dovuti controlli per verificare se l'utente è già loggato

                // Crea un pulsante per sloggarsi che è mostrato solo quando si è loggati
            		//aggiornalmento : lo gestisco nella jsp
            	
                // ricordati di impostare
                // (username) UNIQUE
                // (email) UNIQUEs

                String username = request.getParameter("username");
                String password = request.getParameter("password");
                
                
if(this.isAlphanumeric(username) && this.passwordIsValid(password)) {
                	
                	UserBean user;
                	
                	try {
    					user = model.getUserTramiteCredenziali(username,hashPassword(password));
    					System.out.println(password + " -> " + hashPassword(password));
    					
    					response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");

    					// ricorda che va implementata la funzione di hashing per la password
    	                if (user != null && hashPassword(password).equals(user.getPassword())) {
    	
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
	                    	response.getWriter().write("{\"val\":\"1\"}");
    	                }else {
	                		response.getWriter().write("{\"val\":\"0\"}");
    	                }
    				} catch (Exception e) {
    					System.out.println("exception");
    			        request.getSession().setAttribute("errorMessage", e.getMessage());
    			        response.sendRedirect("Error.jsp");
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
            	System.out.println("Id recuperato (int,Integer): " + ID.intValue()+ " " + ID);
            	
            	//contollo che effettivamente sia loggato
            	if(!(ID == null)) {
            		try {
						user = model.getUserTramiteID(ID.intValue());
						
						request.setAttribute("datiUtente", user);
	                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/AreaUtente.jsp");
	                    dispatcher.forward(request, response);
	                    
					} catch (SQLException e) {
						e.printStackTrace();
					}
            	}
            	
            }
            else if (action.equals("logout")) {

                // situazione in cui l'utente vuole sloggarsi
                // credo vada solo cancellata la sessione
            	
            	request.getSession().invalidate();
            	System.out.println("Sessione invalidata");
            	
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/LoginRegistrazione.jsp");
                dispatcher.forward(request, response);
            }
	         else {
	            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/Home.jsp");
	            dispatcher.forward(request, response);
            }
        } // non so se bisogna fare qualcosa se l'action è vuoto
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    	// forse non va fatto nulla?
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
 	
 	private boolean addressIsValid(String inputtxt) {
        return Pattern.matches("^[0-9a-zA-Z ,]+$", inputtxt);
    }

 	private boolean birthDateIsValid(String birthDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date birthDateObj = sdf.parse(birthDate);

            Calendar hundredYearsAgo = Calendar.getInstance();
            hundredYearsAgo.add(Calendar.YEAR, -100);

            Date today = new Date();

            return birthDateObj.after(hundredYearsAgo.getTime()) && birthDateObj.before(today);
        } catch (Exception e) {
            return false;
        }
    }
	
 	private boolean emptyUser(UserBean user) {
		
		// controllo se l'oggetto ha campi vuoti
        
        if (user.getNome() == null || user.getNome().equals("") || user.getCognome() == null || user.getCognome().equals("") || 
        	user.getUserName() == null || user.getUserName().equals("") || user.getDataNascita() == null || 
        	user.getDataNascita().toString().equals("") || user.getEmail() == null || user.getEmail().equals("") || 
        	user.getPassword() == null || user.getPassword().equals("") || user.getPhoneNumber() == null || user.getPhoneNumber().equals("") ||
        	user.getIndirizzoBase() == null || user.getIndirizzoBase().equals("")) {
        	
            return true;
        }
        return false;
	}
 	
 	private boolean Validate(UserBean user) {
		
		// controllo se l'oggetto ha campi vuoti
        
        if (user.getNome() == null || user.getNome().equals("") || user.getCognome() == null || user.getCognome().equals("") || 
        	user.getUserName() == null || user.getUserName().equals("") || user.getDataNascita() == null || 
        	user.getDataNascita().toString().equals("") || user.getEmail() == null || user.getEmail().equals("") || 
        	user.getPassword() == null || user.getPassword().equals("")) {
        	
            return false;
        }
        
        return true;
	}
 	private String hashPassword(String password) {
        String hashedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
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


