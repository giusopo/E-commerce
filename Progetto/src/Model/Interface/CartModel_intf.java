package Model.Interface;

import java.sql.SQLException;
import java.util.Collection;

import Model.Bean.ProductBean;

public interface CartModel_intf {
	public void addProduct(int idUtente,ProductBean prodotto, boolean giaPresente) throws SQLException;
	public void deleteProduct(int idUtente,ProductBean prodotto,boolean isUltimo) throws SQLException;
	public Collection<ProductBean> getProducts(int id) throws SQLException;
}
