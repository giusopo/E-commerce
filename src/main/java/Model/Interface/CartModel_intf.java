package Model.Interface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import Model.Bean.CartBean;
import Model.Bean.ProductBean;

public interface CartModel_intf {
	public void addProduct(int idUtente,ProductBean prodotto, boolean giaPresente,int qta) throws SQLException;
	
	public void deleteProduct(int idUtente,ProductBean prodotto,boolean isUltimo) throws SQLException;
	
	public Collection<ProductBean> getProducts(int id) throws SQLException;
	
	public void confermaOrdine(String indirizzoConsegna,int idOrdine, int idUtente, List<ProductBean> prodotti) throws SQLException;

}
