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
package org.xwiki.query;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Query Filter interface. A filter can be added to a query through {@link Query#addFilter(QueryFilter)}, it will be
 * called by the {@link QueryExecutor} before the query is executed. Queries can be filtered during 2 stages:
 * <ul>
 *   <li>Before the execution, by modifying the statement</li>
 *   <li>After the execution, by modifying list of results</li>
 * </ul>
 *
 * An example of this is the {@link org.xwiki.query.internal.UniqueDocumentFilter} which transform statements in order
 * to make them return distinct documents names and which also filters query results in order to return only those
 * names.
 *
 * @version $Id$
 * @since 4.0RC1
 */
@Role
public interface QueryFilter
{
    /**
     * Transform a query statement. The statement can be returned without modification.
     *
     * @param statement the query statement to transform.
     * @param language the language of the query statement.
     * @return the transformed statement.
     */
    String filterStatement(String statement, String language);

    /**
     * Filter a list of query results. The result list can be returned without modification.
     *
     * @param results the original result list.
     * @return a filtered result list.
     */
    List filterResults(List results);
}
