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
package com.xpn.xwiki.store.hibernate.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.query.NativeQuery;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryParameter;
import org.xwiki.query.SecureQuery;
import org.xwiki.query.WrappingQuery;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.util.Util;

/**
 * QueryExecutor implementation for Hibernate Store.
 *
 * @version $Id$
 * @since 1.6M1
 */
@Component
@Named("hql")
@Singleton
public class HqlQueryExecutor implements QueryExecutor, Initializable
{
    /**
     * Path to Hibernate mapping with named queries. Configured via component manager.
     */
    private static final String MAPPING_PATH = "queries.hbm.xml";

    private static final String ESCAPE_LIKE_PARAMETERS_FILTER = "escapeLikeParameters";

    @Inject
    private HibernateStore hibernate;

    /**
     * Used for access to XWikiContext.
     */
    @Inject
    private Execution execution;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private volatile Set<String> allowedNamedQueries;

    @Override
    public void initialize() throws InitializationException
    {
        Configuration configuration = this.hibernate.getConfiguration();

        configuration.addInputStream(Util.getResourceAsStream(MAPPING_PATH));
    }

    private Set<String> getAllowedNamedQueries()
    {
        if (this.allowedNamedQueries == null) {
            synchronized (this) {
                if (this.allowedNamedQueries == null) {
                    this.allowedNamedQueries = new HashSet<>();

                    // Gather the list of allowed named queries
                    Collection<NamedQueryDefinition> namedQueries =
                        this.hibernate.getConfigurationMetadata().getNamedQueryDefinitions();
                    for (NamedQueryDefinition definition : namedQueries) {
                        if (HqlQueryUtils.isSafe(definition.getQuery())) {
                            this.allowedNamedQueries.add(definition.getName());
                        }
                    }
                }
            }
        }

        return this.allowedNamedQueries;
    }

    /**
     * @param statementString the statement to evaluate
     * @return true if the select is allowed for user without PR
     */
    protected static boolean isSafeSelect(String statementString)
    {
        return HqlQueryUtils.isShortFormStatement(statementString) || HqlQueryUtils.isSafe(statementString);
    }

    protected void checkAllowed(final Query query) throws QueryException
    {
        if (query instanceof SecureQuery && ((SecureQuery) query).isCurrentAuthorChecked()) {
            if (!this.authorization.hasAccess(Right.PROGRAM)) {
                if (query.isNamed() && !getAllowedNamedQueries().contains(query.getStatement())) {
                    throw new QueryException("Named queries requires programming right", query, null);
                }

                if (!isSafeSelect(query.getStatement())) {
                    throw new QueryException("The query requires programming right", query, null);
                }
            }
        }
    }

    @Override
    public <T> List<T> execute(final Query query) throws QueryException
    {
        // Make sure the query is allowed in the current context
        checkAllowed(query);

        String oldDatabase = getContext().getWikiId();
        try {
            if (query.getWiki() != null) {
                getContext().setWikiId(query.getWiki());
            }
            return getStore().executeRead(getContext(), session -> {
                org.hibernate.query.Query<T> hquery = createHibernateQuery(session, query);

                List<T> results = hquery.list();
                if (query.getFilters() != null && !query.getFilters().isEmpty()) {
                    for (QueryFilter filter : query.getFilters()) {
                        results = filter.filterResults(results);
                    }
                }
                return results;
            });
        } catch (XWikiException e) {
            throw new QueryException("Exception while executing query", query, e);
        } finally {
            getContext().setWikiId(oldDatabase);
        }
    }

    protected <T> org.hibernate.query.Query<T> createHibernateQuery(Session session, Query query)
    {
        org.hibernate.query.Query<T> hquery;

        Query filteredQuery = query;
        if (!filteredQuery.isNamed()) {
            // For non-named queries, convert the short form into long form before we apply the filters.
            filteredQuery = new WrappingQuery(filteredQuery)
            {
                @Override
                public String getStatement()
                {
                    // handle short queries
                    return completeShortFormStatement(getWrappedQuery().getStatement());
                }
            };
            filteredQuery = filterQuery(filteredQuery, Query.HQL);
            hquery = session.createQuery(filteredQuery.getStatement());
            populateParameters(hquery, filteredQuery);
        } else {
            hquery = createNamedHibernateQuery(session, filteredQuery);
        }

        return hquery;
    }

    private Query filterQuery(Query query, String language)
    {
        Query filteredQuery = query;

        // If there are Query parameters of type QueryParameter then, for convenience, automatically add the
        // "escapeLikeParameters" filter (if not already there)
        addEscapeLikeParametersFilter(query);

        if (query.getFilters() != null && !query.getFilters().isEmpty()) {
            for (QueryFilter filter : query.getFilters()) {
                // Step 1: For backward-compatibility reasons call #filterStatement() first
                String filteredStatement = filter.filterStatement(filteredQuery.getStatement(), language);
                // Prevent unnecessary creation of WrappingQuery objects when the QueryFilter doesn't modify the
                // statement.
                if (!filteredStatement.equals(filteredQuery.getStatement())) {
                    filteredQuery = new WrappingQuery(filteredQuery)
                    {
                        @Override
                        public String getStatement()
                        {
                            return filteredStatement;
                        }
                    };
                }
                // Step 2: Run #filterQuery()
                filteredQuery = filter.filterQuery(filteredQuery);
            }
        }
        return filteredQuery;
    }

