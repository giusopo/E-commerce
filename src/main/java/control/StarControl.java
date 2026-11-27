package control;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.StringJoiner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Model.ProductModel;
import Model.Bean.ProductBean;

@WebServlet("/StarControl")
public class StarControl extends HttpServlet {
	private static final long serialVersionUID = 1L;

    private ProductModel model = new ProductModel();

    public StarControl() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Collection<ProductBean> products;

        try {
			products = model.doRetrieveAll("ID",0,9999);
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

        // Array JSON delle votazioni: "[]" se il catalogo è vuoto.
        StringJoiner productsJSON = new StringJoiner(",", "[", "]");
        for (ProductBean p : products) {
            productsJSON.add(p.StartoJSON());
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(productsJSON.toString());
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
