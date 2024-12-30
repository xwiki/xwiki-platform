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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
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
import org.xwiki.query.hql.internal.HQLStatementValidator;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * {@link QueryExecutor} implementation for Hibernate Store.
 *
 * @version $Id$
 * @since 1.6M1
 */
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
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

    @Inject
    private HQLStatementValidator queryValidator;

    @Inject
    private Logger logger;

    private volatile Set<String> safeNamedQueries;

    @Override
    public void initialize() throws InitializationException
    {
        Configuration configuration = this.hibernate.getConfiguration();

        configuration.addInputStream(Util.getResourceAsStream(MAPPING_PATH));
    }

    private Set<String> getSafeNamedQueries()
    {
        if (this.safeNamedQueries == null) {
            synchronized (this) {
                if (this.safeNamedQueries == null) {
                    Set<String> safeQueries = new HashSet<>();

                    // Gather the list of allowed named queries
                    Collection<NamedQueryDefinition> namedQueries =
                        this.hibernate.getConfigurationMetadata().getNamedQueryDefinitions();
                    for (NamedQueryDefinition definition : namedQueries) {
                        try {
                            if (this.queryValidator.isSafe(definition.getQuery())) {
                                safeQueries.add(definition.getName());
                            }
                        } catch (QueryException e) {
                            this.logger.warn("Failed to validate named query [{}] with statement [{}]: {}",
                                definition.getName(), definition.getQuery(), ExceptionUtils.getRootCauseMessage(e));
                        }
                    }

                    this.safeNamedQueries = safeQueries;
                }
            }
        }

        return this.safeNamedQueries;
    }

    /**
     * @param statement the statement to evaluate
     * @return true if the select is allowed for user without PR
     * @deprecated
     */
    @Deprecated(since = "17.0.0RC1, 16.10.2, 15.10.16, 16.4.6")
    protected static boolean isSafeSelect(String statement)
    {
        HQLStatementValidator validator = Utils.getComponent(HQLStatementValidator.class);

        try {
            return validator.isSafe(statement);
        } catch (QueryException e) {
            return false;
        }
    }

    protected void checkAllowed(final Query query) throws QueryException
    {
        // Check if the query needs to be validated according to the current author
        if (query instanceof SecureQuery) {
            checkAllowed((SecureQuery) query);
        }
    }

    private void checkAllowed(SecureQuery secureQuery) throws QueryException
    {
        if (secureQuery.isCurrentAuthorChecked()) {
            // Not need to check the details if current author has programming right
            if (!this.authorization.hasAccess(Right.PROGRAM)) {
                if (secureQuery.isNamed() && !getSafeNamedQueries().contains(secureQuery.getStatement())) {
                    throw new QueryException("Named queries requires programming right", secureQuery, null);
                }

                if (!this.queryValidator.isSafe(secureQuery.getStatement())) {
                    throw new QueryException("The query requires programming right", secureQuery, null);
                }
            }
        }
    }

    @Override
    public <T> List<T> execute(final Query query) throws QueryException
    {
        String oldDatabase = getContext().getWikiId();
        try {
            if (query.getWiki() != null) {
                getContext().setWikiId(query.getWiki());
            }

            // Make sure the query is allowed. Make sure to do it in the target context.
            checkAllowed(query);

            // Filter the query
            Query filteredQuery = filterQuery(query);

            // Execute the query
            List<T> results = getStore().executeRead(getContext(), session -> {
                org.hibernate.query.Query<T> hquery = createQuery(session, filteredQuery);

                return hquery.list();
            });

            // Filter the query result
            if (query.getFilters() != null && !query.getFilters().isEmpty()) {
                for (QueryFilter filter : query.getFilters()) {
                    results = filter.filterResults(results);
                }
            }

            return results;
        } catch (Exception e) {
            throw new QueryException("Exception while executing query", query, e);
        } finally {
            getContext().setWikiId(oldDatabase);
        }
    }

    protected Query filterQuery(Query query)
    {
        Query filteredQuery = query;

        // Named queries are not filtered
        if (!filteredQuery.isNamed()) {
            // Make sure to work with the complete form of the query
            filteredQuery = HqlQueryUtils.toCompleteQuery(filteredQuery);

            // Execute filters
            filteredQuery = filterQuery(filteredQuery, Query.HQL);
        }

        return filteredQuery;
    }

    /**
     * Create an Hibernate query from an XWiki {@link Query}.
     * 
     * @param <T> the type to return
     * @param session the Hibernate session
     * @param query the query to convert
     * @return the Hibernate query
     */
    protected <T> org.hibernate.query.Query<T> createQuery(Session session, Query query)
    {
        org.hibernate.query.Query<T> hquery;

        if (!query.isNamed()) {
            hquery = session.createQuery(query.getStatement());
            populateParameters(hquery, query);
        } else {
            hquery = createNamedHibernateQuery(session, query);
        }

        return hquery;
    }

    /**
     * @deprecated since 13.10.6, 14.4, use {@link #createQuery(Session, Query)} instead
     */
    @Deprecated(since = "13.10.6")
    protected <T> org.hibernate.query.Query<T> createHibernateQuery(Session session, Query query)
    {
        return createQuery(session, filterQuery(query));
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
        return HqlQueryUtils.toCompleteStatement(statement);
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
