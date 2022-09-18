package com.cssat.tool;

import java.sql.*;
import java.util.*;

public class SatRangers {
    static HashMap<String, String> childOEs = new HashMap<String, String>(); // OU, "commaSeparatedListOfChildOUs"
    static HashMap<String, Integer> totalStrength = new HashMap<String, Integer>(); // OU, numberOfSeats
    static HashMap<String, List<String>> oEAssignedSeats = new HashMap<String, List<String>>(); // OU, "ListOfAssignedSeats"

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        Connection con = DBConnection.createConnection();
        Statement stmt = con.createStatement();
        Scanner sc= new Scanner(System.in);

        System.out.print("Space Allocation Tool: Available for the below sites:\n");
        ResultSet rs=stmt.executeQuery("select office_id, site_name from cs_sat.office");
        System.out.print("Site_ID\tSite_Name\n");
        while(rs.next())
            System.out.println(rs.getString(1)+"\t"+rs.getString(2));

        System.out.print("\nEnter Site_ID: ");
        String site_id = sc.next();
        String sqlQuery = "select d.oe_code,count(*) from cs_sat.employee_full as e " +
                "LEFT JOIN cs_sat.department as d ON e.oe_code LIKE concat(d.oe_code,'%') " +
                "where e.office_id = "+site_id+" group by d.oe_code";

        System.out.println("Finding the number of employees: ");
        ResultSet rs1 = stmt.executeQuery(sqlQuery);
        String mainOEs = "";
        HashMap<String, Integer> mainOUs = new HashMap<String, Integer>();
        while(rs1.next()){
            System.out.println(rs1.getString(1)+"  "+rs1.getInt(2));
            mainOUs.put(rs1.getString(1),rs1.getInt(2)); 
            totalStrength.put(rs1.getString(1),rs1.getInt(2));
        }
        mainOEs=String.join(",",mainOUs.keySet());

        // Update Employee table with number of reportees
        sqlQuery = "drop table if exists cs_sat.employee; ";
        PreparedStatement pstmt = con.prepareStatement(sqlQuery);
        pstmt.executeUpdate();
        sqlQuery = "create table cs_sat.employee as select t1.*, t2.num_reportees from cs_sat.employee_full as t1 " +
                "join ( select d.oe_code as oe_code,count(*) as num_reportees from cs_sat.employee_full as e " +
                "LEFT JOIN ( select distinct oe_code from cs_sat.employee_full ) as d ON e.oe_code LIKE concat(d.oe_code,'%') and e.office_id ="+site_id+" " +
                "group by d.oe_code ) as t2 ON t1.oe_code=t2.oe_code;";
        pstmt = con.prepareStatement(sqlQuery);
        pstmt.executeUpdate();

        System.out.println("Processing Main OES: "+mainOEs);

        // Loading child OUs
        sqlQuery = "select t3.moe_code, group_concat(t3.oe_code SEPARATOR ',') as direct_reportee_oe_code from\n" +
                "( select t1.*,t2.manager_oecode as moe_code from cs_sat.employee as t1 left join\n" +
                "( select oe_code, SUBSTRING(oe_code, 1, CHAR_LENGTH(oe_code)-1) as manager_oecode from cs_sat.employee ) t2\n" +
                "on t1.oe_code = t2.oe_code where t1.office_id="+site_id+" ) as t3 group by t3.moe_code;";
        ResultSet rs2 = stmt.executeQuery(sqlQuery);
        while(rs2.next()) {
            childOEs.put(rs2.getString(1).trim(), rs2.getString(2));
            System.out.println("Child OEs of "+rs2.getString(1)+" is "+rs2.getString(2));
        }

        // Populate number of reportees
        sqlQuery = "select oe_code, num_reportees from cs_sat.employee;";
        rs2 = stmt.executeQuery(sqlQuery);
        while(rs2.next()) {
            totalStrength.put(rs2.getString(1), rs2.getInt(2));
            System.out.println("Total Strength of "+rs2.getString(1)+" is "+rs2.getInt(2));
        }

        // Loading total strength for each OU
        sqlQuery = "select oe_code, num_reportees from cs_sat.employee;";
        rs2 = stmt.executeQuery(sqlQuery);
        while(rs2.next()) {
            totalStrength.put(rs2.getString(1), rs2.getInt(2));
            System.out.println("Total Strength of "+rs2.getString(1)+" is "+rs2.getInt(2));
        }

