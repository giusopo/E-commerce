package admin;

import java.sql.SQLException;

import Model.Bean.ProductBean;

public interface AzioniAdminModel_intf {
	/** Inserisce il prodotto; se riesce restituisce true e scrive nel bean l'id generato dal database. */
	public boolean inserisciProdotto(ProductBean prodotto)  throws SQLException;
	public boolean modificaSconto(int idProdotto,int percentualeSconto)  throws SQLException;

	
}
