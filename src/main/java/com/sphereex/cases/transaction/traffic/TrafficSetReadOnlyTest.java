package com.sphereex.cases.transaction.traffic;

import com.sphereex.core.AutoTest;
import com.sphereex.core.CaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@AutoTest
public class TrafficSetReadOnlyTest extends TrafficBaseTest {
    
    private  final Logger logger = LoggerFactory.getLogger(TrafficSetReadOnlyTest.class);
    
    public TrafficSetReadOnlyTest() {
        CaseInfo caseInfo = new CaseInfo();
        caseInfo.setName("TrafficSetReadOnlyTest");
        caseInfo.setFeature("transaction");
        caseInfo.setTag("Traffic");
        caseInfo.setStatus(false);
        caseInfo.setMessage("this is a traffic test for set transaction" +
                "1. one DB have only one connection" +
                "2. session A run 'set session transaction read only', close session A" +
                "3. session B run 'update' successful");
        setCaseInfo(caseInfo);
    }
    
    @Override
    public void pre() throws Exception {
        super.pre();
        Connection conn = getDataSource().getConnection();
        Statement stmt;
        Statement stmt1;
        Statement stmt2;
        stmt = conn.createStatement();
        stmt.executeUpdate("drop table if exists account;");
        stmt.close();
        stmt1 = conn.createStatement();
        stmt1.executeUpdate("create table account(id int primary key , balance int not null);");
        stmt1.close();
        stmt2 = conn.createStatement();
        stmt2.executeUpdate("insert into account(id,balance) values (1,0),(2,100);");
        stmt2.close();
        conn.close();
    }
    
    @Override
    public void run() throws Exception {
        super.run();
        step1();
        step2();
    }
    
    private void step1() throws Exception {
        Connection conn = getDataSource().getConnection();
        conn.setReadOnly(true);
        Statement statement1 = conn.createStatement();
        ResultSet rs = statement1.executeQuery("select * from account;");
        while (rs.next()) {
            int id = rs.getInt("id");
            int balance = rs.getInt("balance");
            if (id == 1) {
                if (balance != 0) {
                    throw new Exception(String.format("balance is %d, should be 0", balance));
                }
            }
            if (id == 2) {
                if (balance != 100) {
                    throw new Exception(String.format("balance is %d, should be 100", balance));
                }
            }
        }
        Statement statement2 = conn.createStatement();
        try {
            statement2.execute("update account set balance=100 where id=2;");
            throw new Exception("update run success, should failed");
        } catch (SQLException e) {
            logger.info("update failed for expect");
        }
        statement1.close();
        statement2.close();
        conn.close();
    }
    
    private void step2() throws Exception {
        Connection conn = getDataSource().getConnection();
        Statement statement1 = conn.createStatement();
        ResultSet rs = statement1.executeQuery("select * from account");
        while (rs.next()) {
            int id = rs.getInt("id");
            int balance = rs.getInt("balance");
            if (id == 1) {
                if (balance != 0) {
                    throw new Exception(String.format("balance is %d, should be 0", balance));
                }
            }
            if (id == 2) {
                if (balance != 100) {
                    throw new Exception(String.format("balance is %d, should be 100", balance));
                }
            }
        }
        Statement statement2 = conn.createStatement();
        statement2.executeUpdate("update account set balance=101 where id=2;");
        statement2.close();
        Statement statement3 = conn.createStatement();
        ResultSet r3 = statement3.executeQuery("select * from account where id=2");
        if (!r3.next()) {
            throw new Exception("update run failed, should success");
        }
        int balanceend = r3.getInt("balance");
        if (balanceend != 101) {
            throw new Exception("update run failed, should success");
        }
    }
}
