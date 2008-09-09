package org.xwiki.query.xwql;

import java.util.List;
import java.util.Map.Entry;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;

public class XWQLQueryExecutor implements QueryExecutor, Composable
{
    private QueryTranslator translator;

    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public QueryManager getQueryManager() throws ComponentLookupException
    {
        // we can't inject QueryManager because of cyclic dependency.
        return (QueryManager) componentManager.lookup(QueryManager.ROLE);
    }

    public <T> List<T> execute(Query query) throws QueryException
    {
        Query nativeQuery;
        try {
            nativeQuery = getQueryManager().createQuery(
                translator.translate(query.getStatement()),
                translator.getOutputLanguage());
            nativeQuery.setLimit(query.getLimit());
            nativeQuery.setOffset(query.getOffset());
            nativeQuery.setWiki(query.getWiki());
            for (Entry<String, Object> e : query.getNamedParameters().entrySet()) {
                nativeQuery.bindValue(e.getKey(), e.getValue());
            }
            for (Entry<Integer, Object> e : query.getPositionalParameters().entrySet()) {
                nativeQuery.bindValue(e.getKey(), e.getValue());
            }
            return nativeQuery.execute();
        } catch (Exception e) {
            if (e instanceof QueryException)
                throw (QueryException)e;
            throw new QueryException("Exception while translate XWQL query to " + translator.getOutputLanguage(), query, e);
        }
    }

    public QueryTranslator getTranslator()
    {
        return translator;
    }
}
