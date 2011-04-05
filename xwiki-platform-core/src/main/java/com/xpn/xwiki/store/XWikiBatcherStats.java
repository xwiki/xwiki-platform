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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XWikiBatcherStats
{
    private static final Log log = LogFactory.getLog(XWikiBatcherStats.class);

    private List sqlList = new ArrayList();

    private List recentSqlList = new ArrayList();

    private boolean resetOnNextSQL = false;

    private int preparedSQLCounter = 0;

    private int executeBatchCounter = 0;

    private int abortBatchCounter = 0;

    private int resultSetCounter = 0;

    private int addToBatchCounter = 0;

    public void resetStats()
    {
        sqlList = new ArrayList();
        preparedSQLCounter = 0;
        executeBatchCounter = 0;
        abortBatchCounter = 0;
        resultSetCounter = 0;
        addToBatchCounter = 0;
    }

    public List getSqlList()
    {
        return sqlList;
    }

    public List getRecentSqlList()
    {
        return recentSqlList;
    }

    public void resetRecentSqlList()
    {
        recentSqlList = new ArrayList();
    }

    public void addToSqlList(String sql)
    {
        if (resetOnNextSQL) {
            resetRecentSqlList();
            resetOnNextSQL = false;
        }
        this.recentSqlList.add(sql);
        this.sqlList.add(sql);
    }

    public void resetOnNextSQL()
    {
        resetOnNextSQL = true;
    }

    public int getPreparedSQLCounter()
    {
        return preparedSQLCounter;
    }

    public void incrementPreparedSQLCounter()
    {
        this.preparedSQLCounter++;
    }

    public int getExecuteBatchCounter()
    {
        return executeBatchCounter;
    }

    public void incrementExecuteBatchCounter()
    {
        this.executeBatchCounter++;
    }

    public int getAbortBatchCounter()
    {
        return abortBatchCounter;
    }

    public void incrementAbortBatchCounter()
    {
        this.abortBatchCounter++;
    }

    public int getResultSetCounter()
    {
        return resultSetCounter;
    }

    public void incrementResultSetCounter()
    {
        this.resultSetCounter++;
    }

    public int getAddToBatchCounter()
    {
        return addToBatchCounter;
    }

    public void incrementAddToBatchCounter()
    {
        this.addToBatchCounter++;
    }

    public void printSQLList(PrintStream out)
    {
        out.println("SQL: number of queries " + sqlList.size());
        for (int i = 0; i < sqlList.size(); i++) {
            out.println("SQL: " + sqlList.get(i));
        }
        out.flush();
    }

    public void logSQLList()
    {
        if (log.isDebugEnabled()) {
            log.debug("SQL: number of queries " + sqlList.size());
            for (int i = 0; i < sqlList.size(); i++) {
                log.debug("SQL: " + sqlList.get(i));
            }
        }
    }
}
