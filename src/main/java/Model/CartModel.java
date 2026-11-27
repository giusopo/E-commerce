package Model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import Model.Bean.ProductBean;
import Model.Interface.CartModel_intf;

public class CartModel implements CartModel_intf{

	private static DataSource ds;
	private static int PERC_IVA = 20;

	private static final String TABLE_ORDINE = "ordine";
	private static final String TABLE_PRODOTTO = "prodotto";
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

	// idCarrello è l'id dell'ordine "in corso" dell'utente, cioè il suo carrello.
	// giaPresente viene dal chiamante: il carrello è già caricato in sessione, quindi non serve una query
	// in più per sapere se il prodotto c'è. Se c'è, la sua quantità aumenta di qta; altrimenti il prodotto
	// viene inserito con quantità qta.
	public synchronized void addProduct(int idCarrello,ProductBean prodotto, boolean giaPresente,int qta) throws SQLException {

	    String querySQL;

	    // L'incremento si calcola sulla riga stessa, senza una subquery sulla tabella che si aggiorna:
	    // MySQL non la accetta (errore 1093).
	    if(giaPresente)
	    	querySQL = "UPDATE " + RELAZIONE + " SET QTAordinata = QTAordinata + ? "
	    			+ "WHERE ID_ordine = ? AND ID_prodotto = ?";
	    else
			querySQL = "INSERT INTO `contiene` "
					+ "(`ID_ordine`, `ID_prodotto`, `QTAordinata`) "
					+ "VALUES (?, ?, ?)";

	    // try-with-resources: le risorse vengono chiuse anche in caso di errore, e un errore nella chiusura
	    // non sostituisce quello della query, ma finisce tra i suoi soppressi.
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(querySQL)) {

		    if(giaPresente) {
		    	pstmt.setInt(1, qta);
		    	pstmt.setInt(2, idCarrello);
		    	pstmt.setInt(3, prodotto.getID());
		    }
		    else {
		    	pstmt.setInt(1, idCarrello);
		    	pstmt.setInt(2, prodotto.getID());
		    	pstmt.setInt(3, qta);
		    }

			System.out.println(pstmt.toString());

		    pstmt.executeUpdate();
        }
	}


	// idCarrello è l'id dell'ordine "in corso" dell'utente, cioè il suo carrello.
	// isUltimo vero toglie il prodotto; falso toglie un pezzo, ma solo se ne resta almeno uno.
	public void deleteProduct(int idCarrello,ProductBean prodotto,boolean isUltimo) throws SQLException {

	    String querySQL;

	    if(isUltimo)
			querySQL = "DELETE FROM " + RELAZIONE + " WHERE "+ RELAZIONE +".`ID_ordine` = ? AND "+ RELAZIONE +".`ID_prodotto` = ?";
	    else
	    	querySQL = "UPDATE " + RELAZIONE + " SET QTAordinata = QTAordinata - 1 "
	    			+ "WHERE ID_ordine = ? AND ID_prodotto = ? AND QTAordinata > 1";

	    // try-with-resources: le risorse vengono chiuse anche in caso di errore, e un errore nella chiusura
	    // non sostituisce quello della query, ma finisce tra i suoi soppressi.
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(querySQL)) {

		    pstmt.setInt(1, idCarrello);
		    pstmt.setInt(2, prodotto.getID());

			System.out.println(pstmt.toString());

		    pstmt.executeUpdate();
        }
	}

	public ArrayList<ProductBean> getProducts(int id) throws SQLException {

	    String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*, " + RELAZIONE + ".QTAordinata " +
	            " FROM " + TABLE_UTENTE +
	            " JOIN " + TABLE_ORDINE + " ON " + TABLE_ORDINE + ".UTENTE_ID = " + TABLE_UTENTE + ".ID" +
	            " JOIN " + RELAZIONE + " ON " + RELAZIONE + ".ID_ordine = " + TABLE_ORDINE + ".ID" +
	            " JOIN " + TABLE_PRODOTTO + " ON " + TABLE_PRODOTTO + ".ID = " + RELAZIONE + ".ID_prodotto" +
	            " WHERE " + TABLE_ORDINE + ".stato = 'in corso' AND " + TABLE_UTENTE + ".ID = ?";

	    ArrayList<ProductBean> products = new ArrayList<>();

	    // try-with-resources: le risorse vengono chiuse anche in caso di errore, e un errore nella chiusura
	    // non sostituisce quello della query, ma finisce tra i suoi soppressi.
	    try (Connection connection = ds.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
		    pstmt.setInt(1, id);

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

	                products.add(bean);
	            }
	        }
	    }
	    System.out.println("CARRELLO RECUPERATO CORRETTAMENRE , GRANDEZZA : " + products.size());
	    return products;
	}

	// Restituisce l'id del nuovo carrello dell'utente.
	public int confermaOrdine(String indirizzoConsegna,int idOrdine, int idUtente, List<ProductBean> prodotti) throws SQLException {
	    String queryPrezziFatturati = "UPDATE contiene SET prezzo_all_acquisto = ? WHERE id_ordine = ? AND id_prodotto = ?";
	    // La giacenza diminuisce solo se basta: altrimenti la riga non viene aggiornata e l'ordine fallisce.
	    String queryGiacenza = "UPDATE prodotto SET QTAmagazzino = QTAmagazzino - ? WHERE ID = ? AND QTAmagazzino >= ?";
	    // carrello = NULL: la riga smette di essere il carrello dell'utente (vincolo un_carrello_per_utente).
	    String queryOrdineConfermato = "UPDATE ordine SET stato = 'concluso', carrello = NULL, prezzo_tot = ?,percentuale_iva = ? ,data_inoltro = ?,indirizzo_consegna = ? WHERE id = ?";
	    String generaNuovoCarrello = "INSERT INTO ordine (utente_id) VALUES (?)";
	    double PrezzoTot = 0;
	    int nuovoCarrello;

	    // I quattro passi formano un'unica transazione: se uno fallisce, nessuna modifica resta nel database.
	    try (Connection connection = ds.getConnection()) {
	        connection.setAutoCommit(false);
	        try {
	            // Aggiorna i prezzi fatturati
	            try (PreparedStatement pstmt = connection.prepareStatement(queryPrezziFatturati)) {
	                for (ProductBean bean : prodotti) {
	                    pstmt.setDouble(1, bean.getPrezzo() - ((bean.getPrezzo() * bean.getPercentualeSconto()) / 100));
	                    pstmt.setInt(2, idOrdine);
	                    pstmt.setInt(3, bean.getID());
	                    pstmt.executeUpdate();
	                    PrezzoTot += (bean.getPrezzo() - ((bean.getPrezzo() * bean.getPercentualeSconto()) / 100))*bean.getQTA_ordinata();
	                }
	            }

	            // Toglie dalla giacenza i pezzi ordinati
	            try (PreparedStatement pstmt = connection.prepareStatement(queryGiacenza)) {
	                for (ProductBean bean : prodotti) {
	                    pstmt.setInt(1, bean.getQTA_ordinata());
	                    pstmt.setInt(2, bean.getID());
	                    pstmt.setInt(3, bean.getQTA_ordinata());
	                    if (pstmt.executeUpdate() == 0) {
	                        throw new SQLException("Giacenza insufficiente per il prodotto " + bean.getID());
	                    }
	                }
	            }

	            // Conferma l'ordine
	            try (PreparedStatement pstmt = connection.prepareStatement(queryOrdineConfermato)) {
	                pstmt.setDouble(1, PrezzoTot);
	                pstmt.setInt(2, PERC_IVA);
	                pstmt.setString(3, LocalDate.now().toString());
	                pstmt.setString(4, indirizzoConsegna);
	                pstmt.setInt(5, idOrdine);
	                pstmt.executeUpdate();
	            }

	            // Genera un nuovo carrello e ne legge l'id, che il chiamante mette in sessione
	            try (PreparedStatement pstmt = connection.prepareStatement(generaNuovoCarrello, Statement.RETURN_GENERATED_KEYS)) {
	                pstmt.setInt(1, idUtente);
	                pstmt.executeUpdate();
	                try (ResultSet chiavi = pstmt.getGeneratedKeys()) {
	                    if (!chiavi.next()) {
	                        throw new SQLException("Il database non ha restituito l'id del nuovo carrello");
	                    }
	                    nuovoCarrello = chiavi.getInt(1);
	                }
	            }

	            connection.commit();
	        } catch (SQLException | RuntimeException e) {
	            annulla(connection, e);
	            throw e;
	        } finally {
	            connection.setAutoCommit(true);
	        }
	    }
	    return nuovoCarrello;
	}

	// Annulla la transazione senza perdere l'errore che l'ha causata.
	private static void annulla(Connection connection, Exception causa) {
	    try {
	        connection.rollback();
	    } catch (SQLException e) {
	        causa.addSuppressed(e);
	    }
	}

}
