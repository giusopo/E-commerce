package admin;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import GestioneIMG.IMGmodel;
import Model.*;
import Model.Bean.ProductBean;
import Model.Interface.*;
/*
 * filtro
 * nuovi css
 * nuovi file
 * 
 * control e interfacce ProductControl e ProductModel,AdminModel e AdminControl
 * jsp home.jsp , cart.jsp
 * 
 * */
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 10,      // 10 MB
    maxRequestSize = 1024 * 1024 * 50    // 50 MB
)
public class AdminControl extends HttpServlet {
    static String SAVE_DIR = "/uploadTemp";
    private static final long serialVersionUID = 1L;

    static AzioniAdminModel_intf model = new AzioniAdminModel();
    static UserModel_intf userModel = new UserModel();
    static ProductModel_intf productModel = new ProductModel();
    public AdminControl() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        System.out.println("action vale : " + action);

        if (action != null) {
            if (action.equalsIgnoreCase("inserisciProdotto")) {
                boolean esito = false;
                String id = request.getParameter("ID");
                String nome = request.getParameter("nome");
                String descrizione = request.getParameter("descrizione");
                String prezzo = request.getParameter("prezzo");
                String grammatura = request.getParameter("grammatura");
                String categoria = request.getParameter("categoria");
                String QTAmagazzino = request.getParameter("QTAmagazzino");
                String percentualeSconto = request.getParameter("percentualeSconto");
                String colorazione = request.getParameter("colorazione");

                ProductBean p = new ProductBean();
                p.setID(Integer.parseInt(id));
                p.setNome(nome);
                p.setDescrizione(descrizione);
                p.setPrezzo(Double.parseDouble(prezzo));
                p.setGrammatura(Double.parseDouble(grammatura));
                p.setCategoria(categoria);
                p.setQTA_magazzino(Integer.parseInt(QTAmagazzino));
                p.setPercentualeSconto(Integer.parseInt(percentualeSconto));
                p.setColorazione(colorazione);
                p.setDataInserimento(Date.valueOf(LocalDate.now()));

                try {
                    esito = model.inserisciProdotto(p);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                if (esito) {
                    ArrayList<Part> parti = new ArrayList<>(request.getParts());
                    insImmagine(request.getServletContext().getRealPath(""), Integer.parseInt(id), "principale", parti.get(parti.size() - 3));
                    insImmagine(request.getServletContext().getRealPath(""), Integer.parseInt(id), "secondaria", parti.get(parti.size() - 2));

                    response.sendRedirect(request.getContextPath() + "/?action=read&ProductID=" + id);
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
                request.getSession().removeAttribute("Utenti");

                try {
                    request.getSession().setAttribute("listaUtenti", userModel.getUtenti());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                response.sendRedirect(request.getContextPath() + "/admin/visualizzaUtenti.jsp");

            } else if (action.equalsIgnoreCase("impostaSconto")) {
                int id = Integer.parseInt(request.getParameter("id"));
                int sconto = Integer.parseInt(request.getParameter("sconto"));

                try {
                    model.modificaSconto(id, sconto);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/admin/modificaProdotto.jsp");
                dispatcher.forward(request, response);
            } else {
                // Porta ad una pagina di errore
                response.sendRedirect(request.getContextPath() + "/error.jsp");
            }
        } else {
            // Porta ad una pagina di errore
            response.sendRedirect(request.getContextPath() + "/error.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    protected void insImmagine(String pathContext, int id, String ruolo, Part part) throws ServletException, IOException {
        String savePath = pathContext + File.separator + SAVE_DIR;

        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        String fileName = extractFileName(part);
        if (fileName != null && !fileName.equals("")) {
            part.write(savePath + File.separator + fileName);
            try {
                IMGmodel.updatePhoto(id, ruolo, savePath + File.separator + fileName);
            } catch (SQLException sqlException) {
                System.out.println(sqlException);
            }
        }
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }
}
