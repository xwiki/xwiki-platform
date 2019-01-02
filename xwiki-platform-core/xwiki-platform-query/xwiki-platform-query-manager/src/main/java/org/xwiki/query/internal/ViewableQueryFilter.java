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

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryFilter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Removes from the query results the rows that correspond to entities that the current user doesn't have the right to
 * view. This filter expects an {@link EntityReference} on the first column so you need another filter that creates the
 * entity reference (like the "attachment" or "user" filter) to be applied before this one.
 * 
 * @version $Id$
 * @since 9.8
 */
@Component
@Named("viewable")
@Singleton
public class ViewableQueryFilter implements QueryFilter
{
    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public String filterStatement(String statement, String language)
    {
        // We don't filter the query statement.
        return statement;
    }

    @Override
    public List filterResults(List results)
    {
        List<Object> filteredResults = new LinkedList<>();
        for (Object result : results) {
            EntityReference entityReference = null;
            if (result instanceof EntityReference) {
                entityReference = (EntityReference) result;
            } else if (result instanceof Object[] && ((Object[]) result)[0] instanceof EntityReference) {
                entityReference = (EntityReference) ((Object[]) result)[0];
            }
            if (entityReference != null && this.authorization.hasAccess(Right.VIEW, entityReference)) {
                filteredResults.add(result);
            }
        }
        return filteredResults;
    }
}
