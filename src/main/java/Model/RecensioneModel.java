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

    // Connessione e statement sono variabili locali dei try-with-resources, non campi: il DAO è condiviso
    // tra le richieste. Con try-with-resources le risorse vengono chiuse anche in caso di errore (anche se
    // fallisce la chiusura dello statement), e un errore nella chiusura non sostituisce quello della query,
    // ma finisce tra i suoi soppressi. getConnection() non restituisce mai null: o dà una connessione o
    // lancia un'eccezione.

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

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setInt(1, prodID);

            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        }

        if (listaRecensioni.isEmpty()) {
            // un prodotto senza recensioni dà una lista vuota, mai null (GEN-05 in specifiche.md)
            System.out.println("lista recensioni vuota");
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

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setInt(1, prodID);
            pstmt.setInt(2, userID);

            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        }

        if (recensione.isEmpty()) {
            // l'utente non ha recensito il prodotto: si restituisce la recensione vuota, mai null (GEN-05)
            System.out.println("recensione vuota, la query non ha prodotto risultati");
        }

        return recensione;
    }

    public synchronized void modificaRecensione(int idRecensione, int punteggio, String commento) throws SQLException {

    	String querySQL = "UPDATE recensione SET votazione = ?, commento = ? WHERE ID = ?";

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(querySQL)) {

            pstmt.setInt(1, punteggio);
            pstmt.setString(2, commento);
            pstmt.setInt(3, idRecensione);

            //controllo sul numero di righe inserite
            int rowsInserted = pstmt.executeUpdate();

            //connection.commit();

            if (rowsInserted > 0) {
                System.out.println("recensione modificata!");
            } else {
                System.out.println("modifica non avvenuta!");
            }
        }
    }

    public synchronized void addRecensione(int userID, int prodID, int punteggio, String commento) throws SQLException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = formatter.format(new Date());

        String querySQL = "INSERT INTO recensione (userID, PRODOTTO_ID, votazione, commento, data_inserimento) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(querySQL)) {

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
        }
    }
}
