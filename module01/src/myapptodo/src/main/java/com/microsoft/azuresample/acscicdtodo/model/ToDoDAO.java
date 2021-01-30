package com.microsoft.azuresample.acscicdtodo.model;

import org.springframework.stereotype.Component;
import com.microsoft.azuresample.acscicdtodo.Utils.SqlHelper;
import java.sql.Connection;
import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ToDoDAO {
    static final Logger LOG = LoggerFactory.getLogger(ToDoDAO.class);

    @PostConstruct
    public void init() throws SQLException {
        LOG.info("### INIT of ToDoDAO called.");
        Boolean createTab = false;
        try {
            Connection conn = SqlHelper.GetConnection();
            // check if table exists
            try (PreparedStatement selectStatement = conn.prepareStatement(
                "select count(*) as CNT from tab where tname='TODO'"))
            {
                ResultSet rs = selectStatement.executeQuery();
                rs.next();
                createTab = rs.getInt("CNT") == 0;
                rs.close();
            }finally {
                if(!createTab) conn.close();
            }
            LOG.info("### INIT of ToDoDAO called -> create table: " + createTab.toString());            
            if(createTab){
                //create table
                try (Statement stmt = conn.createStatement())
                {
                    stmt.executeUpdate(
                        "CREATE TABLE TODO(" +
                        " \"ID\" VARCHAR2(50) NOT NULL," +
                        " \"CATEGORY\" VARCHAR2(50) NULL," +
                        " \"COMMENT\" VARCHAR2(500) NULL," +
                        " \"CREATED\" TIMESTAMP(2) NOT NULL," +
                        " \"UPDATED\" TIMESTAMP(2) NOT NULL" +
                        ")");
                }finally {
                    conn.close();
                }
            }
        } catch (SQLException e) {
            LOG.error("ERROR: cannot connect to Server.");
            throw e;
        }
    }

    public List<ToDo> query(){
        List<ToDo> ret = new ArrayList<ToDo>();
        try {
            Connection conn = SqlHelper.GetConnection();
            try (PreparedStatement selectStatement = conn.prepareStatement(
                    "SELECT * FROM todo"))
            {
                ResultSet rs = selectStatement.executeQuery();
                while(rs.next()) {
                    ret.add(new ToDo(
                            rs.getString("ID"),
                            rs.getString("COMMENT"),
                            rs.getString("CATEGORY"),
                            rs.getDate("CREATED"),
                            rs.getDate("UPDATED")
                            ));
                }
                rs.close();
            }finally {
                conn.close();
            }
        } catch (SQLException e) {
            LOG.error("ERROR: cannot connect to Server.");
        }
        return ret;
    }

    public ToDo query(String id){
        ToDo ret = null;
        try {
            Connection conn = SqlHelper.GetConnection();
            try (PreparedStatement selectStatement = conn.prepareStatement(
                    "SELECT * FROM Todo WHERE Id=?"))
            {
                selectStatement.setString(1, id);

                ResultSet rs = selectStatement.executeQuery();
                while(rs.next()) {
                    ret = new ToDo(
                        rs.getString("ID"),
                        rs.getString("COMMENT"),
                        rs.getString("CATEGORY"),
                        rs.getDate("CREATED"),
                        rs.getDate("UPDATED")
                        );
                }
                rs.close();
            }finally {
                conn.close();
            }
        } catch (SQLException e) {
            LOG.error("ERROR: cannot connect to Server.");
        }
        return ret;
    }

    public ToDo create(ToDo item){

        try {
            Connection conn = SqlHelper.GetConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO TODO(ID, \"COMMENT\", CATEGORY, CREATED, UPDATED) VALUES(?,?,?,?,?)"))
            {
                stmt.setString(1, item.getId());
                stmt.setString(2, item.getComment());
                stmt.setString(3, item.getCategory());
                stmt.setDate(4, new java.sql.Date(item.getCreated().getTime()));
                stmt.setDate(5, new java.sql.Date(item.getUpdated().getTime()));
                System.out.println("INSERT: before insert call.");
                stmt.executeUpdate();
            }finally {
                conn.close();
            }
        } catch (SQLException e) {
            LOG.error("ERROR: cannot connect to Server.");
            LOG.error("SQL Err: ", e);
        }

        return item;
    }

    public ToDo update(ToDo item){
        
        try {
            Connection conn = SqlHelper.GetConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE TODO SET \"COMMENT\"=?, CATEGORY=?, UPDATED=? WHERE ID=?"))
            {
                stmt.setString(4, item.getId());
                stmt.setString(1, item.getComment());
                stmt.setString(2, item.getCategory());
                stmt.setDate(3, new java.sql.Date(item.getUpdated().getTime()));
                System.out.println("UPDATE: before update call.");
                stmt.executeUpdate();
            }finally {
                conn.close();
            }
        } catch (SQLException e) {
            LOG.error("ERROR: cannot connect to Server.", e);
        }

        return item;
    }
}