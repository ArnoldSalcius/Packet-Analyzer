package com.WhateverILike;

import java.sql.*;
import java.util.Scanner;


public class Main {

    final static String TABLE_NAME = "ArnoldPackets";
    final static String IP_TABLE_NAME = TABLE_NAME + "IPs";

    public static void main(String[] args) {


        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\ArnoldasS\\IdeaProjects\\TestDB\\testJava.db");
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet tables = dbm.getTables(null,null,IP_TABLE_NAME, null);
            if(!tables.next()) {
                System.out.println("Haha");
                createIPTable(conn);
                populateIPTable(conn);
            }
            //PrintMostFrequentIPs(conn);
            printMenu(conn);

            //probingType(conn,"178.33.33.74", "TCP");









//            Statement statement = conn.createStatement();
//            statement.execute("select UDPDestPort from packetsMain where Source = '173.242.117.189'");
//            ResultSet results = statement.getResultSet();
//            while(results.next()){
//                if(results.getString("UDPDestPort").equals("")){
//                    System.out.println(results.getString("UDPDestPort"));
//                }else{
//                    System.out.println(results.getString("UDPDestPort"));
//                }
//            }
//
            conn.close();

        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }

    private static void PrintMostFrequentIPs(Connection conn){
        try {
            Statement statement = conn.createStatement();
            //statement.execute("select Source, count(*) as count from " + TABLE_NAME + " group by Source order by COUNT(*) DESC;");
            statement.execute("select IP, packet_number from " + IP_TABLE_NAME + " order by packet_number DESC;");
            ResultSet results = statement.getResultSet();

            System.out.println("Source IP" + "\t\t\t # of packets");
            int i =0;
            while (results.next()) {
                i++;
                System.out.println(i + ".  " + results.getString( "IP") + "\t\t\t" + results.getInt("packet_number"));
                //results.next();
            }
            results.close();
            statement.close();
        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }

    public static void printIPsByRate(Connection conn){
        try {
            Statement statement = conn.createStatement();
            //statement.execute("select Source, count(*) as count from " + TABLE_NAME + " group by Source order by COUNT(*) DESC;");
            statement.execute("select IP, rate from " + IP_TABLE_NAME + " where packet_number > 4 order by rate DESC;");
            ResultSet results = statement.getResultSet();

            System.out.println("Source IP" + "\t\t\t Rate packets/sec");
            int i =0;
            while (results.next()) {
                i++;
                System.out.println(i + ".  " + results.getString( "IP") + "\t\t\t" + results.getDouble("rate"));
                //results.next();
            }
            results.close();
            statement.close();
        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }

    }

    private static void createIPTable(Connection conn){
        try {
            Statement statement = conn.createStatement();

            statement.execute("CREATE TABLE IF NOT EXISTS " + IP_TABLE_NAME + "(IP TEXT, Protocol TEXT, packet_number INTEGER, rate REAL, start_time REAL, end_time REAL, probing_type TEXT)");
        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }

    private static void populateIPTable(Connection conn){
        try{
            Statement statement = conn.createStatement();

            statement.execute("select Source, Protocol, count(*) as count from " + TABLE_NAME + " group by Source order by COUNT(*) DESC;");
            ResultSet results = statement.getResultSet();
            while(results.next()) {
                //Set some variables so it is easier to work with

                String IP = results.getString("Source");
                int packetNumber = results.getInt("count");
                String protocol = results.getString("Protocol");
                //Find Start and End times to insert into IP Table
                double startTime = getStart_Time(conn, IP);
                double endTime = getEnd_Time(conn, IP);
                if(startTime > endTime){
                    double temp = startTime;
                    startTime = endTime;
                    endTime = temp;
                }
                double packetRate;
                String probeType;
                if(packetNumber > 5) {
                    probeType = probingType(conn, IP, protocol);
                }else{
                    probeType = "Unknown/Not Enough Packets";
                }
                //calculate packet Rate
                if(packetNumber > 1) {
                    packetRate = (double) (packetNumber) / (endTime - startTime);
                }else{
                    packetRate = 0;
                }
                Statement insertStatement = conn.createStatement();
                insertStatement.execute("INSERT INTO " + IP_TABLE_NAME + "(IP, Protocol, packet_number, rate, start_time, end_time, probing_type) VALUES " +
                       "('" + IP + "', '" + protocol + "', '" + packetNumber + "', '" + packetRate + "', '" + startTime + "', '" + endTime + "', '" + probeType +"');");
                insertStatement.close();
            }
            results.close();
        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }

    private static double getStart_Time(Connection conn, String IP){
        try {
            Statement statement = conn.createStatement();
            statement.execute("select Time from " + TABLE_NAME + " where Source = '" + IP + "' order by time asc limit 1");
            ResultSet result = statement.getResultSet();


            return result.getDouble("Time");

        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
        return 0;
    }

    private static double getEnd_Time(Connection conn, String IP){
        try {
            Statement statement = conn.createStatement();
            statement.execute("select Time from " + TABLE_NAME + " where Source = '" + IP + "' order by time desc limit 1");
            ResultSet result = statement.getResultSet();


            return result.getDouble("Time");

        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
        return 0;
    }

    private static String probingType(Connection conn, String IP, String protocol){
        try{
            int numberOfResults = 0;
            int isHorizontal = 0;
            int isVertical = 0;
            String currentPort;
            String currentIP;

            Statement statement = conn.createStatement();
            if(protocol.equals("TCP")){
                statement.execute("Select Protocol, TCPDestPort, Destination from " + TABLE_NAME + " where Source ='" + IP + "';");
                ResultSet results = statement.getResultSet();
                currentPort = results.getString("TCPDestPort");
                currentIP = results.getString("Destination");
                while(results.next()){
                    numberOfResults++;
                    if(results.getString("TCPDestPort").equals(currentPort)){
                        isHorizontal++;
                        currentPort = results.getString("TCPDestPort");
                    }else{
                        currentPort = results.getString("TCPDestPort");
                    }
                    if(results.getString("Destination").equals(currentIP)){
                        isVertical++;
                        currentIP = results.getString("Destination");
                    }else{
                        currentIP = results.getString("Destination");
                    }

                }
            }
            else{ // if it is not TCP then UDP protocol is used!
                statement.execute("Select Protocol, UDPDestPort, Destination from " + TABLE_NAME + " where Source ='" + IP + "';");
                ResultSet results = statement.getResultSet();
                currentPort = results.getString("UDPDestPort");
                if(currentPort.equals("")){
                    return "Ports Unknown";
                }
                currentIP = results.getString("Destination");
                while(results.next()){
                    numberOfResults++;
                    if(results.getString("UDPDestPort").equals(currentPort)){
                        isHorizontal++;
                        currentPort = results.getString("UDPDestPort");
                    }else{
                        currentPort = results.getString("UDPDestPort");
                    }
                    if(results.getString("Destination").equals(currentIP)){
                        isVertical++;
                        currentIP = results.getString("Destination");
                    }else{
                        currentIP = results.getString("Destination");
                    }
                }

            }

            double verticalSum = (double) (isVertical) / (double) numberOfResults;
            double horizontalSum = (double) (isHorizontal) / (double) (numberOfResults);

            if(verticalSum > 0.9 && horizontalSum > 0.9 && numberOfResults > 10){
                return("Same Port/IP");
            }
            else if(verticalSum > 0.9){
                return("Vertical");
            }else if(horizontalSum > 0.9){
                return("Horizontal");
            }else{
                return("Strobe/Unknown");
            }

        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
        return "error/Unknown";
    }

    private static void printMenu(Connection conn){
        String choice = "";
        while(!choice.equals("q")) {
            System.out.println("\n*********Welcome to Packet analyzer!**********");
            System.out.println("Print Options: \n1. Print all IPs and their information");
            System.out.println("2. Print IPs by packet number");
            System.out.println("3. Print IPs by packet rate");
            System.out.println("4. Print IP packets (requires IP number)");
            System.out.println("Enter 'q' to exit the program");
            System.out.println("Enter your Choice: ");
            Scanner scanner = new Scanner(System.in);
            choice = scanner.nextLine();
            if(choice.equals("1")){
                printIPInfo(conn);

            }else if(choice.equals("2")){
                PrintMostFrequentIPs(conn);
            }else if(choice.equals("3")){
                printIPsByRate(conn);
            }else if(choice.equals("4")){
                System.out.println("Enter IP address: ");
                String IP = scanner.nextLine();
                printIPpackets(conn, IP);
            }else if(!choice.equals("q")){
                System.out.println("Invalid Input!!!");
            }
        }

    }
    private static void printIPpackets(Connection conn, String IP){
        try {
            Statement statement = conn.createStatement();
            statement.execute("select * from " + TABLE_NAME + " where Source='" + IP + "'");
            ResultSet results = statement.getResultSet();
            System.out.println("  #\t\t\tTime\t\t\tSource\t\t\t\t\tDestination\t\t\tProtocol\tUDPSourcePort\tUDPDestSource\tTCPSourcePort\tTCPDestPort\t\tInfo");
            while(results.next()){
                String time, source, destination, protocol, udpSource, udpDest, tcpSource, tcpDest, info, number;
                time = results.getString("Time");
                number = results.getString("No.");
                source = results.getString("Source");
                destination = results.getString("Destination");
                protocol = results.getString("protocol");
                info = results.getString("Info");
                udpDest = results.getString("UDPDestPort");
                udpSource = results.getString("UDPSourcePort");
                tcpDest = results.getString("TCPDestPort");
                tcpSource = results.getString("TCPSourcePort");
                System.out.println(number + " \t\t" + time+ " \t\t" + source + " \t\t\t" + destination+ " \t\t\t" + protocol + " \t\t\t" + udpSource + " \t\t\t" + udpDest + " \t\t\t" + tcpSource + " \t\t\t" + tcpDest + " \t\t\t" + info);
            }

        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }
    private static void printIPInfo(Connection conn){
        try{
            Statement statement = conn.createStatement();
            statement.execute("Select * from " + IP_TABLE_NAME + ";");
            ResultSet results = statement.getResultSet();
            String IP, packet_number, rate, start_time, end_time, probing_type,protocol;
            System.out.println("\tIP\t\t\t\tProtocol\t\tNumber of Packets\t\tRate\t\tStart Time\tEnd Time\tProbing Type");
            int counter = 0;
            while(results.next()){
                counter++;
                IP = results.getString("IP");
                packet_number = results.getString("packet_number");
                protocol = results.getString("Protocol");
                rate = results.getString("rate");
                start_time = results.getString("start_time");
                end_time = results.getString("end_time");
                probing_type = results.getString("probing_type");
                System.out.println(counter + ".  " + IP + "\t\t\t\t" + protocol+ "\t\t" + packet_number + "\t\t" + rate + "\t\t" + start_time + "\t" + end_time + "\t" + probing_type);

            }

        }catch(SQLException e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }

}
