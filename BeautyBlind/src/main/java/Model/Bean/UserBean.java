package Model.Bean;

import java.sql.Date;

public class UserBean {

    private int ID;
    private String Nome;
    private String Cognome;
    private String UserName;
    private Date DataNascita;
    private String email;
    private String password;
    private String indirizzoBase;
    private String numeroDiTelefono;
    private boolean permessiAdmin;
    private int cartID;

    public UserBean() {

        this.ID = 0;
        this.Nome = "";
        this.Cognome = "";
        this.UserName = "";
        this.DataNascita = new Date(0); // Imposta la data su Epoch (1 gennaio 1970)
        this.email = "";
        this.password = "";
        this.cartID = -1;
        this.numeroDiTelefono = "";
        this.indirizzoBase = "";
        this.permessiAdmin = false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }
    
    public int getCartID() {
        return cartID;
    }

    public void setCartID(int cartID) {
        this.cartID = cartID;
    }

    public String getNome() {
        return Nome;
    }

    public void setNome(String Nome) {
        this.Nome = Nome;
    }

    public String getCognome() {
        return Cognome;
    }

    public void setCognome(String Cognome) {
        this.Cognome = Cognome;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String user) {
        this.UserName = user;
    }

    public Date getDataNascita() {
        return DataNascita;
    }

    public void setDataNascita(Date DataNascita) {
        this.DataNascita = DataNascita;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIndirizzoBase() {
        return indirizzoBase;
    }

    public void setIndirizzoBase(String indirizzoBase) {
        this.indirizzoBase = indirizzoBase;
    }
    
    public String getNumeroDiTelefono() {
        return numeroDiTelefono;
    }

    public void setNumeroDiTelefono(String numeroDiTelefono) {
        this.numeroDiTelefono = numeroDiTelefono;
    }    
    
    public boolean getPermessiAdmin() {
        return permessiAdmin;
    }

    public void setPermessiAdmin(boolean permessiAdmin) {
        this.permessiAdmin = permessiAdmin;
    }    
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }    
}

