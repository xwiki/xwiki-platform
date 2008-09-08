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
package com.xpn.xwiki.store.jcr.query;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.StringUtils;
import org.xwiki.context.Execution;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.jcr.JcrUtil;
import com.xpn.xwiki.store.jcr.XWikiJcrSession;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.store.jcr.XWikiJcrBaseStore.JcrCallBack;

/**
 * QueryManager implementation for Java Content Repository v1.0.
 * @version $Id$
 * @since 1.6M1
 */
public class JcrQueryExecutor implements QueryExecutor
{
    /**
     * Used for get named queries.
     */
    private ResourceBundle queriesBundle = ResourceBundle.getBundle("JcrQueries");

    /**
     * Used for creating JcrQuery.
     */
    private Execution execution;

    /**
     * Store component for execute the query.
     */
    private XWikiJcrStore store;

    /**
     * @return Execution object, used for access to store system.
     */
    protected Execution getExecution()
    {
        return execution;
    }

    protected XWikiJcrStore getStore()
    {
        return store;
    }

    protected XWikiContext getContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> execute(final Query query) throws QueryException
    {
        String olddatabase = getContext().getDatabase();
        try {
            if (query.getWiki()!=null) {
                getContext().setDatabase(query.getWiki());
            }
            return (List<T>) store.executeRead(getContext(), new JcrCallBack() {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    javax.jcr.query.Query jcrquery = createQuery(query, session.getJcrSession());
                    RowIterator ri = jcrquery.execute().getRows();
                    if (query.getOffset()>0)
                        ri.skip(query.getOffset());
                    int size = query.getLimit() > 0 ? query.getLimit() : Integer.MAX_VALUE;
                    List<T> result = new ArrayList<T>();
                    while (ri.hasNext() && size > 0) {
                        Row row = ri.nextRow();
                        Value[] values = row.getValues();
                        Object[] el = new Object[values.length]; 
                        for (int i=0; i<values.length; i++) {
                            el[i] = JcrUtil.fromValue( values[i] );
                        }
                        if (values.length==1) {
                            result.add((T) el[0]);
                        } else {
                            result.add((T) el);
                        }
                        size--;
                    }
                    return result;
                }
            });
        } catch (Exception e) {
            throw new QueryException("Exception while excecute the query", query, e);
        } finally {
            getContext().setDatabase(olddatabase);
        }
    }

    /**
     * Substitute query parameters to its values.
     * TODO: Use PreparedQuery from JCRv2.
     * @param query 
     * @return clear query statement without parameters
     */
    protected String createNativeStatement(Query query) {
        String statement = query.getStatement();
        if (query.isNamed()) {
            statement = this.queriesBundle.getString(statement);
        }
        int n = query.getNamedParameters().size();
        String[] vars = new String[n];
        String[] vals = new String[n];
        int count = 0;
        for (Entry<String, Object> e : query.getNamedParameters().entrySet()) {
            vars[count] = ":{"+e.getKey()+"}";
            vals[count] = getValueAsString(e.getValue());
            ++count;
        }
        return StringUtils.replaceEach(statement, vars, vals);
    }

    protected String getValueAsString(Object val) {
        return "'"+val.toString()+"'";
    }

    protected javax.jcr.query.Query createQuery(Query query, Session session) throws RepositoryException
    {
        return session.getWorkspace().getQueryManager().createQuery(createNativeStatement(query), query.getLanguage());
    }
}
