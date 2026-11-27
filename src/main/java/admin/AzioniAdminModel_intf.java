package admin;

import java.sql.SQLException;

import Model.Bean.ProductBean;

public interface AzioniAdminModel_intf {
	public boolean inserisciProdotto(ProductBean prodotto)  throws SQLException;
	public boolean modificaSconto(int idProdotto,int percentualeSconto)  throws SQLException;

	
}
