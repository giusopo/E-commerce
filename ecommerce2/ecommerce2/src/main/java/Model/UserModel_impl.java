package Model;

import Model.Bean.UserBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserModel_impl implements UserModel {


    @Override
    public void AddUser(UserBean user) {

        Connection connection = null;
        PreparedStatement pstmt = null;

        String sql = "INSERT INTO utente (NOME, COGNOME, USER_NAME, DATA_NASCITA, email, password) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            connection = ConnectionPool.getConnection();

            if (connection != null) {
                pstmt = connection.prepareStatement(sql);

                pstmt.setString(1, user.getNome());
                pstmt.setString(2, user.getCognome());
                pstmt.setString(3, user.getUserName());
                pstmt.setString(4, user.getDataNascita().toString());
                pstmt.setString(5, user.getEmail());
                pstmt.setString(6, user.getPassword());

                //controllo sul numero di righe inserite
                int rowsInserted = pstmt.executeUpdate();

                connection.commit();

                if (rowsInserted > 0) {
                    System.out.println("A new user was inserted successfully!");
                }

            } else {
                System.out.println("problema con la connessione al db");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);

        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) { /* gestione delle eccezioni */ }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) { /* gestione delle eccezioni */ }
            }
        }
    }

    public UserBean getUserBy(String username) {

        Connection connection = null;
        PreparedStatement pstmt = null;

        UserBean user = new UserBean();

        String sql = "SELECT ID, USER_NAME, password FROM utente WHERE USER_NAME = ?";

        try {
            connection = ConnectionPool.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            //connection.commit();

            if (rs.next()) {

                user.setID(rs.getInt("ID"));
                user.setUserName(rs.getString("USER_NAME"));
                user.setPassword(rs.getString("password"));
            } else {

                System.out.println("l'utente "+ username +" non è stato trovato");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);

        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    /* gestione delle eccezioni */
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    /* gestione delle eccezioni */
                }
            }
        }

        return user;
    }
}
