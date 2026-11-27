package Model.Interface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import Model.Bean.CartBean;
import Model.Bean.ProductBean;

public interface CartModel_intf {
	public void addProduct(int idCarrello,ProductBean prodotto, boolean giaPresente,int qta) throws SQLException;

	public void deleteProduct(int idCarrello,ProductBean prodotto,boolean isUltimo) throws SQLException;
	
	public Collection<ProductBean> getProducts(int id) throws SQLException;
	
	/** Conferma l'ordine e restituisce l'id del nuovo carrello dell'utente. */
	public int confermaOrdine(String indirizzoConsegna,int idOrdine, int idUtente, List<ProductBean> prodotti) throws SQLException;

}
