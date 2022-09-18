package com.cssat.tool;
import java.sql.*;

public class DBConnection {

    public static DBConnection testDAO = null;

    public static Connection createConnection() throws SQLException, ClassNotFoundException {

        Connection conn = null;
        try {

//            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mysql?useSSL=false","root","admin");
            return conn;

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}