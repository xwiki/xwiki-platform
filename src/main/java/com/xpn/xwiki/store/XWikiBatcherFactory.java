package com.xpn.xwiki.store;

import org.hibernate.jdbc.BatchingBatcherFactory;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.jdbc.BatchingBatcher;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 25 sept. 2005
 * Time: 12:02:10
 * To change this template use File | Settings | File Templates.
 */
public class XWikiBatcherFactory extends BatchingBatcherFactory {
    public Batcher createBatcher(JDBCContext jdbcContext) {
        return new XWikiBatcher( jdbcContext );
    }
}
