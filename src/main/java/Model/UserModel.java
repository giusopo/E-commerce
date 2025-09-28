package Model;

import Model.Bean.OrdineBean;
import Model.Bean.ProductBean;
import Model.Bean.UserBean;
import Model.Interface.UserModel_intf;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class UserModel implements UserModel_intf {

	private static DataSource ds;

	private static final String TABLE_ORDINE = "ordine";
	private static final String TABLE_PRODOTTO = "prodotto";
    private static final String TABLE_IMMAGINE = "immagine";
    private static final String TABLE_UTENTE = "utente";
    private static final String RELAZIONE = "contiene";
    
    
    private Connection connection = null;
    private PreparedStatement pstmt = null;
    
    static {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");

            ds = (DataSource) envCtx.lookup("jdbc/ecommerce");

        } catch (NamingException e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

    @Override
    public void AddUser(UserBean user) throws SQLException  {

        String sql = "INSERT INTO utente (NOME, COGNOME, USER_NAME, DATA_NASCITA, email, password, telefono, indirizzoBase, permessi_admin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        String sql2 = "SELECT id FROM utente WHERE user_name = ? AND password = ?";

        String sql3 = "INSERT INTO ordine (utente_id) VALUES (?)";
        int idUtente = -1;
        
        try {
            connection = ds.getConnection();

            if (connection != null) {
                pstmt = connection.prepareStatement(sql);

                pstmt.setString(1, user.getNome());
                pstmt.setString(2, user.getCognome());
                pstmt.setString(3, user.getUserName());
                pstmt.setString(4, user.getDataNascita().toString());
                pstmt.setString(5, user.getEmail());
                pstmt.setString(6, user.getPassword());
                pstmt.setString(7, user.getPhoneNumber());
                pstmt.setString(8, user.getIndirizzoBase());
                pstmt.setString(9, "0");

                System.out.println(user.getPassword());                
                //controllo sul numero di righe inserite
                int rowsInserted = pstmt.executeUpdate();
                
                //connection.commit();

                if (rowsInserted > 0) {
                    System.out.println("A new user was inserted successfully!");
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
        
        try {
            connection = ds.getConnection();

            if (connection != null) {
                pstmt = connection.prepareStatement(sql2);

                pstmt.setString(1, user.getUserName());
                pstmt.setString(2, user.getPassword());

                //controllo sul numero di righe inserite
                ResultSet rs = pstmt.executeQuery();
                
                if(rs.next()) {
                	idUtente = rs.getInt("id");
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
        
        try {
            connection = ds.getConnection();

            if (connection != null) {
                pstmt = connection.prepareStatement(sql3);

                pstmt.setInt(1, idUtente);
                
                //controllo sul numero di righe inserite
                int rowsInserted = pstmt.executeUpdate();
                
                //connection.commit();

                if (rowsInserted > 0) {
                    System.out.println("A new user was inserted successfully!");
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

    public UserBean getUserTramiteCredenziali(String username,String pwd) throws SQLException  {

        UserBean user = new UserBean();

        String sql = "SELECT utente.* , Ordine.ID as cartID FROM utente"
        		+ " JOIN ordine ON ordine.utente_id = utente.id"
        		+ " WHERE USER_NAME = ? AND password = ? AND ordine.stato = 'in corso';";
        try {
            connection = ds.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, pwd);
            System.out.println(pstmt.toString());

            ResultSet rs = pstmt.executeQuery();

            //connection.commit();

            if (rs.next()) {

                user.setID(rs.getInt("ID"));
                user.setUserName(rs.getString("USER_NAME"));
                user.setPassword(rs.getString("password"));
                user.setCartID(rs.getInt("cartID"));
                user.setIndirizzoBase(rs.getString("indirizzoBase"));
                user.setPhoneNumber(rs.getString("telefono"));
                user.setPermessiAdmin(rs.getBoolean("permessi_admin"));

            } else {

                System.out.println("l'utente "+ username +" non è stato trovato");
            }

        }finally {
            if (pstmt != null) 
                    pstmt.close();
            if (connection != null) 
                    connection.close();   
        }

        return user;
        
    }

    
    public int getIdCart(String username,String pwd) throws SQLException  {

    	String sql = "SELECT Ordine.ID as cartID FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE USER_NAME = ? AND password = ? AND ordine.stato = 'in corso';";

        try {
            connection = ds.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, pwd);

            ResultSet rs = pstmt.executeQuery();

            //connection.commit();
            if (rs.next()) {
                int temp = rs.getInt("cartID");
                System.out.println("aaaa " + temp);
                return temp;
                
            } else {
                System.out.println("bbbbbbbbbbbbbbb");
                return -1;
            }

        }finally {
            if (pstmt != null) 
                    pstmt.close();
            if (connection != null) 
                    connection.close();   
        }
        
    }
 
    public OrdineBean getOrdineById(int idUtente,int idOrdine) throws SQLException  {

    	String sql = "SELECT Ordine.* FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE utente.id = ? AND ordine.id = ? AND ordine.stato = 'concluso';";
    	
    	
        OrdineBean ordine = new OrdineBean();

        try {
            connection = ds.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, idUtente);
            pstmt.setInt(2, idOrdine);

            ResultSet rs = pstmt.executeQuery();
            
            //connection.commit();
            if (rs.next()) {
                
            	ordine.setIdOrdine(rs.getInt("id"));
            	ordine.setPrezzoTot(rs.getDouble("prezzo_tot"));
            	ordine.setPercentualeIva(rs.getInt("percentuale_iva"));
            	ordine.setDataInoltro(rs.getString("data_inoltro"));
            	ordine.setIndirizzoConsegna(rs.getString("indirizzo_consegna"));

            }

        }finally {
            if (pstmt != null) 
                    pstmt.close();
            if (connection != null) 
                    connection.close();   
        }
        
        String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*, " + RELAZIONE + ".QTAordinata, " + RELAZIONE + ".prezzo_all_acquisto " +
                " FROM " + TABLE_ORDINE +
                " JOIN " + RELAZIONE + " ON " + RELAZIONE + ".ID_ordine = " + TABLE_ORDINE + ".ID" +
                " JOIN " + TABLE_PRODOTTO + " ON " + TABLE_PRODOTTO + ".ID = " + RELAZIONE + ".ID_prodotto" +
                " WHERE " + TABLE_ORDINE + ".ID = ?";

	    
	    ArrayList<ProductBean> products = new ArrayList<>();
	    Connection connection = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;

	    try {
	        connection = ds.getConnection();
	        pstmt = connection.prepareStatement(selectSQL);
		    pstmt.setInt(1, ordine.getIdOrdine());

	        rs = pstmt.executeQuery();

	        while (rs.next()) {
	            ProductBean bean = new ProductBean();

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
	            bean.setPrezzoAllAcquisto(rs.getDouble("prezzo_all_acquisto"));

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
	    System.out.println("ORDINE RECUPERATO CORRETTAMENRE , GRANDEZZA : " + products.size());
	    
	    ordine.setProdotti(products);
        return ordine;
    }    
    
    public ArrayList<Integer> getAllOrdine(int idUtente) throws SQLException  {

    	String sql = "SELECT Ordine.Id FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE utente.id = ? AND ordine.stato = 'concluso';";
    	
    	
        ArrayList<Integer> idOrdini = new ArrayList<Integer>();

        try {
            connection = ds.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, idUtente);

            ResultSet rs = pstmt.executeQuery();
            
            //connection.commit();
            while (rs.next()) {
            	idOrdini.add(rs.getInt("id"));
            }

        }finally {
            if (pstmt != null) 
                    pstmt.close();
            if (connection != null) 
                    connection.close();   
        }
        
	    System.out.println("ORDINI RECUPERATI CORRETTAMENRE , GRANDEZZA : " + idOrdini.size());
	    
        return idOrdini;
    }   
    
    public int getIdCartByUserId(int id) throws SQLException  {

    	String sql = "SELECT Ordine.ID as cartID FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE utente.id = ? AND ordine.stato = 'in corso';";

        try {
            connection = ds.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();

            //connection.commit();
            if (rs.next()) {
                int temp = rs.getInt("cartID");
                System.out.println("aaaa " + temp);
                return temp;
                
            } else {
                System.out.println("bbbbbbbbbbbbbbb");
                return -1;
            }

        }finally {
            if (pstmt != null) 
                    pstmt.close();
            if (connection != null) 
                    connection.close();   
        }
        
    }
    public UserBean getUserTramiteID(int ID) throws SQLException  {

        UserBean user = new UserBean();

        String sql = "SELECT utente.* FROM utente"
        		+ " WHERE utente.ID = ?";

        try {
            connection = ds.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, ID);

            ResultSet rs = pstmt.executeQuery();

            //connection.commit();

            if (rs.next()) {
            	user.setNome(rs.getString("nome"));
            	user.setCognome(rs.getString("cognome"));
                user.setID(rs.getInt("ID"));
                user.setUserName(rs.getString("USER_NAME"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setCartID(-1);
                user.setIndirizzoBase(rs.getString("indirizzoBase"));
                user.setPhoneNumber(rs.getString("telefono"));
                user.setPermessiAdmin(rs.getBoolean("permessi_admin"));

            } else {
                System.out.println("accesso negato, l'utente non è stato trovato");
            }

        }finally {
            if (pstmt != null) 
                    pstmt.close();
            if (connection != null) 
                    connection.close();   
        }

        return user;
        
    }
    
    public Collection<UserBean> getUtenti() throws SQLException{

        UserBean user;
        Collection<UserBean> utenti = new ArrayList<UserBean>();
        String sql = "SELECT utente.* FROM utente";

        try {
            connection = ds.getConnection();
            pstmt = connection.prepareStatement(sql);

            ResultSet rs = pstmt.executeQuery();

            while(rs.next()){
            	user = new UserBean();
	            	user.setNome(rs.getString("nome"));
	            	user.setCognome(rs.getString("cognome"));
	                user.setID(rs.getInt("ID"));
	                user.setUserName(rs.getString("USER_NAME"));
	                user.setPassword(rs.getString("password"));
	                user.setEmail(rs.getString("email"));
	                user.setCartID(-1);
	                user.setIndirizzoBase(rs.getString("indirizzoBase"));
	                user.setPhoneNumber(rs.getString("telefono"));
	                user.setPermessiAdmin(rs.getBoolean("permessi_admin"));
	                
	                utenti.add(user);
	            };  
        }finally {
            if (pstmt != null) 
                    pstmt.close();
            if (connection != null) 
                    connection.close();   
        }

        return utenti;
        	
	}
}
