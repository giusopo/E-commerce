package Model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import Model.Bean.RecensioneBean;
import Model.Interface.RecensioneModel_intf;


public class RecensioneModel implements RecensioneModel_intf {

	private static DataSource ds;
	
    private Connection connection = null;
    private PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    static {
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");

			ds = (DataSource) envCtx.lookup("jdbc/ecommerce");

		} catch (NamingException e) {
			System.out.println("Error:" + e.getMessage());
		}
	}
    
    public RecensioneModel() {}
    
    public synchronized ArrayList<RecensioneBean> getAllRecensioni(int prodID) throws SQLException {
    	
    	String querySQL = "SELECT * FROM recensione WHERE PRODOTTO_ID = ?";    
    	ArrayList<RecensioneBean> listaRecensioni = new ArrayList<RecensioneBean>();
    	RecensioneBean recensione = new RecensioneBean();
    	
        try {
            connection = ds.getConnection();

            if (connection != null) {
                pstmt = connection.prepareStatement(querySQL);
                pstmt.setInt(1, prodID);

                rs = pstmt.executeQuery();

                while (rs.next()) {
                	
                	System.out.println("rs pieno");
                	
                	recensione = new RecensioneBean();
                	
                	recensione.setID(rs.getInt("ID"));
                	recensione.setDataInserimento(rs.getDate("data_inserimento"));
                	recensione.setVotazione(rs.getInt("votazione"));
                	recensione.setCommento(rs.getString("commento"));
                	recensione.setPRODOTTO_ID(rs.getInt("PRODOTTO_ID"));
                	recensione.setUserID(rs.getInt("userID"));
                	
                	listaRecensioni.add(recensione);
                } 
                
                if (listaRecensioni.isEmpty()) {
                    // bisognerebbe lanciare un eccezione e quindi avere una pagina per le eccezioni
                    System.out.println("lista recensioni vuota");
                }
                
            } else {
                System.out.println("problema con la connessione al db");
            }
        } finally {
            if (pstmt != null) 
                pstmt.close();            
            if (connection != null) 
                connection.close();
        }
        return listaRecensioni;
    }
    
    // verifica se un utente loggato ha recensito il prodotto passato
    public synchronized RecensioneBean getRecensione(int userID, int prodID) throws SQLException {
    	
    	String querySQL = "SELECT * FROM recensione WHERE PRODOTTO_ID = ? AND userID = ?";    
    	RecensioneBean recensione = new RecensioneBean();
    	
    	System.out.println("user e prod");
    	
    	System.out.println(userID);
    	System.out.println(prodID);

        try {
            connection = ds.getConnection();

            if (connection != null) {
                pstmt = connection.prepareStatement(querySQL);
                pstmt.setInt(1, prodID);
                pstmt.setInt(2, userID);

                rs = pstmt.executeQuery();

                if (rs.next()) {
                	
                	System.out.println("rs pieno");
                	
                	recensione.setID(rs.getInt("ID"));
                	recensione.setDataInserimento(rs.getDate("data_inserimento"));
                	recensione.setVotazione(rs.getInt("votazione"));
                	recensione.setCommento(rs.getString("commento"));
                	recensione.setPRODOTTO_ID(rs.getInt("PRODOTTO_ID"));
                	recensione.setUserID(rs.getInt("userID"));
                } else {
                	
                	System.out.println("rs vuoto");
                }
                
                if (recensione.isEmpty()) {
                    // bisognerebbe lanciare un eccezione e quindi avere una pagina per le eccezioni
                    System.out.println("recensione vuota, la query non ha prodotto risultati");
                }
                
            } else {
                System.out.println("problema con la connessione al db");
            }
        } finally {
            if (pstmt != null) 
                pstmt.close();            
            if (connection != null) 
                connection.close();
        }
        
        return recensione;
    }
    
    public synchronized void modificaRecensione(int userID, int punteggio, String commento) throws SQLException {
    	
    	String querySQL = "UPDATE recensione SET votazione = ?, commento = ? WHERE ID = ?";
        
        try {
            connection = ds.getConnection();
            
            if (connection != null) {
                pstmt = connection.prepareStatement(querySQL);
                
                pstmt.setInt(1, punteggio);
                pstmt.setString(2, commento);
                pstmt.setInt(3, userID);

                //controllo sul numero di righe inserite
                int rowsInserted = pstmt.executeUpdate();
                
                //connection.commit();

                if (rowsInserted > 0) {
                    System.out.println("recensione modificata!");
                } else {
                    System.out.println("modifica non avvenuta!");
                }
            } else {
                System.out.println("problema con la connessione al db");
            }
        } finally {
            if (pstmt != null) 
                pstmt.close();            
            if (connection != null) 
                connection.close();
        }
    }
    
    public synchronized void addRecensione(int userID, int prodID, int punteggio, String commento) throws SQLException {
    	
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = formatter.format(new Date());
        
        String querySQL = "INSERT INTO recensione (userID, PRODOTTO_ID, votazione, commento, data_inserimento) VALUES (?, ?, ?, ?, ?)";
        
        try {
            connection = ds.getConnection();
            
            if (connection != null) {
                pstmt = connection.prepareStatement(querySQL);
                
                pstmt.setInt(1, userID);
                pstmt.setInt(2, prodID);
                pstmt.setInt(3, punteggio);
                pstmt.setString(4, commento);
                pstmt.setString(5, strDate); // data corente 

                //controllo sul numero di righe inserite
                int rowsInserted = pstmt.executeUpdate();
                
                //connection.commit();

                if (rowsInserted > 0) {
                    System.out.println("recensione inserita!");
                } else {
                    System.out.println("non inserito!");
                }
            } else {
                System.out.println("problema con la connessione al db");
            }
        } finally {
            if (pstmt != null) 
                pstmt.close();            
            if (connection != null) 
                connection.close();
        }
    }
}
