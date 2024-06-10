package control;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

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
	
        Collection<ProductBean> products = new LinkedList<ProductBean>(); 
        
        try {
			products = model.doRetrieveAll("ID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
        
        String productsJSON = new String();
        
        productsJSON = productsJSON.concat("[");
        
        for(ProductBean p : products) {
        
            productsJSON = productsJSON.concat(p.StartoJSON());

            productsJSON = productsJSON.concat(",");            
        }
        
        productsJSON = productsJSON.substring(0, productsJSON.length() - 1);
        
        productsJSON = productsJSON.concat("]");
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(productsJSON);
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
