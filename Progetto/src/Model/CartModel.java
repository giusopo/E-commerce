package Model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import Model.Bean.ImgBean;
import Model.Bean.ProductBean;
import Model.Interface.CartModel_intf;

public class CartModel implements CartModel_intf{

	private static DataSource ds;
    
	private static final String TABLE_ORDINE = "ordine";
	private static final String TABLE_PRODOTTO = "prodotto";
    private static final String TABLE_IMMAGINE = "immagine";
    private static final String TABLE_UTENTE = "utente";
    private static final String RELAZIONE = "contiene";



	static {
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");

			ds = (DataSource) envCtx.lookup("jdbc/ecommerce");

		} catch (NamingException e) {
			System.out.println("Error:" + e.getMessage());
		}
	}
	
	public CartModel() {
	}
	
	//giaPresente è un booleano che viene passato dal chiamante del carrello, 
	//una volta effettuato il login il carrello viene caricato in sessione , 
	// quindi non ha senso verificare con una query aggiuntiva siccome i dati li sappiamo già
	
	public synchronized void addProduct(int idOrdine,ProductBean prodotto, boolean giaPresente) throws SQLException {	

		Connection connection = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    
	    String querySQL;
	    
	    if(giaPresente) 
	    	querySQL = "UPDATE " + RELAZIONE + " SET `QTAordinata` = (SELECT QTAordinata FROM " + RELAZIONE 
	    			+ " WHERE " + RELAZIONE + ".`ID_ordine` = ? "
	    			+ " AND " + RELAZIONE + ".`ID_prodotto` = ?)+1 "
	    					+ "WHERE " + RELAZIONE + ".`ID_ordine` = ? AND " + RELAZIONE + ".`ID_prodotto` = ?;";
	    else 
			querySQL = "INSERT INTO `contiene` "
					+ "(`ID_ordine`, `ID_prodotto`, `QTAordinata`) "
					+ "VALUES (?, ?, ?)";
	    
        try {
			connection = ds.getConnection();
	        pstmt = connection.prepareStatement(querySQL);
	        
		    pstmt.setInt(1, idOrdine);
		    pstmt.setInt(2, prodotto.getID());

		    if(giaPresente) {
		    	pstmt.setInt(3, idOrdine);
		    	pstmt.setInt(4, prodotto.getID());
		    }
		    else
		    	pstmt.setInt(3, 1);
		    
			System.out.println(pstmt.toString());

		    pstmt.executeUpdate();
		    

        } finally {
	        try {
	            if (rs != null)
	                rs.close();
	        } finally {
	            try {
	                if (pstmt != null)
	                    pstmt.close();
	            } finally {
	                if (connection != null)
	                    connection.close();
	            }
	        }
	    }
	}
	
	
	public void deleteProduct(int idOrdine,ProductBean prodotto,boolean isUltimo) throws SQLException {

		String queryIdOrdine = "SELECT " + TABLE_ORDINE + ".ID AS idOrdine"
				+ " FROM " + TABLE_ORDINE +" "
				+ " JOIN " + TABLE_UTENTE + " ON " + TABLE_UTENTE + ".ID = " + TABLE_ORDINE + ".UTENTE_ID"
				+ " WHERE " + TABLE_ORDINE + ".STATO = 'in corso'"
						+ " AND " + TABLE_UTENTE + ".ID = ?";
	
		Connection connection = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    
	    String querySQL;
	    
	    if(isUltimo) 
			querySQL = "DELETE FROM " + RELAZIONE + " WHERE "+ RELAZIONE +".`ID_ordine` = ? AND "+ RELAZIONE +".`ID_prodotto` = ?";
	    else 
	    	querySQL = "UPDATE " + RELAZIONE + " SET `QTAordinata` = (SELECT QTAordinata FROM " + RELAZIONE 
			+ " WHERE " + RELAZIONE + ".`ID_ordine` = ? "
			+ " AND " + RELAZIONE + ".`ID_prodotto` = ?) - 1 "
					+ "WHERE " + RELAZIONE + ".`ID_ordine` = ? AND " + RELAZIONE + ".`ID_prodotto` = ?;";
	    
        try {
			connection = ds.getConnection();
	        pstmt = connection.prepareStatement(querySQL);
		    
	        if(isUltimo) {
			    pstmt.setInt(1, idOrdine);
			    pstmt.setInt(2, prodotto.getID());
		    }
		    else {
			    pstmt.setInt(1, idOrdine);
			    pstmt.setInt(2, prodotto.getID());		 
			    pstmt.setInt(3, idOrdine);
			    pstmt.setInt(4, prodotto.getID());		
		    }
		    
			System.out.println(pstmt.toString());

		    pstmt.executeUpdate();
		    

        } finally {
	        try {
	            if (rs != null)
	                rs.close();
	        } finally {
	            try {
	                if (pstmt != null)
	                    pstmt.close();
	            } finally {
	                if (connection != null)
	                    connection.close();
	            }
	        }
	    }
	}
	
	public ArrayList<ProductBean> getProducts(int id) throws SQLException {

	    String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*, " + RELAZIONE + ".QTAordinata ," + TABLE_IMMAGINE + ".immagine " +
	            " FROM " + TABLE_UTENTE +
	            " JOIN " + TABLE_ORDINE + " ON " + TABLE_ORDINE + ".UTENTE_ID = " + TABLE_UTENTE + ".ID" +
	            " JOIN " + RELAZIONE + " ON " + RELAZIONE + ".ID_ordine = " + TABLE_ORDINE + ".ID" +
	            " JOIN " + TABLE_PRODOTTO + " ON " + TABLE_PRODOTTO + ".ID = " + RELAZIONE + ".ID_prodotto" +
	            " JOIN " + TABLE_IMMAGINE + " ON " + TABLE_IMMAGINE + ".prodotto_ID = " + TABLE_PRODOTTO + ".ID" +
	            " WHERE " + TABLE_ORDINE + ".stato = 'in corso' AND " + TABLE_UTENTE + ".ID = ?";
	    
	    ArrayList<ProductBean> products = new ArrayList<>();
	    Connection connection = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;

	    try {
	        connection = ds.getConnection();
	        pstmt = connection.prepareStatement(selectSQL);
		    pstmt.setInt(1, id);

	        rs = pstmt.executeQuery();

	        while (rs.next()) {
	            ProductBean bean = new ProductBean();
	            ImgBean img = new ImgBean();
	            ArrayList<ImgBean> images = new ArrayList<>();

	            bean.setID(rs.getInt("ID"));
	            bean.setNome(rs.getString("nome"));
	            bean.setDescrizione(rs.getString("descrizione"));
	            bean.setPrezzo(rs.getDouble("prezzo"));
	            bean.setGrammatura(rs.getDouble("grammatura"));
	            bean.setCategoria(rs.getString("categoria"));
	            bean.setQTA_magazzino(rs.getInt("QTAmagazzino"));
	            bean.setPercentualeSconto(rs.getInt("percentualeSconto"));
	            bean.setColorazione(rs.getString("colorazione"));
	            bean.setQTA_ordinata(rs.getInt("QTAordinata"));

	            img.setImg(rs.getBlob("immagine"));
	            images.add(img);

	            bean.setImages(images);
	            products.add(bean);
	        }
	    } finally {
	        try {
	            if (rs != null)
	                rs.close();
	        } finally {
	            try {
	                if (pstmt != null)
	                    pstmt.close();
	            } finally {
	                if (connection != null)
	                    connection.close();
	            }
	        }
	    }
	    System.out.println("CARRELLO RECUPERATO , GRANDEZZA : " + products.size());
	    return products;
	}
	

}