        // Get total available seats for space admins
        List<String> seatList = new ArrayList<String>();
        sqlQuery = "select seat_id from cs_sat.seatall where office_id = "+site_id;
        System.out.println(sqlQuery);
        rs2 = stmt.executeQuery(sqlQuery);
        System.out.println("List of seatIds are:\n");
        while(rs2.next()) {
            seatList.add(rs2.getString(1));
            System.out.println(rs2.getString(1));
        }

        oEAssignedSeats.put("space_admin",seatList);

        processSubOEs(mainOEs,oEAssignedSeats.get("space_admin"));

        System.out.println("\nTotal Seat allocated:\n");
        for (String oecode1: oEAssignedSeats.keySet() ) {
            System.out.println("\n" + oecode1 + "\t" + oEAssignedSeats.get(oecode1));
        }

        for (String oecode: oEAssignedSeats.keySet() ) {
            if ( oecode != "space_admin" && totalStrength.get(oecode) != 1  ) {
                List<String> olst = oEAssignedSeats.get(oecode);
                Collections.sort(olst);
                System.out.println("String: "+oecode+" "+olst);
                int totalAssigned=0;
                for (String oec : childOEs.get(oecode).split(",")) {
                    if (totalStrength.get(oec) != 1) {
                        System.out.println("\tString: " + oec + " " + oEAssignedSeats.get(oec));
                        totalAssigned += oEAssignedSeats.get(oec).size();
                    }
                }
                oEAssignedSeats.put(oecode, olst.subList(totalAssigned,olst.size()));
            }
        }
        for (String oecode: oEAssignedSeats.keySet() ) {
            if ( oecode != "space_admin" && totalStrength.get(oecode) == 1  ) {
                System.out.println(oecode+"\t"+oecode.substring(0,oecode.length()-1));
                oEAssignedSeats.put(oecode,oEAssignedSeats.get(oecode.substring(0,oecode.length()-1)));
            }
        }

        System.out.println("\nTotal Seat after removing:\n");
        for (String oecode1: oEAssignedSeats.keySet() ) {
            System.out.println("\n" + oecode1 + "\t" + oEAssignedSeats.get(oecode1));
        }

        System.out.println("\nOnly for managers:\n");
        con.prepareStatement("truncate table cs_sat.allocatedseats").executeUpdate();
        for (String oecode: oEAssignedSeats.keySet() ) {
            if (oecode != "space_admin" && totalStrength.get(oecode) != 1 ) {
                System.out.println("\n"+oecode+"\t"+oEAssignedSeats.get(oecode));
                for ( String seatid : oEAssignedSeats.get(oecode)) {
                    con.prepareStatement("insert into cs_sat.allocatedseats values ( '"+seatid+"', '"+oecode+"', '20220918')").executeUpdate();
                }
            }
        }

        boolean exitMenu=true;
        while ( exitMenu ){
            System.out.println("\nEnter UserId to see details: ");
            String user_name = sc.next();
            DisplayUserDetails(user_name);

//            System.out.println("\n ");
//            String user_name = sc.next();
//            DisplayUserDetails(user_name);
            exitMenu = false;
        }

        con.close();
    }

    private static void DisplayUserDetails(String user_id) throws SQLException, ClassNotFoundException {
        String sqlquery="";
        if ( user_id.equals("space_admin") ){
            sqlquery = "select allocated_to,date_allocate, count(*), group_concat(seat_id, ',') from cs_sat.allocatedseats group by allocated_to, date_allocate order by allocated_to;";
        }else {
            sqlquery = "select allocated_to,date_allocate, count(*), group_concat(seat_id, ',') from cs_sat.allocatedseats " +
                    "where allocated_to LIKE (select concat(oe_code,'%') from cs_sat.employee as e " +
                    "where emp_id = '" + user_id + "') group by allocated_to, date_allocate;";
        }
            System.out.println(sqlquery);
            Connection con = DBConnection.createConnection();
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
    }

    private static void processSubOEs(String subOEs, List<String> seatList) {
        System.out.println("\n\nInside Function for "+subOEs);
        Collections.sort(seatList);
        int startIndex,sIndex=0;
        for ( String oecode: subOEs.split(",") ) {
            if (totalStrength.get(oecode) == 1) {
                System.out.println("Inside if: "+oecode);
                oEAssignedSeats.put(oecode, seatList);
            }else {
                System.out.println("Inside else: "+oecode);
                int eIndex = sIndex + totalStrength.get(oecode);
                oEAssignedSeats.put(oecode, seatList.subList(sIndex, eIndex));
                sIndex = eIndex;
                processSubOEs(childOEs.get(oecode),oEAssignedSeats.get(oecode));
            }
        }
    }
}
