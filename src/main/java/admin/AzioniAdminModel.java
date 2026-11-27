package admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import Model.Bean.ProductBean;

public class AzioniAdminModel implements AzioniAdminModel_intf{

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
	
	public AzioniAdminModel() {
		
	}
	
	// try-with-resources chiude statement e connessione anche in caso di errore,
	// così le connessioni tornano al pool invece di esaurirlo.
	public boolean inserisciProdotto(ProductBean prodotto)  throws SQLException {
		// inserisco i prodotti nel database
		String insertSQL = "INSERT INTO prodotto (id, nome, descrizione, prezzo, grammatura, categoria, QTAmagazzino, percentualeSconto, colorazione, dataInserimento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection connection = ds.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
			preparedStatement.setInt(1, prodotto.getID());
			preparedStatement.setString(2, prodotto.getNome());
			preparedStatement.setString(3, prodotto.getDescrizione());
			preparedStatement.setDouble(4, prodotto.getPrezzo());
			preparedStatement.setDouble(5, prodotto.getGrammatura());
			preparedStatement.setString(6, prodotto.getCategoria());
			preparedStatement.setInt(7, prodotto.getQTA_magazzino());
			preparedStatement.setDouble(8, prodotto.getPercentualeSconto());
			preparedStatement.setString(9, prodotto.getColorazione());
			preparedStatement.setDate(10, prodotto.getDataInserimento());

			return preparedStatement.executeUpdate() > 0;
		}
	}

	// Come inserisciProdotto: true solo se lo sconto è stato salvato. Con un id inesistente non viene
	// modificata nessuna riga e il chiamante mostra la pagina di errore (ADM-05 in specifiche.md).
	public boolean modificaSconto(int idProdotto,int percentualeSconto)  throws SQLException{
		String updateSQL = "UPDATE prodotto SET percentualeSconto = ? WHERE ID = ?";

		try (Connection connection = ds.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
			preparedStatement.setDouble(1, percentualeSconto);
			preparedStatement.setInt(2, idProdotto);

			return preparedStatement.executeUpdate() > 0;
		}
	}

}
