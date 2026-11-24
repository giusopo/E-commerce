package Model.Bean;

import java.util.ArrayList;
import java.util.List;
import Model.Bean.*;

public class CartBean {
    private ArrayList<ProductBean> products;

	public CartBean() {
		this.products = new ArrayList<ProductBean>();
	}

    public void addProduct(ProductBean p) {
        products.add(p);
        System.out.println("Aggiunto nel carrello");
    }
    
	public void deleteProduct(ProductBean product) {
		for(ProductBean prod : products) {
			if(prod.getID() == product.getID()) {
				products.remove(prod);
				break;
			}
		}
 	}

    public List<ProductBean> getProducts() {
        return products;
    }
    public void setProducts(ArrayList<ProductBean> carrello) {
        this.products = carrello;
    }
    
    // I prodotti si confrontano per ID: il servlet passa un prodotto appena ricaricato dal DB,
    // che può avere prezzo, sconto o magazzino diversi da quello salvato nel carrello.
    public boolean contiene(ProductBean bean) {
    	for(ProductBean prod : products) {
    		if(prod.getID() == bean.getID())
    			return true;
    	}
    	return false;
    }

    public void incrementaQuantitàOrdinata(ProductBean bean) {

    	for(ProductBean prod : products) {
    		if(prod.getID() == bean.getID()){
    			prod.setQTA_ordinata(prod.getQTA_ordinata() + 1);
    			break;
    		}
    	}
    }
    public void rimuoviSingolaQuantitàOrdinata(ProductBean bean) {

    	for(ProductBean prod : products) {
    		if(prod.getID() == bean.getID()){
    			int quantità = prod.getQTA_ordinata();

    	    	if(quantità <= 1)
    	    		deleteProduct(bean);
    	    	else
    	    		prod.setQTA_ordinata(quantità - 1);
    			break;
    		}
    	}
    }
    
	public int getQuantitàOrdinata(ProductBean product) {
		for(ProductBean prod : products) {
			if(prod.getID() == product.getID()) 
				return prod.getQTA_ordinata();
		}
		
		return -1;
 	}
}
