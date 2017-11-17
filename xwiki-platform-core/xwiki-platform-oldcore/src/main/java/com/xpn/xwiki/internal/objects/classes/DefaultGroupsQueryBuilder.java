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

import com.xpn.xwiki.objects.classes.GroupsClass;

/**
 * Builds a query that returns the values allowed for a "List of Groups" property.
 * 
 * @version $Id$
 * @since 9.8
 */
@Component
@Singleton
public class DefaultGroupsQueryBuilder implements QueryBuilder<GroupsClass>
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
    public Query build(GroupsClass groupsClass) throws QueryException
    {
        // We select distinct results because the document that defines the group has multiple XWiki.XWikiGroups
        // objects, one for each group member. We order by document full name because the group document title is often
        // empty. We could use coalesce and nullif functions to fall back on the document name when the title is empty
        // but it complicates too much the query (and we can have a lot of XWiki.XWikiGroups objects, more than the
        // number of users). We have to select the lower case version of the group reference in order to be able to use
        // it in the order by clause (otherwise we get "PSQLException: ERROR: for SELECT DISTINCT, ORDER BY expressions
        // must appear in select list").
        String statement = new StringBuilder("select distinct doc.fullName as groupReference, doc.title as groupName,")
            .append(" lower(doc.fullName) as lowerGroupReference ")
            .append("from XWikiDocument doc, BaseObject obj ")
            .append("where doc.fullName = obj.name and obj.className = 'XWiki.XWikiGroups'")
            .append(" and doc.space = 'XWiki' and doc.name <> 'XWikiGroupTemplate' ")
            .append("order by lowerGroupReference, groupReference").toString();
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        // Resolve the document full name from the first column into a DocumentReference (the group profile reference).
        query.addFilter(this.documentFilter);
        // Remove the groups whose profile page is not viewable by the current user.
        query.addFilter(this.viewableFilter);
        query.setWiki(groupsClass.getOwnerDocument().getDocumentReference().getWikiReference().getName());
        return query;
    }
}
