package Model.Interface;

import java.sql.SQLException;

import Model.Bean.UserBean;

public interface UserModel_intf {

    public void AddUser(UserBean user) throws SQLException ;
    
    public UserBean getUserTramiteCredenziali(String username,String pwd) throws SQLException ;
}