    private void addEscapeLikeParametersFilter(Query query)
    {
        if (!hasQueryParametersType(query)) {
            return;
        }

        // Find the component class for the "escapeLikeParameters" filter
        QueryFilter escapeFilter;
        try {
            escapeFilter =
                this.componentManagerProvider.get().getInstance(QueryFilter.class, ESCAPE_LIKE_PARAMETERS_FILTER);
        } catch (ComponentLookupException e) {
            // Shouldn't happen!
            throw new RuntimeException(
                String.format("Failed to locate [%s] Query Filter", ESCAPE_LIKE_PARAMETERS_FILTER), e);
        }

        boolean found = false;
        for (QueryFilter filter : query.getFilters()) {
            if (escapeFilter.getClass() == filter.getClass()) {
                found = true;
                break;
            }
        }

        if (!found) {
            query.addFilter(escapeFilter);
        }
    }

    private boolean hasQueryParametersType(Query query)
    {
        boolean found = false;

        for (Object value : query.getNamedParameters().values()) {
            if (value instanceof QueryParameter) {
                found = true;
                break;
            }
        }
        if (!found) {
            for (Object value : query.getPositionalParameters().values()) {
                if (value instanceof QueryParameter) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    /**
     * Append the required select clause to HQL short query statements. Short statements are the only way for users
     * without programming rights to perform queries. Such statements can be for example:
     * <ul>
     * <li>{@code , BaseObject obj where doc.fullName=obj.name and obj.className='XWiki.MyClass'}</li>
     * <li>{@code where doc.creationDate > '2008-01-01'}</li>
     * </ul>
     *
     * @param statement the statement to complete if required.
     * @return the complete statement if it had to be completed, the original one otherwise.
     */
    protected String completeShortFormStatement(String statement)
    {
        String lcStatement = statement.toLowerCase().trim();
        if (lcStatement.isEmpty() || lcStatement.startsWith(",") || lcStatement.startsWith("where ")
            || lcStatement.startsWith("order by ")) {
            return "select doc.fullName from XWikiDocument doc " + statement.trim();
        }

        return statement;
    }

    private <T> org.hibernate.query.Query<T> createNamedHibernateQuery(Session session, Query query)
    {
        org.hibernate.query.Query<T> hQuery = session.getNamedQuery(query.getStatement());

        Query filteredQuery = query;
        if (filteredQuery.getFilters() != null && !filteredQuery.getFilters().isEmpty()) {
            // Since we can't modify the Hibernate query statement at this point we need to create a new one to apply
            // the query filter. This comes with a performance cost, we could fix it by handling named queries ourselves
            // and not delegate them to Hibernate. This way we would always get a statement that we can transform before
            // the execution.
            boolean isNative = hQuery instanceof NativeQuery;
            String language = isNative ? "sql" : Query.HQL;
            final String statement = hQuery.getQueryString();

            // Run filters
            filteredQuery = filterQuery(new WrappingQuery(filteredQuery)
            {
                @Override
                public String getStatement()
                {
                    return statement;
                }
            }, language);

            if (isNative) {
                hQuery = session.createSQLQuery(filteredQuery.getStatement());
                // Copy the information about the return column types, if possible.
                NamedSQLQueryDefinition definition =
                    this.hibernate.getConfigurationMetadata().getNamedNativeQueryDefinition(query.getStatement());
                if (!StringUtils.isEmpty(definition.getResultSetRef())) {
                    ((NativeQuery<T>) hQuery).setResultSetMapping(definition.getResultSetRef());
                }
            } else {
                hQuery = session.createQuery(filteredQuery.getStatement());
            }
        }
        populateParameters(hQuery, filteredQuery);
        return hQuery;
    }

    /**
     * @param hquery query to populate parameters
     * @param query query from to populate.
     */
    protected void populateParameters(org.hibernate.query.Query<?> hquery, Query query)
    {
        if (query.getOffset() > 0) {
            hquery.setFirstResult(query.getOffset());
        }
        if (query.getLimit() > 0) {
            hquery.setMaxResults(query.getLimit());
        }
        for (Entry<String, Object> e : query.getNamedParameters().entrySet()) {
            setNamedParameter(hquery, e.getKey(), e.getValue());
        }
        Map<Integer, Object> positionalParameters = query.getPositionalParameters();
        if (positionalParameters.size() > 0) {
            positionalParameters.forEach(hquery::setParameter);
        }
    }

    /**
     * Sets the value of the specified named parameter, taking into account the type of the given value.
     *
     * @param query the query to set the parameter for
     * @param name the parameter name
     * @param value the non-null parameter value
     */
    protected void setNamedParameter(org.hibernate.query.Query<?> query, String name, Object value)
    {
        if (value instanceof Collection) {
            query.setParameterList(name, (Collection<?>) value);
        } else if (value.getClass().isArray()) {
            query.setParameterList(name, (Object[]) value);
        } else {
            query.setParameter(name, value);
        }
    }

    /**
     * @return Store component
     */
    protected XWikiHibernateStore getStore()
    {
        return getContext().getWiki().getHibernateStore();
    }

    /**
     * @return XWiki Context
     */
    protected XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
