package Model.Bean;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Objects;

public class ImgBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int ID;
    private Blob img;
    private int PRODOTTO_ID;

    // Getters
    public int getID() {
        return ID;
    }

    public Blob getImg() {
        return img;
    }

    public int getPRODOTTO_ID() {
        return PRODOTTO_ID;
    }

    // Setters
    public void setID(int ID) {
        this.ID = ID;
    }

    public void setImg(Blob img) {
        this.img = img;
    }

    public void setPRODOTTO_ID(int PRODOTTO_ID) {
        this.PRODOTTO_ID = PRODOTTO_ID;
    }

    // No-args constructor
    public ImgBean() {
        this.ID = 0;
        this.img = null;
        this.PRODOTTO_ID = 0;
    }

    // toString
    @Override
    public String toString() {
        return "ImgBean{" +
                "ID=" + ID +
                ", img=" + img +
                ", PRODOTTO_ID=" + PRODOTTO_ID +
                '}';
    }

    // Equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImgBean imgBean = (ImgBean) o;
        return ID == imgBean.ID &&
                PRODOTTO_ID == imgBean.PRODOTTO_ID &&
                Objects.equals(img, imgBean.img);
    }

}
