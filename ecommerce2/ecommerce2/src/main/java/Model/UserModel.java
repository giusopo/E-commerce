package Model;

import Model.Bean.UserBean;

public interface UserModel {

    public void AddUser(UserBean user);
    public UserBean getUserBy(String username);
}
