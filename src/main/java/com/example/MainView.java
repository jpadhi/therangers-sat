package com.example;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

import java.sql.*;

@JavaScript("./script.js")
@Route
public class MainView extends Div {

    public MainView() {
        getElement().executeJs("displayuser($0,$1)", "client", getElement());
    }

    @ClientCallable
    public void displayuser(String user_id) {
        System.out.println("Hi, " + user_id);
        System.out.println("Inside DisplayUserDetails");
        String sqlquery="";
        if ( user_id.equals("space_admin") ){
            sqlquery = "select allocated_to,date_allocate, count(*), group_concat(seat_id, ',') from cs_sat.allocatedseats group by allocated_to, date_allocate order by allocated_to;";
        }else {
            sqlquery = "select allocated_to,date_allocate, count(*), group_concat(seat_id, ',') from cs_sat.allocatedseats " +
                    "where allocated_to LIKE (select concat(oe_code,'%') from cs_sat.employee as e " +
                    "where emp_id = '" + user_id + "') group by allocated_to, date_allocate;";
        }
        System.out.println(sqlquery);
        Connection con = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mysql?useSSL=false","root","admin");
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sqlquery);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(", \t\t");
                    String columnValue = rs.getString(i);
                    System.out.print(columnValue);
                }
                System.out.println("");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
