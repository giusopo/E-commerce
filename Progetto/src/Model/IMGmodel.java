package Model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class IMGmodel {
	
	private static DataSource ds;
	
    private static Connection connection = null;
    private static PreparedStatement pstmt = null;
    
    static {
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");

			ds = (DataSource) envCtx.lookup("jdbc/ecommerce");

		} catch (NamingException e) {
			System.out.println("Error:" + e.getMessage());
		}
	}

	public synchronized static byte[] load(String id) throws SQLException {

		
		ResultSet rs = null;

		byte[] bt = null;

		try {
			connection = ds.getConnection();
			String sql = "SELECT IMMAGINE FROM immagine WHERE PRODOTTO_ID = " + id;
			pstmt = connection.prepareStatement(sql);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				bt = rs.getBytes("IMMAGINE");
			}

		} catch (SQLException sqlException) {
			System.out.println(sqlException);
		} 
			finally {
			try {
				if (pstmt != null)
					pstmt.close();
			} catch (SQLException sqlException) {
				System.out.println(sqlException);
			} finally {
				if (connection != null) 
					connection.close();
			}
		}
		return bt;
	}
}
