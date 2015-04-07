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
 */
package com.xpn.xwiki.store;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XWikiBatcherStats
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiBatcherStats.class);

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
        this.sqlList = new ArrayList();
        this.preparedSQLCounter = 0;
        this.executeBatchCounter = 0;
        this.abortBatchCounter = 0;
        this.resultSetCounter = 0;
        this.addToBatchCounter = 0;
    }

    public List getSqlList()
    {
        return this.sqlList;
    }

    public List getRecentSqlList()
    {
        return this.recentSqlList;
    }

    public void resetRecentSqlList()
    {
        this.recentSqlList = new ArrayList();
    }

    public void addToSqlList(String sql)
    {
        if (this.resetOnNextSQL) {
            resetRecentSqlList();
            this.resetOnNextSQL = false;
        }
        this.recentSqlList.add(sql);
        this.sqlList.add(sql);
    }

    public void resetOnNextSQL()
    {
        this.resetOnNextSQL = true;
    }

    public int getPreparedSQLCounter()
    {
        return this.preparedSQLCounter;
    }

    public void incrementPreparedSQLCounter()
    {
        this.preparedSQLCounter++;
    }

    public int getExecuteBatchCounter()
    {
        return this.executeBatchCounter;
    }

    public void incrementExecuteBatchCounter()
    {
        this.executeBatchCounter++;
    }

    public int getAbortBatchCounter()
    {
        return this.abortBatchCounter;
    }

    public void incrementAbortBatchCounter()
    {
        this.abortBatchCounter++;
    }

    public int getResultSetCounter()
    {
        return this.resultSetCounter;
    }

    public void incrementResultSetCounter()
    {
        this.resultSetCounter++;
    }

    public int getAddToBatchCounter()
    {
        return this.addToBatchCounter;
    }

    public void incrementAddToBatchCounter()
    {
        this.addToBatchCounter++;
    }

    public void printSQLList(PrintStream out)
    {
        out.println("SQL: number of queries " + this.sqlList.size());
        for (int i = 0; i < this.sqlList.size(); i++) {
            out.println("SQL: " + this.sqlList.get(i));
        }
        out.flush();
    }

    public void logSQLList()
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL: number of queries " + this.sqlList.size());
            for (int i = 0; i < this.sqlList.size(); i++) {
                LOGGER.debug("SQL: " + this.sqlList.get(i));
            }
        }
    }
}
