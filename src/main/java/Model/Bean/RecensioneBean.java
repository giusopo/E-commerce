package Model.Bean;

import java.sql.Date;
import java.util.Objects;

public class RecensioneBean {
    private int ID;
    private Date dataInserimento;
    private int votazione;
    private String commento;
    private int PRODOTTO_ID;

    // Costruttore con valori di default
    public RecensioneBean() {
        this.ID = 0;
        this.dataInserimento = new Date(0);
        this.votazione = 0;
        this.commento = "";
        this.PRODOTTO_ID = 0;
    }

    // Getters
    public int getID() {
        return ID;
    }

    public Date getDataInserimento() {
        return dataInserimento;
    }

    public int getVotazione() {
        return votazione;
    }

    public String getCommento() {
        return commento;
    }

    public int getPRODOTTO_ID() {
        return PRODOTTO_ID;
    }

    // Setters
    public void setID(int ID) {
        this.ID = ID;
    }

    public void setDataInserimento(Date dataInserimento) {
        this.dataInserimento = dataInserimento;
    }

    public void setVotazione(int votazione) {
        this.votazione = votazione;
    }

    public void setCommento(String commento) {
        this.commento = commento;
    }

    public void setPRODOTTO_ID(int PRODOTTO_ID) {
        this.PRODOTTO_ID = PRODOTTO_ID;
    }

    // toString
    @Override
    public String toString() {
        return "RecensioneBean{" +
                "ID=" + ID +
                ", dataInserimento=" + dataInserimento +
                ", votazione=" + votazione +
                ", commento='" + commento + '\'' +
                ", PRODOTTO_ID=" + PRODOTTO_ID +
                '}';
    }

    // equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecensioneBean that = (RecensioneBean) o;
        return ID == that.ID &&
                votazione == that.votazione &&
                PRODOTTO_ID == that.PRODOTTO_ID &&
                Objects.equals(dataInserimento, that.dataInserimento) &&
                Objects.equals(commento, that.commento);
    }

}
