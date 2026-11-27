package admin;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import GestioneIMG.IMGmodel;
import Model.*;
import Model.Bean.ProductBean;
import Model.Interface.*;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 10,      // 10 MB
    maxRequestSize = 1024 * 1024 * 50    // 50 MB
)
public class AdminControl extends HttpServlet {
    static String SAVE_DIR = "/uploadTemp";
    private static final long serialVersionUID = 1L;
    /** Pagina di errore dell'applicazione (il nome del file distingue le maiuscole). */
    private static final String PAGINA_ERRORE = "/Error.jsp";
    /** Unità di misura ammesse per la grammatura (colonna unita, varchar(2)). */
    private static final Set<String> UNITA = Set.of("g", "ml");

    static AzioniAdminModel_intf model = new AzioniAdminModel();
    static UserModel_intf userModel = new UserModel();
    static ProductModel_intf productModel = new ProductModel();
    public AdminControl() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Difesa in profondità: anche se AuthFilter non fosse applicato, solo un admin esegue le azioni.
        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute("adminRoles"))) {
            response.sendRedirect(request.getContextPath() + "/LoginRegistrazione.jsp");
            return;
        }

        String action = request.getParameter("action");
        System.out.println("action vale : " + action);

        if (action != null) {
            if (action.equalsIgnoreCase("inserisciProdotto")) {
                // I controlli del form vanno ripetuti sul server: una richiesta diretta salta quelli del browser.
                ProductBean p = prodottoDalForm(request);
                boolean esito = false;

                if (p != null) {
                    try {
                        esito = model.inserisciProdotto(p);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                if (esito) {
                    // l'id è quello generato dal database, scritto nel bean da inserisciProdotto
                    String percorso = request.getServletContext().getRealPath("");
                    insImmagine(percorso, p.getID(), "principale", request.getPart("immagine1"));
                    insImmagine(percorso, p.getID(), "secondaria", request.getPart("immagine2"));

                    response.sendRedirect(request.getContextPath() + "/?action=read&ProductID=" + p.getID());
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/InserisciProdotto.jsp");
                }
            }
            else if (action.equalsIgnoreCase("readallEditPrice")) {
            	request.getSession().removeAttribute("products");
            	try {
					request.getSession().setAttribute("products", productModel.doRetrieveByCat("all", "ID"));
				} catch (SQLException e) {
					e.printStackTrace();
				}
                response.sendRedirect(request.getContextPath() + "/admin/modificaProdotto.jsp");

            } else if (action.equalsIgnoreCase("visualizzaUtenti")) {
                request.getSession().removeAttribute("listaUtenti");

                try {
                    request.getSession().setAttribute("listaUtenti", userModel.getUtenti());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                response.sendRedirect(request.getContextPath() + "/admin/visualizzaUtenti.jsp");

            } else if (action.equalsIgnoreCase("impostaSconto")) {
                int id;
                int sconto;
                try {
                    id = Integer.parseInt(request.getParameter("id"));
                    sconto = Integer.parseInt(request.getParameter("sconto"));
                } catch (NumberFormatException e) {
                    response.sendRedirect(request.getContextPath() + PAGINA_ERRORE);
                    return;
                }

                // Uno sconto fuori da 0-100 non viene salvato, come uno non numerico.
                boolean salvato = false;
                if (scontoValido(sconto)) {
                    try {
                        salvato = model.modificaSconto(id, sconto);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                // Prodotto inesistente, sconto non valido o errore del database: lo sconto non è stato
                // salvato e non va mostrato come se lo fosse.
                if (!salvato) {
                    response.sendRedirect(request.getContextPath() + PAGINA_ERRORE);
                    return;
                }

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/admin/modificaProdotto.jsp");
                dispatcher.forward(request, response);
            } else {
                // Azione sconosciuta: pagina di errore
                response.sendRedirect(request.getContextPath() + PAGINA_ERRORE);
            }
        } else {
            // Azione mancante: pagina di errore
            response.sendRedirect(request.getContextPath() + PAGINA_ERRORE);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    /** Uno sconto è una percentuale intera da 0 a 100. */
    static boolean scontoValido(int sconto) {
        return sconto >= 0 && sconto <= 100;
    }

    /**
     * Il prodotto descritto dal form di inserimento, con la data di oggi, oppure null se un campo
     * obbligatorio manca o un valore non è valido: nome, descrizione, categoria e colorazione non vuoti;
     * prezzo e grammatura numeri maggiori di 0; giacenza intera non negativa; sconto intero da 0 a 100
     * (0 se manca); unità "g" o "ml". La sottocategoria è facoltativa.
     */
    static ProductBean prodottoDalForm(HttpServletRequest request) {
        String nome = testo(request, "nome");
        String descrizione = testo(request, "descrizione");
        String categoria = testo(request, "categoria");
        String colorazione = testo(request, "colorazione");
        String unita = testo(request, "unita");
        String sottocategoria = testo(request, "subCategoria");
        String sconto = testo(request, "percentualeSconto");

        if (nome.isEmpty() || descrizione.isEmpty() || categoria.isEmpty() || colorazione.isEmpty()
                || !UNITA.contains(unita)) {
            return null;
        }

        ProductBean p = new ProductBean();
        try {
            p.setPrezzo(Double.parseDouble(testo(request, "prezzo")));
            p.setGrammatura(Double.parseDouble(testo(request, "grammatura")));
            p.setQTA_magazzino(Integer.parseInt(testo(request, "QTAmagazzino")));
            p.setPercentualeSconto(sconto.isEmpty() ? 0 : Integer.parseInt(sconto));
        } catch (NumberFormatException e) {
            return null;
        }

        boolean valido = Double.isFinite(p.getPrezzo()) && p.getPrezzo() > 0
                && Double.isFinite(p.getGrammatura()) && p.getGrammatura() > 0
                && p.getQTA_magazzino() >= 0 && scontoValido(p.getPercentualeSconto());
        if (!valido) {
            return null;
        }

        p.setNome(nome);
        p.setDescrizione(descrizione);
        p.setCategoria(categoria);
        p.setSubCategoria(sottocategoria);
        p.setColorazione(colorazione);
        p.setUnita(unita);
        p.setDataInserimento(Date.valueOf(LocalDate.now()));
        return p;
    }

    /** Il parametro senza spazi iniziali e finali; "" se manca. */
    private static String testo(HttpServletRequest request, String nome) {
        String valore = request.getParameter(nome);
        return valore == null ? "" : valore.trim();
    }

    protected void insImmagine(String pathContext, int id, String ruolo, Part part) throws ServletException, IOException {
        String savePath = pathContext + File.separator + SAVE_DIR;

        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        // Solo il nome del file, senza eventuali cartelle indicate dal browser.
        String fileName = part == null || part.getSubmittedFileName() == null
                ? "" : new File(part.getSubmittedFileName()).getName();
        if (!fileName.isEmpty()) {
            part.write(savePath + File.separator + fileName);
            try {
                IMGmodel.updatePhoto(id, ruolo, savePath + File.separator + fileName);
            } catch (SQLException sqlException) {
                // Il prodotto c'è ma l'immagine no: l'admin deve saperlo.
                throw new ServletException("Immagine " + ruolo + " del prodotto " + id + " non salvata", sqlException);
            }
        }
    }
}
