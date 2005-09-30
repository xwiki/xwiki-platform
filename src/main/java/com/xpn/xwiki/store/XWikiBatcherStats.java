package com.xpn.xwiki.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 25 sept. 2005
 * Time: 11:40:10
 * To change this template use File | Settings | File Templates.
 */
public class XWikiBatcherStats {
    private static final Log log = LogFactory.getLog(XWikiBatcherStats.class);

    private List sqlList = new ArrayList();
    private int preparedSQLCounter = 0;
    private int executeBatchCounter = 0;
    private int abortBatchCounter = 0;
    private int resultSetCounter = 0;
    private int addToBatchCounter = 0;

    public void resetStats() {
        sqlList = new ArrayList();
        preparedSQLCounter = 0;
        executeBatchCounter = 0;
        abortBatchCounter = 0;
        resultSetCounter = 0;
        addToBatchCounter = 0;
    }

    public List getSqlList() {
        return sqlList;
    }

    public void addToSqlList(String sql) {
        this.sqlList.add(sql);
    }

    public int getPreparedSQLCounter() {
        return preparedSQLCounter;
    }

    public void incrementPreparedSQLCounter() {
        this.preparedSQLCounter++;
    }

    public int getExecuteBatchCounter() {
        return executeBatchCounter;
    }

    public void incrementExecuteBatchCounter() {
        this.executeBatchCounter++;
    }

    public int getAbortBatchCounter() {
        return abortBatchCounter;
    }

    public void incrementAbortBatchCounter() {
        this.abortBatchCounter++;
    }

    public int getResultSetCounter() {
        return resultSetCounter;
    }

    public void incrementResultSetCounter() {
        this.resultSetCounter++;
    }

    public int getAddToBatchCounter() {
        return addToBatchCounter;
    }

    public void incrementAddToBatchCounter() {
        this.addToBatchCounter++;
    }

    public void printSQLList(PrintStream out) {
        out.println("SQL: number of queries " + sqlList.size());
        for (int i=0;i<sqlList.size();i++) {
           out.println("SQL: " + sqlList.get(i));
        }
        out.flush();
    }

    public void logSQLList() {
        if (log.isDebugEnabled()) {
        log.debug("SQL: number of queries " + sqlList.size());
        for (int i=0;i<sqlList.size();i++)
           log.debug("SQL: " + sqlList.get(i));
        }
    }
}
