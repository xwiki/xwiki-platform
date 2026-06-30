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

import org.apache.commons.lang3.Strings;
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

import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.store.DatabaseProduct;

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
    private static final String PROP_VALUE = "prop.value";

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

    private record SelectColumnAndFromTable(String selectColumn, String fromTable)
    {
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

    @Inject
    private HibernateStore hibernateStore;

    @Override
    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    public Query build(ListClass listClass) throws QueryException
    {
        String statement;

        // On Oracle, StringListProperty uses a CLOB column which cannot be used in GROUP BY.
        // We need a special query that doesn't use GROUP BY for this case.
        boolean usesClobColumn = usesClobColumn(listClass);

        SelectColumnAndFromTable selectAndFrom = getSelectColumnAndFromTable(listClass);
        if (usesClobColumn) {
            // For CLOB columns on Oracle, we cannot use GROUP BY or DISTINCT on CLOB columns.
            // Unfortunately, we also cannot use subqueries in select clauses in HQL in Hibernate < 6 - otherwise, we
            // could possibly have used a partitioned ROW_NUMBER() over (PARTITION BY ...) approach.
            // We use a "not exists" subquery to get distinct values instead by selecting only the first occurrence of
            // each value by ordering by id and ensuring no prior id has the same value.
            String selectColumn = selectAndFrom.selectColumn();
            String fromTable = selectAndFrom.fromTable();
            // Add an alias to the second instance of the table to avoid conflicts.
            String fromTableProp2 = fromTable.replace(" as prop", " as prop2");
            String selectColumnProp2 = selectColumn.replace("prop.", "prop2.");

            // TextQueryFilter modifies the following query to filter results based on the text typed by the user. It
            // uses aliases to determine the column type. By using an alias with the prefix "string" we avoid casting
            // CLOB columns to string which fails when the value is longer than 4000 characters and by using an alias
            // with the prefix "unfilterable" we avoid filtering on the count.
            statement = String.format(
                "select %1$s as stringValue, 1L as unfilterable0 "
                    + "from BaseObject as obj, %2$s "
                    + "where obj.className = :className "
                    + "  and obj.name <> :templateName "
                    + "  and prop.id.id = obj.id "
                    + "  and prop.id.name = :propertyName "
                    + "  and not exists ("
                    + "    select 1 from %3$s "
                    + "    where prop2.id.name = :propertyName "
                    + "      and prop2.id.id < prop.id.id "
                    + "      and FUNCTION('DBMS_LOB.COMPARE', %4$s, %1$s) = 0"
                    + "  ) "
                    + "order by obj.id",
                selectColumn, fromTable, fromTableProp2, selectColumnProp2);
        } else {
            // As in the query above, TextQueryFilter modifies the query and can be influenced with aliases. The first
            // column could be a CLOB column in some databases. As the cast to string fails for long values, for
            // example, in HSQLDB, we prevent TextQueryFilter from casting it to string by using an alias with the
            // prefix "string". As we're dealing with a text value in all cases, there is no need for the cast. We also
            // avoid filtering on the count again by using an alias with the prefix "unfilterable".
            statement = String.format("select %1$s as stringValue, count(*) as unfilterable0 "
                + "from BaseObject as obj, %2$s "
                + "where obj.className = :className and obj.name <> :templateName"
                + " and prop.id.id = obj.id and prop.id.name = :propertyName "
                + "group by %1$s "
                + "order by unfilterable0 desc", selectAndFrom.selectColumn(), selectAndFrom.fromTable());
        }
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        bindParameterValues(query, listClass);
        query.addFilter(new ViewableValueFilter(listClass));
        query.setWiki(listClass.getReference().extractReference(EntityType.WIKI).getName());
        return query;
    }

    private boolean usesClobColumn(ListClass listClass)
    {
        if (this.hibernateStore.getDatabaseProductName() != DatabaseProduct.ORACLE) {
            return false;
        }

        BaseProperty<?> property = listClass.newProperty();
        return property instanceof StringListProperty || property instanceof LargeStringProperty;
    }

    private SelectColumnAndFromTable getSelectColumnAndFromTable(ListClass listClass)
    {
        // Base the selection on the actual type of a property as different classes like UsersClass and GroupsClass
        // don't respect the configured storage type. Further, this avoids duplicating the logic for selecting the
        // property type.
        BaseProperty<?> property = listClass.newProperty();
        SelectColumnAndFromTable result;
        if (property instanceof StringListProperty) {
            result = new SelectColumnAndFromTable("prop.textValue", "StringListProperty as prop");
        } else if (property instanceof DBStringListProperty) {
            result = new SelectColumnAndFromTable("listItem", "DBStringListProperty as prop join prop.list listItem");
        } else if (property instanceof LargeStringProperty) {
            result = new SelectColumnAndFromTable(PROP_VALUE, "LargeStringProperty as prop");
        } else {
            result = new SelectColumnAndFromTable(PROP_VALUE, "StringProperty as prop");
        }
        return result;
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
        return Strings.CS.removeEnd(className, "Class") + "Template";
    }

    private boolean canView(ListClass listClass, String value, long count)
    {
        // We can't check all the documents where this value occurs so we check just one of them, chosen randomly.
        long offset = ThreadLocalRandom.current().nextLong(count);
        SelectColumnAndFromTable selectAndFrom = getSelectColumnAndFromTable(listClass);
        String selectColumn = selectAndFrom.selectColumn();

        // For CLOB columns on Oracle, we need to use DBMS_LOB.COMPARE to compare the values.
        String comparison;
        if (usesClobColumn(listClass)) {
            comparison = String.format("FUNCTION('DBMS_LOB.COMPARE', %s, :propertyValue) = 0", selectColumn);
        } else {
            comparison = String.format("%s = :propertyValue", selectColumn);
        }

        String statement = String.format(
            "select obj.name from BaseObject as obj, %s "
                + "where obj.className = :className and obj.name <> :templateName "
                + "and prop.id.id = obj.id and prop.id.name = :propertyName and %s",
            selectAndFrom.fromTable(), comparison);
        try {
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            bindParameterValues(query, listClass);
            query.bindValue("propertyValue", value);
            query.setWiki(listClass.getReference().extractReference(EntityType.WIKI).getName());
            query.setOffset((int) offset).setLimit(1);
            List<?> results = query.execute();
            if (!results.isEmpty()) {
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
