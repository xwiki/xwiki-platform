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
package org.xwiki.query.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.query.SecureQuery;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Query wrapper that allows to set filter from the filter component hint.
 *
 * @version $Id$
 * @since 4.0RC1
 */
public class ScriptQuery implements SecureQuery
{
    /**
     * Used to log possible warnings.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptQuery.class);

    /**
     * Used to retrieve {@link org.xwiki.query.QueryFilter} implementations.
     */
    private ComponentManager componentManager;

    /**
     * The wrapped {@link Query}.
     */
    private Query query;

    private boolean switchAuthor;

    private DocumentReference authorReference;

    private DocumentReference sourceReference;

    /**
     * Constructor.
     *
     * @param query the query object to wrap.
     * @param cm the xwiki component manager.
     */
    public ScriptQuery(Query query, ComponentManager cm)
    {
        this.query = query;
        this.componentManager = cm;
    }

    /**
     * Set a filter in the wrapped query from the filter component hint.
     *
     * @param filter the hint of the component filter to set in the wrapped query.
     * @return this query object.
     */
    public Query addFilter(String filter)
    {
        if (!StringUtils.isBlank(filter)) {
            try {
                QueryFilter queryFilter = this.componentManager.getInstance(QueryFilter.class, filter);
                addFilter(queryFilter);
            } catch (ComponentLookupException e) {
                // We need to avoid throwing exceptions in the wiki if the filter does not exist.
                LOGGER.warn("Failed to load QueryFilter with component hint [{}]. Root error [{}]", filter,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return this;
    }

    /**
     * Allow to retrieve the total count of items for the given query instead of the actual results. This method will
     * only work for queries selecting document full names, see {@link CountDocumentFilter} for more information.
     *
     * @return the total number of results for this query or -1 if an error occurred.
     */
    public long count()
    {
        long result = -1;

        try {
            // Create a copy of the wrapped query.
            QueryManager queryManager = this.componentManager.getInstance(QueryManager.class);
            Query countQuery = queryManager.createQuery(getStatement(), getLanguage());
            countQuery.setWiki(getWiki());
            for (Map.Entry<Integer, Object> entry : getPositionalParameters().entrySet()) {
                countQuery.bindValue(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Object> entry : getNamedParameters().entrySet()) {
                countQuery.bindValue(entry.getKey(), entry.getValue());
            }
            for (QueryFilter filter : getFilters()) {
                countQuery.addFilter(filter);
            }

            // Add the count filter to it.
            countQuery.addFilter(this.componentManager.getInstance(QueryFilter.class, "count"));

            // Execute and retrieve the count result.
            List<Long> results = countQuery.execute();
            result = results.get(0);
        } catch (Exception e) {
            LOGGER.warn("Failed to create count query for query [{}]. Root error: [{}]", getStatement(),
                ExceptionUtils.getRootCauseMessage(e));
        }

        return result;
    }

    @Override
    public String getStatement()
    {
        return this.query.getStatement();
    }

    @Override
    public String getLanguage()
    {
        return this.query.getLanguage();
    }

    @Override
    public boolean isNamed()
    {
        return this.query.isNamed();
    }

    @Override
    public Query setWiki(String wiki)
    {
        this.query.setWiki(wiki);
        return this;
    }

    @Override
    public String getWiki()
    {
        return this.query.getWiki();
    }

    @Override
    public Query bindValue(String variable, Object val)
    {
        this.query.bindValue(variable, val);
        return this;
    }

    @Override
    public Query bindValue(int index, Object val)
    {
        this.query.bindValue(index, val);
        return this;
    }

    @Override
    public Query bindValues(List<Object> values)
    {
        this.query.bindValues(values);
        return this;
    }

    @Override
    public Query bindValues(Map<String, ?> values)
    {
        this.query.bindValues(values);
        return this;
    }

    @Override
    public QueryParameter bindValue(String variable)
    {
        QueryParameter parameter = this.query.bindValue(variable);
        return new ScriptQueryParameter(this, parameter);
    }

    @Override
    public Map<String, Object> getNamedParameters()
    {
        return this.query.getNamedParameters();
    }

    @Override
    public Map<Integer, Object> getPositionalParameters()
    {
        return this.query.getPositionalParameters();
    }

    @Override
    public Query addFilter(QueryFilter filter)
    {
        this.query.addFilter(filter);
        return this;
    }

    @Override
    public List<QueryFilter> getFilters()
    {
        return this.query.getFilters();
    }

    @Override
    public Query setLimit(int limit)
    {
        this.query.setLimit(limit);
        return this;
    }

    @Override
    public Query setOffset(int offset)
    {
        this.query.setOffset(offset);
        return this;
    }

    @Override
    public int getLimit()
    {
        return this.query.getLimit();
    }

    @Override
    public int getOffset()
    {
        return this.query.getOffset();
    }

    @Override
    public <T> List<T> execute() throws QueryException
    {
        if (this.switchAuthor) {
            try {
                AuthorExecutor authorExecutor = this.componentManager.getInstance(AuthorExecutor.class);
                return authorExecutor.call(this.query::execute, this.authorReference, this.sourceReference);
            } catch (QueryException e) {
                throw e;
            } catch (Exception e) {
                throw new QueryException("Failed to execute query", this.query, e);
            }
        } else {
            return this.query.execute();
        }
    }

    @Override
    public boolean isCurrentAuthorChecked()
    {
        return this.query instanceof SecureQuery ? ((SecureQuery) this.query).isCurrentAuthorChecked() : true;
    }

    /**
     * Switch the author and reference to use to execute the query.
     * 
     * @param authorReference the user to check rights on
     * @param sourceReference the reference of the document associated with the {@link Callable} (which will be used to
     *            test the author right)
     * @return this query.
     * @throws AccessDeniedException when switching the query author is not allowed
     * @since 14.10
     * @since 14.4.7
     * @since 13.10.11
     */
    public SecureQuery setQueryAuthor(DocumentReference authorReference, DocumentReference sourceReference)
        throws AccessDeniedException
    {
        if (this.query instanceof SecureQuery) {
            // Only author with programming right can switch the query author
            try {
                ContextualAuthorizationManager authorization =
                    this.componentManager.getInstance(ContextualAuthorizationManager.class);
                authorization.checkAccess(Right.PROGRAM);

                this.switchAuthor = true;
                this.authorReference = authorReference;
                this.sourceReference = sourceReference;
            } catch (ComponentLookupException e) {
                LOGGER.error("Failed to lookup authorization manager", e);
            }
        }

        return this;
    }

    @Override
    public SecureQuery checkCurrentAuthor(boolean checkCurrentAuthor)
    {
        // Always check current author for scripts

        return this;
    }

    @Override
    public boolean isCurrentUserChecked()
    {
        return this.query instanceof SecureQuery ? ((SecureQuery) this.query).isCurrentAuthorChecked() : false;
    }

    @Override
    public SecureQuery checkCurrentUser(boolean checkCurrentUser)
    {
        if (this.query instanceof SecureQuery) {
            ((SecureQuery) this.query).isCurrentAuthorChecked();
        }

        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScriptQuery that = (ScriptQuery) o;

        return new EqualsBuilder().append(switchAuthor, that.switchAuthor)
            .append(componentManager, that.componentManager).append(query, that.query)
            .append(authorReference, that.authorReference).append(sourceReference, that.sourceReference).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(componentManager).append(query).append(switchAuthor)
            .append(authorReference).append(sourceReference).toHashCode();
    }
}
