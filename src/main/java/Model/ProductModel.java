package Model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import Model.Bean.ProductBean;
import Model.Interface.ProductModel_intf;

public class ProductModel implements ProductModel_intf {

	private static DataSource ds;

    // Connessione e statement sono variabili locali dei try-with-resources, non campi: il DAO è condiviso
    // tra le richieste, e con dei campi due richieste contemporanee si sovrascriverebbero le risorse.
    // Con try-with-resources le risorse vengono chiuse anche in caso di errore, e un errore nella chiusura
    // non sostituisce quello della query, ma finisce tra i suoi soppressi.

    // private ProductBean beanData = new ProductBean();
    //private List<ImgBean> images = new ArrayList<ImgBean>();

	private static final String TABLE_PRODOTTO = "prodotto";
    private static final String TABLE_IMG = "immagine";
    private static final String TABLE_RECENSIONE = "recensione";

	static {
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");

			ds = (DataSource) envCtx.lookup("jdbc/ecommerce");

		} catch (NamingException e) {
			System.out.println("Error:" + e.getMessage());
		}
	}

	public StringBuilder doRetrieveByInput(String input) throws SQLException {

    	 StringBuilder prodotti = new StringBuilder("["); // Uso StringBuilder per efficienza

        //presumiamo che le immagini siano 1 per prodotto , servirebbe un campo per la principale //FATTOs
    	 String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*" +
                 " FROM " + TABLE_PRODOTTO +
                 " JOIN " + TABLE_IMG + " ON " +
                 TABLE_PRODOTTO + ".ID = " + TABLE_IMG + ".PRODOTTO_ID" +
                 " WHERE " + TABLE_IMG + ".RUOLO = 'principale'" +
                 " AND " + TABLE_PRODOTTO + ".nome LIKE ?" +
                 " LIMIT 6";
        // evitiamo di prendere prodotti se eroneamente non hanno immagini , forse è una cosa inutile
        // eseguo la query per prendere i prodotti
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, "%" + input + "%");
            System.out.println(pstmt.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    prodotti.append("{")
                        .append("\"Id\": ").append(rs.getInt("ID")).append(", ")
                        .append("\"nome\": \"").append(rs.getString("nome")).append("\"")
                        .append("}, ");
                }
            }
        }

        //rimuovo l'ultima virgola
        if (prodotti.length() > 1) {
            prodotti.setLength(prodotti.length() - 2);
        }

        prodotti.append("]");

        return prodotti;
    }

	@Override
	public synchronized ProductBean doRetrieveByKey(int id) throws SQLException {

		ProductBean beanData = new ProductBean();

		String selectSQL = "SELECT " + TABLE_PRODOTTO + ".* " +
				                " FROM " + TABLE_PRODOTTO +
				                " WHERE " + TABLE_PRODOTTO + ".ID = ?";
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    beanData.setID(rs.getInt("ID"));
                    beanData.setNome(rs.getString("nome"));
                    beanData.setDescrizione(rs.getString("descrizione"));
                    beanData.setGrammatura(rs.getDouble("grammatura"));
                    beanData.setCategoria(rs.getString("categoria"));
                    beanData.setQTA_magazzino(rs.getInt("QTAmagazzino"));
                    beanData.setPrezzo(rs.getDouble("prezzo"));
                    beanData.setPercentualeSconto(rs.getInt("percentualeSconto"));
                    beanData.setColorazione(rs.getString("colorazione"));

                }
            }
        }

        if (beanData.isEmpty()) {
            // nessun prodotto con quell'id: si restituisce il prodotto vuoto, mai null (GEN-05 in specifiche.md)
            System.out.println("bean vuoto, la query non ha prodotto risultati");
        }

		return beanData;
	}

	@Override
	public Collection<ProductBean> doRetrieveAll(String order, int offset, int limit) throws SQLException {

	    Collection<ProductBean> products = new LinkedList<ProductBean>();

	    String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*" +
	                        " FROM " + TABLE_PRODOTTO + " JOIN " + TABLE_IMG + " ON " +
	                        TABLE_PRODOTTO + ".ID = " + TABLE_IMG + ".PRODOTTO_ID "
	                        + " WHERE " + TABLE_IMG + ".RUOLO = 'principale'";

	    String votazioneSQL = "SELECT PRODOTTO_ID, AVG(votazione) as MediaValutazione , COUNT(PRODOTTO_ID) AS numValut " +
	                          "FROM " + TABLE_RECENSIONE + " GROUP BY PRODOTTO_ID";

	    if (order != null && !order.isEmpty()) {
	        selectSQL += " ORDER BY " + order;
	    }

	    // Aggiungo il limite e l'offset
	    selectSQL += " LIMIT ? OFFSET ?";

	    // eseguo la query per prendere i prodotti
	    try (Connection connection = ds.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {

	        pstmt.setInt(1, limit);
	        pstmt.setInt(2, offset);

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
	                bean.setDataInserimento(rs.getDate("DataInserimento"));

	                products.add(bean);
	            }
	        }
	    }

	    aggiungiVotazioni(products, votazioneSQL);

	    return products;
	}


 	@Override
    public Collection<ProductBean> doRetrieveByCat(String category, String order) throws SQLException {

	 	//una volta list , una volta array list , una volta arrayList...
        Collection<ProductBean> products = new LinkedList<ProductBean>();

        //presumiamo che le immagini siano 1 per prodotto , servirebbe un campo per la principale
        String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*" + " FROM " + TABLE_PRODOTTO +
        		           " JOIN " + TABLE_IMG + " ON " + TABLE_PRODOTTO + ".ID = " + TABLE_IMG + ".PRODOTTO_ID" +
			               " WHERE "+ TABLE_IMG + ".RUOLO = 'principale'";
        // evitiamo di prendere prodotti se erroneamente non hanno immagini, è inutile!!!

        String votazioneSQL = "SELECT PRODOTTO_ID, AVG(votazione) as MediaValutazione , COUNT(PRODOTTO_ID) AS numValut " +
				  "FROM " + TABLE_RECENSIONE + " GROUP BY PRODOTTO_ID;";

        // la categoria arriva dalla richiesta HTTP: va passata come parametro, mai concatenata (SQL injection)
        boolean filtraCategoria = !category.equals("all");
        if (filtraCategoria) {
        	selectSQL += " AND prodotto.categoria = ?";
        }

        if (order != null && !order.isEmpty()) {
            selectSQL += " ORDER BY " + order;
        }

        System.out.println("ProductModel - SQL: " + selectSQL);

        // eseguo la query per prendere i prodotti
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            if (filtraCategoria)
            	pstmt.setString(1, category);

            try (ResultSet rs = pstmt.executeQuery()) {
                leggiProdottiConUnita(rs, products);
            }
        }

        aggiungiVotazioni(products, votazioneSQL);

        return products;
    }

 	@Override
 	public Collection<ProductBean> doRetrieveBySubCat(String category, String subcategory, String order) throws SQLException {

	 	//una volta list , una volta array list , una volta arrayList...
        Collection<ProductBean> products = new LinkedList<ProductBean>();

        //presumiamo che le immagini siano 1 per prodotto , servirebbe un campo per la principale
        String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*" + " FROM " + TABLE_PRODOTTO +
        		           " JOIN " + TABLE_IMG + " ON " + TABLE_PRODOTTO + ".ID = " + TABLE_IMG + ".PRODOTTO_ID" +
			               " WHERE "+ TABLE_IMG + ".RUOLO = 'principale'";
        // evitiamo di prendere prodotti se erroneamente non hanno immagini, è inutile!!!

        String votazioneSQL = "SELECT PRODOTTO_ID, AVG(votazione) as MediaValutazione , COUNT(PRODOTTO_ID) AS numValut " +
				  "FROM " + TABLE_RECENSIONE + " GROUP BY PRODOTTO_ID;";

        // categoria e sottocategoria arrivano dalla richiesta HTTP: vanno passate come parametri (SQL injection)
        boolean filtraCategoria = !category.equals("all");
        if (filtraCategoria) {
        	selectSQL += " AND prodotto.categoria = ? AND prodotto.subCategoria = ?";
        }

        if (order != null && !order.isEmpty()) {
            selectSQL += " ORDER BY " + order;
        }

        System.out.println("ProductModel - SQL: " + selectSQL);

        // eseguo la query per prendere i prodotti
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            if (filtraCategoria) {
            	pstmt.setString(1, category);
            	pstmt.setString(2, subcategory);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                leggiProdottiConUnita(rs, products);
            }
        }

        aggiungiVotazioni(products, votazioneSQL);

        return products;
 	}

 	/** Copia nei bean i prodotti letti da doRetrieveByCat e doRetrieveBySubCat, compresa l'unità di misura. */
 	private static void leggiProdottiConUnita(ResultSet rs, Collection<ProductBean> products) throws SQLException {
        while (rs.next()) {

            ProductBean bean = new ProductBean();

            bean.setID(rs.getInt("ID"));
            bean.setNome(rs.getString("nome"));
            bean.setDescrizione(rs.getString("descrizione"));
            bean.setPrezzo(rs.getDouble("prezzo"));
            bean.setGrammatura(rs.getDouble("grammatura"));
            bean.setUnita(rs.getString("unita"));
            bean.setCategoria(rs.getString("categoria"));
            bean.setQTA_magazzino(rs.getInt("QTAmagazzino"));
            bean.setPercentualeSconto(rs.getInt("percentualeSconto"));
            bean.setColorazione(rs.getString("colorazione"));
            bean.setDataInserimento(rs.getDate("DataInserimento"));

            products.add(bean);
        }
 	}

 	/** Seconda query di doRetrieveAll, doRetrieveByCat e doRetrieveBySubCat: media e numero dei voti. */
 	private void aggiungiVotazioni(Collection<ProductBean> products, String votazioneSQL) throws SQLException {
        try (Connection connection = ds.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(votazioneSQL);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                double media = rs.getDouble("MediaValutazione");
                int ID = rs.getInt("PRODOTTO_ID");
                int NumValut = rs.getInt("numValut");

                for (ProductBean p : products) {
                    if (p.getID() == ID) {
                        p.setVotazione(media);
                        p.setNrecensioni(NumValut);
                    }
                }
            }
        }
 	}

 	@Override
	public synchronized boolean doDelete(int code) throws SQLException {
		String deleteSQL = "DELETE FROM " + TABLE_PRODOTTO + " WHERE ID = ?";
		int result;

		try (Connection conn = ds.getConnection();
			 PreparedStatement preparedStatement = conn.prepareStatement(deleteSQL)) {
			preparedStatement.setInt(1, code);
			result = preparedStatement.executeUpdate();
		}
		// true solo se è stata cancellata almeno una riga
		return result != 0;
	}


}
