package Model.Bean;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class ProductBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int ID;
    private String nome;
    private String descrizione;
    private double prezzo;
    private double grammatura;
    private String categoria;
    private int QTAmagazzino;
    private int percentualeSconto;
    private String colorazione;

    private List<ImgBean> images;

    public ProductBean() {

        this.ID = -1;
        this.nome = "";
        this.descrizione = "";
        this.prezzo = 0.0;
        this.grammatura = 0.0;
        this.categoria = "";
        this.QTAmagazzino = 0;
        this.percentualeSconto = 0;
        this.colorazione = "";
        this.images = new ArrayList<ImgBean>();
    }


    public String getBase64Image(Blob blob) throws SQLException, IOException {

        byte[] bytes = blob.getBytes(1, (int) blob.length());
        String encoded = Base64.getEncoder().encodeToString(bytes);
        return "data:image/jpeg;base64," + encoded;
    }

    public boolean isEmpty() {
        return this.ID == -1 &&
                this.nome.isEmpty() &&
                this.descrizione.isEmpty() &&
                this.prezzo == 0.0 &&
                this.grammatura == 0.0 &&
                this.categoria.isEmpty() &&
                this.QTAmagazzino == 0 &&
                this.percentualeSconto == 0 &&
                this.colorazione.isEmpty();
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public double getGrammatura() {
        return grammatura;
    }

    public void setGrammatura(double grammatura) {
        this.grammatura = grammatura;
    }

    public List<ImgBean> getImages() {
        return images;
    }


    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getQTA_magazzino() {
        return QTAmagazzino;
    }

    public void setQTA_magazzino(int QTA_magazzino) {
        this.QTAmagazzino = QTA_magazzino;
    }

    public int getPercentualeSconto() {
        return percentualeSconto;
    }

    public void setPercentualeSconto(int percentualeSconto) {
        this.percentualeSconto = percentualeSconto;
    }

    public String getColorazione() {
        return colorazione;
    }

    public void setColorazione(String colorazione) {
        this.colorazione = colorazione;
    }

    public void setImages(List<ImgBean> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "MyClass{" +
                "ID=" + ID +
                ", nome='" + nome + '\'' +
                ", descrizione='" + descrizione + '\'' +
                ", prezzo=" + prezzo +
                ", grammatura=" + grammatura +
                ", categoria='" + categoria + '\'' +
                ", QTA_magazzino=" + QTAmagazzino +
                ", percentualeSconto=" + percentualeSconto +
                ", colorazione='" + colorazione + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProductBean other = (ProductBean) obj;
        return ID == other.ID &&
                Double.compare(other.prezzo, prezzo) == 0 &&
                Double.compare(other.grammatura, grammatura) == 0 &&
                QTAmagazzino == other.QTAmagazzino &&
                percentualeSconto == other.percentualeSconto &&
                Objects.equals(nome, other.nome) &&
                Objects.equals(descrizione, other.descrizione) &&
                Objects.equals(categoria, other.categoria) &&
                Objects.equals(colorazione, other.colorazione);
    }

}
