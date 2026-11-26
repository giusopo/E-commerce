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

    // Connessione e statement sono variabili locali dei try-with-resources, non campi: il DAO è condiviso
    // tra le richieste. Con try-with-resources le risorse vengono chiuse anche in caso di errore (anche se
    // fallisce la chiusura dello statement), e un errore nella chiusura non sostituisce quello della query,
    // ma finisce tra i suoi soppressi.

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
        // Utente e carrello vengono salvati in un'unica transazione:
        // se la creazione del carrello fallisce, l'utente non resta registrato senza carrello.
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, user.getNome());
                    stmt.setString(2, user.getCognome());
                    stmt.setString(3, user.getUserName());
                    stmt.setString(4, user.getDataNascita().toString());
                    stmt.setString(5, user.getEmail());
                    stmt.setString(6, user.getPassword());
                    stmt.setString(7, user.getPhoneNumber());
                    stmt.setString(8, user.getIndirizzoBase());
                    stmt.setString(9, "0");
                    stmt.executeUpdate();
                }

                int idUtente = -1;
                try (PreparedStatement stmt = conn.prepareStatement(sql2)) {
                    stmt.setString(1, user.getUserName());
                    stmt.setString(2, user.getPassword());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            idUtente = rs.getInt("id");
                        }
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(sql3)) {
                    stmt.setInt(1, idUtente);
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException erroreRollback) {
                    e.addSuppressed(erroreRollback);
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public UserBean getUserTramiteCredenziali(String username,String pwd) throws SQLException  {

        UserBean user = new UserBean();

        String sql = "SELECT utente.* , ordine.ID as cartID FROM utente"
        		+ " JOIN ordine ON ordine.utente_id = utente.id"
        		+ " WHERE USER_NAME = ? AND password = ? AND ordine.stato = 'in corso';";
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, pwd);

            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        }

        return user;

    }


    public int getIdCart(String username,String pwd) throws SQLException  {

    	String sql = "SELECT ordine.ID as cartID FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE USER_NAME = ? AND password = ? AND ordine.stato = 'in corso';";

        // Il return è dopo i try-with-resources: con un return all'interno il compilatore genera branch
        // che JaCoCo non riesce a escludere dal conteggio.
        int idCarrello = -1;
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, pwd);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    idCarrello = rs.getInt("cartID");
                    System.out.println("aaaa " + idCarrello);
                } else {
                    System.out.println("bbbbbbbbbbbbbbb");
                }
            }
        }

        return idCarrello;
    }

    public OrdineBean getOrdineById(int idUtente,int idOrdine) throws SQLException  {

    	String sql = "SELECT ordine.* FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE utente.id = ? AND ordine.id = ? AND ordine.stato = 'concluso';";


        OrdineBean ordine = new OrdineBean();

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, idUtente);
            pstmt.setInt(2, idOrdine);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {

                	ordine.setIdOrdine(rs.getInt("id"));
                	ordine.setPrezzoTot(rs.getDouble("prezzo_tot"));
                	ordine.setPercentualeIva(rs.getInt("percentuale_iva"));
                	ordine.setDataInoltro(rs.getString("data_inoltro"));
                	ordine.setIndirizzoConsegna(rs.getString("indirizzo_consegna"));

                }
            }
        }

        String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*, " + RELAZIONE + ".QTAordinata, " + RELAZIONE + ".prezzo_all_acquisto " +
                " FROM " + TABLE_ORDINE +
                " JOIN " + RELAZIONE + " ON " + RELAZIONE + ".ID_ordine = " + TABLE_ORDINE + ".ID" +
                " JOIN " + TABLE_PRODOTTO + " ON " + TABLE_PRODOTTO + ".ID = " + RELAZIONE + ".ID_prodotto" +
                " WHERE " + TABLE_ORDINE + ".ID = ?";


	    ArrayList<ProductBean> products = new ArrayList<>();

	    try (Connection connection = ds.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
		    pstmt.setInt(1, ordine.getIdOrdine());

	        try (ResultSet rs = pstmt.executeQuery()) {
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
	        }
	    }
	    System.out.println("ORDINE RECUPERATO CORRETTAMENRE , GRANDEZZA : " + products.size());

	    ordine.setProdotti(products);
        return ordine;
    }

    public ArrayList<Integer> getAllOrdine(int idUtente) throws SQLException  {

    	String sql = "SELECT ordine.Id FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE utente.id = ? AND ordine.stato = 'concluso';";


        ArrayList<Integer> idOrdini = new ArrayList<Integer>();

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, idUtente);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                	idOrdini.add(rs.getInt("id"));
                }
            }
        }

	    System.out.println("ORDINI RECUPERATI CORRETTAMENRE , GRANDEZZA : " + idOrdini.size());

        return idOrdini;
    }

    public int getIdCartByUserId(int id) throws SQLException  {

    	String sql = "SELECT ordine.ID as cartID FROM utente"
    	           + " JOIN ordine ON ordine.utente_id = utente.id"
    	           + " WHERE utente.id = ? AND ordine.stato = 'in corso';";

        // Come in getIdCart, il return è dopo i try-with-resources.
        int idCarrello = -1;
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    idCarrello = rs.getInt("cartID");
                    System.out.println("aaaa " + idCarrello);
                } else {
                    System.out.println("bbbbbbbbbbbbbbb");
                }
            }
        }

        return idCarrello;

    }
    public UserBean getUserTramiteID(int ID) throws SQLException  {

        UserBean user = new UserBean();

        String sql = "SELECT utente.* FROM utente"
        		+ " WHERE utente.ID = ?";

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ID);

            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        }

        return user;

    }

    public Collection<UserBean> getUtenti() throws SQLException{

        UserBean user;
        Collection<UserBean> utenti = new ArrayList<UserBean>();
        String sql = "SELECT utente.* FROM utente";

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

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
            }
        }

        return utenti;

	}
}
