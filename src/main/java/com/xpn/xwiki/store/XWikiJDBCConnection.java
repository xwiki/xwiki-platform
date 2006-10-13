/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author ludovic
 */
package com.xpn.xwiki.store;

import java.sql.*;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 25 sept. 2005
 * Time: 02:58:49
 * To change this template use File | Settings | File Templates.
 */
public class XWikiJDBCConnection implements Connection {
    private Connection connection;
    private int countStatements = 0;
    private int countCalls = 0;
    private int countManagement = 0;

    public XWikiJDBCConnection(Connection connection) {
        this.setConnection(connection);
    }

    public Statement createStatement() throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().createStatement();
    }

    public PreparedStatement prepareStatement(String s) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().prepareStatement(s);
    }

    public CallableStatement prepareCall(String s) throws SQLException {
        countCalls = getCountCalls() + 1;
        return getConnection().prepareCall(s);
    }

    public String nativeSQL(String s) throws SQLException {
        return getConnection().nativeSQL(s);
    }

    public void setAutoCommit(boolean b) throws SQLException {
        countManagement = getCountManagement() + 1;
        getConnection().setAutoCommit(b);
    }

    public boolean getAutoCommit() throws SQLException {
        countManagement = getCountManagement() + 1;
        return getConnection().getAutoCommit();
    }

    public void commit() throws SQLException {
        countManagement = getCountManagement() + 1;
        getConnection().commit();
    }

    public void rollback() throws SQLException {
        countManagement = getCountManagement() + 1;
        getConnection().rollback();
    }

    public void close() throws SQLException {
        countManagement = getCountManagement() + 1;
        getConnection().close();
    }

    public boolean isClosed() throws SQLException {
        return getConnection().isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return getConnection().getMetaData();
    }

    public void setReadOnly(boolean b) throws SQLException {
        getConnection().setReadOnly(b);
    }

    public boolean isReadOnly() throws SQLException {
        return getConnection().isReadOnly();
    }

    public void setCatalog(String s) throws SQLException {
        countManagement = getCountManagement() + 1;
        getConnection().setCatalog(s);
    }

    public String getCatalog() throws SQLException {
        return getConnection().getCatalog();
    }

    public void setTransactionIsolation(int i) throws SQLException {
        getConnection().setTransactionIsolation(i);
    }

    public int getTransactionIsolation() throws SQLException {
        return getConnection().getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return getConnection().getWarnings();
    }

    public void clearWarnings() throws SQLException {
        getConnection().clearWarnings();
    }

    public Statement createStatement(int i, int i1) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().createStatement(i, i1);
    }

    public PreparedStatement prepareStatement(String s, int i, int i1) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().prepareStatement(s, i, i1);
    }

    public CallableStatement prepareCall(String s, int i, int i1) throws SQLException {
        countCalls = getCountCalls() + 1;
        return getConnection().prepareCall(s, i, i1);
    }


    public Map getTypeMap() throws SQLException {
        return getConnection().getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        getConnection().setTypeMap(map);
    }


    public void setHoldability(int i) throws SQLException {
        getConnection().setHoldability(i);
    }

    public int getHoldability() throws SQLException {
        return getConnection().getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return getConnection().setSavepoint();
    }

    public Savepoint setSavepoint(String s) throws SQLException {
        return getConnection().setSavepoint(s);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        countManagement = getCountManagement() + 1;
        getConnection().rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        getConnection().releaseSavepoint(savepoint);
    }

    public Statement createStatement(int i, int i1, int i2) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().createStatement(i, i1, i2);
    }

    public PreparedStatement prepareStatement(String s, int i, int i1, int i2) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().prepareStatement(s, i, i1, i2);
    }

    public CallableStatement prepareCall(String s, int i, int i1, int i2) throws SQLException {
        countCalls = getCountCalls() + 1;
        return getConnection().prepareCall(s, i, i1, i2);
    }

    public PreparedStatement prepareStatement(String s, int i) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().prepareStatement(s, i);
    }

    public PreparedStatement prepareStatement(String s, int[] ints) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().prepareStatement(s, ints);
    }

    public PreparedStatement prepareStatement(String s, String[] strings) throws SQLException {
        countStatements = getCountStatements() + 1;
        return getConnection().prepareStatement(s, strings);
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public int getCountStatements() {
        return countStatements;
    }

    public int getCountCalls() {
        return countCalls;
    }

    public int getCountManagement() {
        return countManagement;
    }

    public int getCountTotal() {
        return getCountCalls() + getCountStatements() + getCountManagement();
    }

    public void resetCounters() {
        countCalls = 0;
        countStatements = 0;
        countManagement = 0;
    }
}
