package GestioneIMG;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Accesso alla tabella immagine. Come negli altri DAO, connessione e statement sono variabili locali dei
 * try-with-resources e i valori arrivano al database come parametri, mai concatenati alla query.
 */
public class IMGmodel {

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

	private IMGmodel() {
	}

	/** I byte dell'immagine del prodotto con il ruolo indicato, oppure null se non c'è. */
	public static byte[] load(int idProdotto, String ruolo) throws SQLException {
		String sql = "SELECT IMMAGINE FROM immagine WHERE PRODOTTO_ID = ? AND ruolo = ?";
		byte[] immagine = null;

		try (Connection connection = ds.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, idProdotto);
			pstmt.setString(2, ruolo);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					immagine = rs.getBytes("IMMAGINE");
				}
			}
		}
		return immagine;
	}

	/** Salva come immagine del prodotto, con il ruolo indicato, il file che si trova nel percorso foto. */
	public static void updatePhoto(int idProdotto, String ruolo, String foto) throws SQLException, IOException {
		String sql = "INSERT INTO immagine(IMMAGINE,RUOLO,PRODOTTO_ID) VALUES(?,?,?)";
		Path file = Path.of(foto);

		try (Connection connection = ds.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(sql);
			 InputStream contenuto = Files.newInputStream(file)) {
			pstmt.setBinaryStream(1, contenuto, Files.size(file));
			pstmt.setString(2, ruolo);
			pstmt.setInt(3, idProdotto);

			pstmt.executeUpdate();
		}
	}
}
