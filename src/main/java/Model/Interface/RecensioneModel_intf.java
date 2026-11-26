package Model.Interface;

import java.sql.SQLException;
import java.util.ArrayList;

import Model.Bean.RecensioneBean;

public interface RecensioneModel_intf {

	public RecensioneBean getRecensione(int userID, int prodID) throws SQLException;
	
	public ArrayList<RecensioneBean> getAllRecensioni(int prodID) throws SQLException;
	
	public void modificaRecensione(int userID, int punteggio, String commento) throws SQLException;
	
	public void addRecensione(int userID, int prodID, int punteggio, String commento) throws SQLException;
}
