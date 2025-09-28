package Model.Interface;

import java.sql.SQLException;
import java.util.Collection;

import Model.Bean.ProductBean;

public interface ProductModel_intf {
	

	public boolean doDelete(int code) throws SQLException;

	public StringBuilder doRetrieveByInput(String string) throws SQLException;
	
	public ProductBean doRetrieveByKey(int code) throws SQLException;
	
	public Collection<ProductBean> doRetrieveAll(String order, int offset, int limit) throws SQLException;
		
	public Collection<ProductBean> doRetrieveByCat(String category, String order) throws SQLException;

	public Collection<ProductBean> doRetrieveBySubCat(String category, String subcategory, String order) throws SQLException;
}
