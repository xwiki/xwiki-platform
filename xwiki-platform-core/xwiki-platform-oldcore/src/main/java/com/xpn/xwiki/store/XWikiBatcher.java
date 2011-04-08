/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.store;

/*
 import org.hibernate.jdbc.BatchingBatcher;
 import org.hibernate.jdbc.JDBCContext;
 import org.hibernate.jdbc.ConnectionManager;
 import org.hibernate.HibernateException;
 import org.hibernate.ScrollMode;
 import org.hibernate.Interceptor;
 import org.hibernate.dialect.Dialect;

 import java.sql.SQLException;
 import java.sql.ResultSet;
 import java.sql.PreparedStatement;
 import java.sql.CallableStatement;
 */

public class XWikiBatcher
{
}

/*
 * private static ThreadLocal sqlStats = new ThreadLocal() { protected synchronized Object initialValue() { return new
 * XWikiBatcherStats(); } }; public static XWikiBatcherStats getSQLStats() { return (XWikiBatcherStats) sqlStats.get();
 * } public void addToBatch(int expectedRowCount) throws SQLException, HibernateException {
 * getSQLStats().incrementAddToBatchCounter(); super.addToBatch(expectedRowCount); } public
 * XWikiBatcher(ConnectionManager cmgr, Interceptor interceptor) { super(cmgr, interceptor); } public void
 * abortBatch(SQLException sqle) { getSQLStats().incrementAbortBatchCounter(); super.abortBatch(sqle); } public
 * CallableStatement prepareCallableStatement(String sql) throws SQLException, HibernateException {
 * addToPreparedSql(sql); return super.prepareCallableStatement(sql); } private void addToPreparedSql(String sql) {
 * getSQLStats().addToSqlList(sql); getSQLStats().incrementPreparedSQLCounter(); } public PreparedStatement
 * prepareStatement(String sql, boolean getGeneratedKeys, String[] param) throws SQLException, HibernateException {
 * addToPreparedSql(sql); return super.prepareStatement(sql, getGeneratedKeys, param); } public PreparedStatement
 * prepareSelectStatement(String sql) throws SQLException, HibernateException { addToPreparedSql(sql); return
 * super.prepareSelectStatement(sql); } public PreparedStatement prepareQueryStatement(String sql, boolean scrollable,
 * ScrollMode scrollMode) throws SQLException, HibernateException { addToPreparedSql(sql); return
 * super.prepareQueryStatement(sql, scrollable, scrollMode); } public CallableStatement
 * prepareCallableQueryStatement(String sql, boolean scrollable, ScrollMode scrollMode) throws SQLException,
 * HibernateException { addToPreparedSql(sql); return super.prepareCallableQueryStatement(sql, scrollable, scrollMode);
 * } public PreparedStatement prepareBatchStatement(String sql) throws SQLException, HibernateException {
 * addToPreparedSql(sql); return super.prepareBatchStatement(sql); } public CallableStatement
 * prepareBatchCallableStatement(String sql) throws SQLException, HibernateException { addToPreparedSql(sql); return
 * super.prepareBatchCallableStatement(sql); } public ResultSet getResultSet(PreparedStatement ps) throws SQLException {
 * getSQLStats().incrementResultSetCounter(); return super.getResultSet(ps); } public ResultSet
 * getResultSet(CallableStatement ps, Dialect dialect) throws SQLException { getSQLStats().incrementResultSetCounter();
 * return super.getResultSet(ps, dialect); } protected void doExecuteBatch(PreparedStatement ps) throws SQLException,
 * HibernateException { getSQLStats().incrementExecuteBatchCounter(); super.doExecuteBatch(ps);
 * getSQLStats().resetOnNextSQL(); } }
 */
