package GestioneIMG;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/getPicture")
public class GetPicturesControl extends HttpServlet {
	private static final long serialVersionUID = 1L;

    public GetPicturesControl() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	int idProdotto;
    	try {
    		idProdotto = Integer.parseInt(request.getParameter("IDpic"));
    	} catch (NumberFormatException e) {
    		// id mancante o non numerico: richiesta malformata
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    		return;
    	}
    	String ruolo = request.getParameter("ruolo") == null ? "principale" : request.getParameter("ruolo");

    	byte[] immagine;
		try {
			immagine = IMGmodel.load(idProdotto, ruolo);
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		if (immagine == null) {
			// le pagine mostrano un'immagine di riserva quando questa non si carica
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// Il tipo va dichiarato prima di scrivere i byte, altrimenti la risposta può essere già inviata.
		response.setContentType(tipoImmagine(immagine));
		response.setContentLength(immagine.length);
		try (ServletOutputStream out = response.getOutputStream()) {
			out.write(immagine);
		}
	}

	/** image/png per i file che iniziano con la firma PNG, image/jpeg per gli altri. */
	static String tipoImmagine(byte[] immagine) {
		boolean png = immagine.length >= 4 && (immagine[0] & 0xFF) == 0x89 && immagine[1] == 'P'
				&& immagine[2] == 'N' && immagine[3] == 'G';
		return png ? "image/png" : "image/jpeg";
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
