/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emission.testing;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

/**
 *
 * @author genwockz
 */
public class DBconnection {
    
     private static Connection Myconnection;
    
    public static void init() {
    try{ 
        
        Class.forName("com.mysql.jdbc.Driver");
        Myconnection=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3307/desipcdf_ltoemission","desipcdf", "HESpcOSUKbY0");
    }
   
    catch(Exception e){}
    }
    
    public static Connection getConnection(){
        return Myconnection; }
    
    public static void close(ResultSet rs){
       
    
         if(rs!=null){
         try{
             rs.close();
        }
         catch(Exception e){System.out.println(e);}
         
         }
    }
    
    public void Destroy(){
    if(Myconnection!=null){
     
        try{
        Myconnection.close();
        }
        catch(Exception e){}
    
    }
  
}
    
    
    
}
