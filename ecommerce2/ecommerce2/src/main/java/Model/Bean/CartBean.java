package Model.Bean;

import java.util.List;

public class CartBean {

    private List<ProductBean> products;

    public void setProducts(ProductBean p) {

        products.add(p);
    }

    public List<ProductBean> getProducts() {
        return products;
    }
}
