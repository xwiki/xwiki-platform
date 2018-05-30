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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Builds a query that returns the values used by a List property.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("usedValues")
@Singleton
public class UsedValuesListQueryBuilder implements QueryBuilder<ListClass>
{
    private class ViewableValueFilter implements QueryFilter
    {
        private final ListClass listClass;

        ViewableValueFilter(ListClass listClass)
        {
            this.listClass = listClass;
        }

        @Override
        public String filterStatement(String statement, String language)
        {
            // We only filter the results.
            return statement;
        }

        @Override
        public List filterResults(List results)
        {
            List<Object> filteredResults = new LinkedList<>();
            for (Object result : results) {
                Object[] row = (Object[]) result;
                String value = (String) row[0];
                Long count = (Long) row[1];
                if (UsedValuesListQueryBuilder.this.canView(this.listClass, value, count)) {
                    filteredResults.add(result);
                }
            }
            return filteredResults;
        }
    }

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public Query build(ListClass listClass) throws QueryException
    {
        String statement = String.format("select %1$s, count(*) as unfilterable0 " + "from BaseObject as obj, %2$s "
            + "where obj.className = :className and obj.name <> :templateName"
            + " and prop.id.id = obj.id and prop.id.name = :propertyName " + "group by %1$s "
            + "order by count(*) desc", getSelectColumnAndFromTable(listClass));
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        bindParameterValues(query, listClass);
        query.addFilter(new ViewableValueFilter(listClass));
        query.setWiki(listClass.getReference().extractReference(EntityType.WIKI).getName());
        return query;
    }

    private Object[] getSelectColumnAndFromTable(ListClass listClass)
    {
        String selectColumn = "prop.textValue";
        String fromTable = "StringListProperty as prop";
        if (!listClass.isMultiSelect()) {
            selectColumn = "prop.value";
            fromTable = "StringProperty as prop";
        } else if (listClass.isRelationalStorage()) {
            selectColumn = "listItem";
            fromTable = "DBStringListProperty as prop join prop.list listItem";
        }
        return new Object[] {selectColumn, fromTable};
    }

    private void bindParameterValues(Query query, ListClass listClass)
    {
        String className = listClass.getObject().getName();
        query.bindValue("className", className);
        query.bindValue("propertyName", listClass.getName());
        query.bindValue("templateName", getTemplateName(className));
    }

    private String getTemplateName(String className)
    {
        return StringUtils.removeEnd(className, "Class") + "Template";
    }

    private boolean canView(ListClass listClass, String value, long count)
    {
        // We can't check all the documents where this value occurs so we check just one of them, chosen randomly.
        long offset = ThreadLocalRandom.current().nextLong(count);
        String statement = String.format(
            "select obj.name from BaseObject as obj, %2$s "
                + "where obj.className = :className and obj.name <> :templateName "
                + "and prop.id.id = obj.id and prop.id.name = :propertyName and %1$s = :propertyValue",
            getSelectColumnAndFromTable(listClass));
        try {
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            bindParameterValues(query, listClass);
            query.bindValue("propertyValue", value);
            query.setWiki(listClass.getReference().extractReference(EntityType.WIKI).getName());
            query.setOffset((int) offset).setLimit(1);
            List<?> results = query.execute();
            if (results.size() > 0) {
                DocumentReference documentReference = this.documentReferenceResolver.resolve((String) results.get(0));
                if (this.authorization.hasAccess(Right.VIEW, documentReference)) {
                    return true;
                }
            }
        } catch (QueryException e) {
            this.logger.warn("Failed to check if the list value is viewable. Root cause is [{}]."
                + " Continue assuming the value is not viewable.", ExceptionUtils.getRootCauseMessage(e));
        }
        return false;
    }
}
