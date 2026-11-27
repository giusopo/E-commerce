package admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import Model.Bean.ProductBean;

public class AzioniAdminModel implements AzioniAdminModel_intf{

	private static DataSource ds;

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
	// L'id del prodotto lo genera il database: dopo l'inserimento viene scritto nel bean.
	public boolean inserisciProdotto(ProductBean prodotto)  throws SQLException {
		String insertSQL = "INSERT INTO prodotto (nome, descrizione, prezzo, grammatura, unita, categoria, subCategoria, QTAmagazzino, percentualeSconto, colorazione, DataInserimento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		boolean inserito;

		try (Connection connection = ds.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
			preparedStatement.setString(1, prodotto.getNome());
			preparedStatement.setString(2, prodotto.getDescrizione());
			preparedStatement.setDouble(3, prodotto.getPrezzo());
			preparedStatement.setDouble(4, prodotto.getGrammatura());
			preparedStatement.setString(5, prodotto.getUnita());
			preparedStatement.setString(6, prodotto.getCategoria());
			// la sottocategoria è facoltativa: vuota diventa NULL
			if (prodotto.getSubCategoria() == null || prodotto.getSubCategoria().isEmpty())
				preparedStatement.setNull(7, Types.VARCHAR);
			else
				preparedStatement.setString(7, prodotto.getSubCategoria());
			preparedStatement.setInt(8, prodotto.getQTA_magazzino());
			preparedStatement.setDouble(9, prodotto.getPercentualeSconto());
			preparedStatement.setString(10, prodotto.getColorazione());
			preparedStatement.setDate(11, prodotto.getDataInserimento());

			inserito = preparedStatement.executeUpdate() > 0;
			if (inserito) {
				try (ResultSet chiavi = preparedStatement.getGeneratedKeys()) {
					if (!chiavi.next()) {
						throw new SQLException("Il database non ha restituito l'id del prodotto");
					}
					prodotto.setID(chiavi.getInt(1));
				}
			}
		}
		return inserito;
	}

	// Come inserisciProdotto: true solo se lo sconto è stato salvato. Con un id inesistente non viene
	// modificata nessuna riga e il chiamante mostra la pagina di errore.
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
