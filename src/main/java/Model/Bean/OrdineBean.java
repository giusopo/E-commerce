package Model.Bean;

import java.util.ArrayList;

public class OrdineBean {
    private int idOrdine;
    private double prezzoTot;
    private int percentualeIva;
    private String dataInoltro;
    private String indirizzoConsegna;
    private ArrayList<ProductBean> prodotti;
    
    // Costruttore
    public OrdineBean() {
        this.idOrdine = 0;
        this.prezzoTot = 0;
        this.percentualeIva = 0;
        this.dataInoltro = "";
        this.indirizzoConsegna = "";
    }

	// Getter e Setter
    public ArrayList<ProductBean> getProdotti() {
        return prodotti;
    }

    public void setProdotti(ArrayList<ProductBean> prodotti) {
        this.prodotti = prodotti;
    }
    
    public int getIdOrdine() {
        return idOrdine;
    }

    public void setIdOrdine(int idOrdine) {
        this.idOrdine = idOrdine;
    }

    public double getPrezzoTot() {
        return prezzoTot;
    }

    public void setPrezzoTot(double prezzoTot) {
        this.prezzoTot = prezzoTot;
    }

    public int getPercentualeIva() {
        return percentualeIva;
    }

    public void setPercentualeIva(int percentualeIva) {
        this.percentualeIva = percentualeIva;
    }

    public String getDataInoltro() {
        return dataInoltro;
    }

    public void setDataInoltro(String dataInoltro) {
        this.dataInoltro = dataInoltro;
    }

    public String getIndirizzoConsegna() {
        return indirizzoConsegna;
    }

    public void setIndirizzoConsegna(String indirizzoConsegna) {
        this.indirizzoConsegna = indirizzoConsegna;
    }
}

