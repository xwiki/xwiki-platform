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
package com.xpn.xwiki.internal.objects.classes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.objects.classes.UsersClass;

/**
 * Builds a query that returns the values allowed for a "List of Users" property.
 * 
 * @version $Id$
 * @since 9.8
 */
@Component
@Singleton
public class DefaultUsersQueryBuilder implements QueryBuilder<UsersClass>
{
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("document")
    private QueryFilter documentFilter;

    @Inject
    @Named("viewable")
    private QueryFilter viewableFilter;

    @Override
    public Query build(UsersClass usersClass) throws QueryException
    {
        String statement = new StringBuilder("select doc.fullName as userReference,")
            .append(" firstName.value||' '||lastName.value as userName ")
            .append("from XWikiDocument doc, BaseObject obj, StringProperty firstName, StringProperty lastName ")
            .append("where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'")
            .append(" and obj.id = firstName.id.id and firstName.id.name = 'first_name'")
            .append(" and obj.id = lastName.id.id and lastName.id.name = 'last_name'")
            .append(" and doc.space = 'XWiki' ")
            .append("order by lower(firstName.value), firstName.value, lower(lastName.value), lastName.value")
            .toString();
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        // Resolve the document full name from the first column into a DocumentReference (the user profile reference).
        query.addFilter(this.documentFilter);
        // Remove the users whose profile page is not viewable by the current user.
        query.addFilter(this.viewableFilter);
        query.setWiki(usersClass.getOwnerDocument().getDocumentReference().getWikiReference().getName());
        return query;
    }
}
