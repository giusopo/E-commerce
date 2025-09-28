package Model.Interface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import Model.Bean.OrdineBean;
import Model.Bean.UserBean;

public interface UserModel_intf {

    public void AddUser(UserBean user) throws SQLException ;
    
    public UserBean getUserTramiteCredenziali(String username,String pwd) throws SQLException ;

	public Collection<UserBean> getUtenti() throws SQLException;
	
    public int getIdCart(String username,String pwd) throws SQLException;
    
    public int getIdCartByUserId(int id) throws SQLException;
    
    public OrdineBean getOrdineById(int idUtente,int idOrdine) throws SQLException;

    public ArrayList<Integer> getAllOrdine(int idUtente) throws SQLException;

}
