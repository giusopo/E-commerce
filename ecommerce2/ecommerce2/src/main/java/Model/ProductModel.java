package Model;

import Model.Bean.ProductBean;

import java.sql.SQLException;
import java.util.Collection;

public interface ProductModel {
    void doSave(ProductBean product) throws SQLException;

    boolean doDelete(int code) throws SQLException;

    ProductBean doRetrieveByKey(int code) throws SQLException;

    Collection<ProductBean> doRetrieveAll(String order) throws SQLException;
}