package Model;

import Model.Bean.ImgBean;
import Model.Bean.ProductBean;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ProductModel_impl implements  ProductModel{

    private static final String TABLE_PRODOTTO = "prodotto";
    private static final String TABLE_IMG = "immagine";

    @Override
    public void doSave(ProductBean product) throws SQLException {

    }

    @Override
    public boolean doDelete(int code) throws SQLException {
        return false;
    }

    @Override
    public ProductBean doRetrieveByKey(int id) throws SQLException {

        Connection connection = null;
        PreparedStatement pstmt = null;

        ProductBean beanData = new ProductBean();
        List<ImgBean> images = new ArrayList<ImgBean>();


        String selectSQL = "SELECT " + TABLE_PRODOTTO + ".*, " + TABLE_IMG + ".immagine " +
                "FROM " + TABLE_PRODOTTO + " " +
                "JOIN " + TABLE_IMG + " ON " + TABLE_PRODOTTO + ".ID = " + TABLE_IMG + ".PRODOTTO_ID " +
                "WHERE " + TABLE_PRODOTTO + ".ID = ? AND " + TABLE_IMG + ".PRODOTTO_ID = ?";

        try {
            connection = ConnectionPool.getConnection();
            pstmt = connection.prepareStatement(selectSQL);
            pstmt.setInt(1, id);
            pstmt.setInt(2, id);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                beanData.setID(rs.getInt("ID"));
                beanData.setNome(rs.getString("nome"));
                beanData.setDescrizione(rs.getString("descrizione"));
                beanData.setGrammatura(rs.getInt("grammatura"));
                beanData.setCategoria(rs.getString("categoria"));
                beanData.setQTA_magazzino(rs.getInt("QTAmagazzino"));
                beanData.setPrezzo(rs.getInt("prezzo"));
                beanData.setPercentualeSconto(rs.getInt("percentualeSconto"));
                beanData.setColorazione(rs.getString("colorazione"));

                ImgBean img = new ImgBean();

                img.setImg(rs.getBlob("immagine"));
                images.add(img);
                beanData.setImages(images);
            }

            if (beanData.isEmpty()) {
                // bisognerebbe lanciare un eccezione e quindi avere una pagina per le eccezioni
                System.out.println("bean vuoto, la query non ha prodotto risultati");
            }

        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
            } finally {
                ConnectionPool.releaseConnection(connection);
            }
        }
        return beanData;
    }

    @Override
    public Collection<ProductBean> doRetrieveAll(String order) throws SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        Collection<ProductBean> products = new LinkedList<ProductBean>();

        String selectSQL = "SELECT * FROM " + ProductModel_impl.TABLE_PRODOTTO;

        if (order != null && !order.isEmpty()) {
            selectSQL += " ORDER BY " + order;
        }

        try {
            connection = ConnectionPool.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);

            ResultSet rs = preparedStatement.executeQuery();

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

                products.add(bean);
            }

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
            } finally {
                ConnectionPool.releaseConnection(connection);
            }
        }
        return products;
    }

}
