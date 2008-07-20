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
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.StringUtils;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.jcr.JcrUtil;
import com.xpn.xwiki.store.jcr.XWikiJcrSession;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.store.jcr.XWikiJcrBaseStore.JcrCallBack;
import com.xpn.xwiki.store.query.AbstractQuery;

/**
 * Query implementation for Java Content Repository v1.0.
 * @version $Id$
 * @since 1.6M1
 */
public class JcrQuery extends AbstractQuery
{
    /**
     * It is used for access to environment.
     * Injected via component manager.
     */
    private Execution execution;

    protected Execution getExecution()
    {
        return execution;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> execute() throws XWikiException
    {
        XWikiContext context = (XWikiContext) getExecution().getContext().getProperty("xwikicontext");
        XWikiJcrStore store = (XWikiJcrStore) context.getWiki().getNotCacheStore();
        try {
            return (List<T>) store.executeRead(context, new JcrCallBack() {
                public Object doInJcr(XWikiJcrSession session) throws Exception
                {
                    Query query = createQuery(session.getJcrSession());
                    RowIterator ri = query.execute().getRows();
                    // Emulate offset and limit
                    int skip = getOffset();
                    int size = getLimit() > 0 ? getLimit() : Integer.MAX_VALUE;
                    List<T> result = new ArrayList<T>();
                    while (ri.hasNext()) {
                        Row row = ri.nextRow();
                        if (skip>0) {
                            skip--;
                        } else {
                            if (size<=0) break;
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
                    }
                    return result;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(0, 0, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Substitute query parameters to its values.
     * TODO: Use PreparedQuery from JCRv2.
     * @return clear query statement without parameters
     */
    protected String createNativeStatement() {
        int n = getParameters().size();
        String[] vars = new String[n];
        String[] vals = new String[n];
        int count = 0;
        for (Entry<String, Object> e : getParameters().entrySet()) {
            vars[count] = ":{"+e.getKey()+"}";
            vals[count] = getValueAsString(e.getValue());
            ++count;
        }
        return StringUtils.replaceEach(getStatement(), vars, vals);
    }

    protected String getValueAsString(Object val) {
        return val.toString();
    }

    protected Query createQuery(Session session) throws RepositoryException
    {
        return session.getWorkspace().getQueryManager().createQuery(createNativeStatement(), getLanguage());
    }
}
