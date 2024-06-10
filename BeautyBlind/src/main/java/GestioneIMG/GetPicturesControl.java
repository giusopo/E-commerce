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
		
    	String id = (String) request.getParameter("IDpic");
    	String ruolo = (String) request.getParameter("ruolo");

    	ServletOutputStream out;
    	byte[] bt;
    	
		if (id != null){
			try {
				bt = IMGmodel.load(id,ruolo);
				out = response.getOutputStream();
				
				if(bt != null) {
					out.write(bt);
					response.setContentType("image/jpeg");			
				}
				out.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
