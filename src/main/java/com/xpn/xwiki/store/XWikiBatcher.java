package com.xpn.xwiki.store;

import org.hibernate.jdbc.BatchingBatcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.Dialect;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 25 sept. 2005
 * Time: 10:56:45
 * To change this template use File | Settings | File Templates.
 */
public class XWikiBatcher extends BatchingBatcher {

    private static ThreadLocal sqlStats = new ThreadLocal() {
         protected synchronized Object initialValue() {
             return new XWikiBatcherStats();
         }
     };

    public static XWikiBatcherStats getSQLStats() {
        return (XWikiBatcherStats) sqlStats.get();
    }

    public void addToBatch(int expectedRowCount) throws SQLException, HibernateException {
        getSQLStats().incrementAddToBatchCounter();
        super.addToBatch(expectedRowCount);
    }

    public XWikiBatcher(JDBCContext jdbcContext) {
        super(jdbcContext);
    }

    public void abortBatch(SQLException sqle) {
        getSQLStats().incrementAbortBatchCounter();
        super.abortBatch(sqle);
    }

    public CallableStatement prepareCallableStatement(String sql) throws SQLException, HibernateException {
        addToPreparedSql(sql);
        return super.prepareCallableStatement(sql);
    }

    private void addToPreparedSql(String sql) {
        getSQLStats().addToSqlList(sql);
        getSQLStats().incrementPreparedSQLCounter();
    }

    public PreparedStatement prepareStatement(String sql, boolean getGeneratedKeys) throws SQLException, HibernateException {
        addToPreparedSql(sql);
        return super.prepareStatement(sql, getGeneratedKeys);
    }

    public PreparedStatement prepareSelectStatement(String sql) throws SQLException, HibernateException {
        addToPreparedSql(sql);
        return super.prepareSelectStatement(sql);
    }

    public PreparedStatement prepareQueryStatement(String sql, boolean scrollable, ScrollMode scrollMode) throws SQLException, HibernateException {
        addToPreparedSql(sql);
        return super.prepareQueryStatement(sql, scrollable, scrollMode);
    }

    public CallableStatement prepareCallableQueryStatement(String sql, boolean scrollable, ScrollMode scrollMode) throws SQLException, HibernateException {
        addToPreparedSql(sql);
        return super.prepareCallableQueryStatement(sql, scrollable, scrollMode);
    }

    public PreparedStatement prepareBatchStatement(String sql) throws SQLException, HibernateException {
        addToPreparedSql(sql);
        return super.prepareBatchStatement(sql);
    }

    public CallableStatement prepareBatchCallableStatement(String sql) throws SQLException, HibernateException {
        addToPreparedSql(sql);
        return super.prepareBatchCallableStatement(sql);
    }

    public ResultSet getResultSet(PreparedStatement ps) throws SQLException {
        getSQLStats().incrementResultSetCounter();
        return super.getResultSet(ps);
    }

    public ResultSet getResultSet(CallableStatement ps, Dialect dialect) throws SQLException {
        getSQLStats().incrementResultSetCounter();
        return super.getResultSet(ps, dialect);
    }

    protected void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException {
        getSQLStats().incrementExecuteBatchCounter();
        super.doExecuteBatch(ps);
        getSQLStats().resetOnNextSQL();
    }
}
