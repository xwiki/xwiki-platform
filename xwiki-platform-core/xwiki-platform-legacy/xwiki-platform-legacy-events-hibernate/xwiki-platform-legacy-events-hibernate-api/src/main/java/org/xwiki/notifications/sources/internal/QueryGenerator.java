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
package org.xwiki.notifications.sources.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Generate a query to retrieve notifications events according to the preferences of the user.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component(roles = QueryGenerator.class)
@Singleton
public class QueryGenerator
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private QueryExpressionGenerator expressionGenerator;

    @Inject
    private ExpressionNodeToHQLConverter hqlConverter;

    /**
     * Generate the query.
     *
     * @param parameters parameters to use
     * @return the query to execute
     * @throws QueryException if error happens
     * @throws EventStreamException if error happens
     */
    public Query generateQuery(NotificationParameters parameters) throws QueryException, EventStreamException
    {
        ExpressionNodeToHQLConverter.HQLQuery result =
            hqlConverter.parse(this.expressionGenerator.generateQueryExpression(parameters));
        if (result.getQuery().isEmpty()) {
            return null;
        }

        Query query = queryManager.createQuery(String.format("where %s", result.getQuery()), Query.HQL);
        for (Map.Entry<String, Object> queryParameter : result.getQueryParameters().entrySet()) {
            query.bindValue(queryParameter.getKey(), queryParameter.getValue());
        }

        return query;
    }

    /**
     * Generate the query.
     *
     * @param parameters parameters to use
     * @return the query to execute
     * @since 9.8RC12x
     */
    public ExpressionNode generateQueryExpression(NotificationParameters parameters) throws EventStreamException
    {
        return this.expressionGenerator.generateQueryExpression(parameters);
    }
}
