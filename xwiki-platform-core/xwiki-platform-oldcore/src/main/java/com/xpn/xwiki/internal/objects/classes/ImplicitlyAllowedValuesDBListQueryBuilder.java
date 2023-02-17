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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.DBTreeListClass;

/**
 * Builds a query from the meta data of a Database List property.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("implicitlyAllowedValues")
@Singleton
public class ImplicitlyAllowedValuesDBListQueryBuilder implements QueryBuilder<DBListClass>
{
    private static final class DBListQuerySpec
    {
        public String wiki;

        public String className;

        public String idField;

        public String valueField;

        public String parentField;

        public boolean hasClassName;

        public boolean hasIdField;

        public boolean hasValueField;

        public boolean hasParentField;
    }

    private static final String DOC_PREFIX = "doc.";

    private static final String OBJ_PREFIX = "obj.";

    private static final String CLASS_NAME = "className";

    private static final String TEMPLATE_NAME = "templateName";

    private static final String COLUMN_SEPARATOR = ", ";

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("viewableAllowedDBListPropertyValue")
    private QueryFilter viewableValueFilter;

    /**
     * {@inheritDoc} The query is constructed according to the following rules:
     * <ul>
     * <li>If no class name, id or value fields are selected, return a query that returns no rows.</li>
     * <li>If only the class name is provided, select all document names which have an object of that type.</li>
     * <li>If only one of id and value is provided, select just one column.</li>
     * <li>If id = value, select just one column.</li>
     * <li>If no class name is provided, assume the fields are document properties.</li>
     * </ul>
     * If there are two columns selected, use the first one as the stored value and the second one as the displayed
     * value.
     * 
     * @see QueryBuilder#build(java.lang.Object)
     */
    @Override
    public Query build(DBListClass dbListClass) throws QueryException
    {
        DBListQuerySpec spec = new DBListQuerySpec();
        spec.wiki = dbListClass.getOwnerDocument().getDocumentReference().getWikiReference().getName();

        spec.className = StringUtils.defaultString(dbListClass.getClassname());
        spec.idField = StringUtils.defaultString(dbListClass.getIdField());
        spec.valueField = StringUtils.defaultString(dbListClass.getValueField());
        spec.parentField =
            dbListClass instanceof DBTreeListClass ? ((DBTreeListClass) dbListClass).getParentField() : "";

        spec.hasClassName = !StringUtils.isBlank(spec.className);
        spec.hasIdField = !StringUtils.isBlank(spec.idField);
        spec.hasValueField = !StringUtils.isBlank(spec.valueField);
        spec.hasParentField = !StringUtils.isBlank(spec.parentField);

        return build(spec);
    }

    private Query build(DBListQuerySpec spec) throws QueryException
    {

        // Initialize with default query (that returns no rows).
        String statement = "select doc.name from XWikiDocument doc where 1 = 0";
        Map<String, Object> parameters = new HashMap<>();

        if (spec.hasIdField || spec.hasValueField) {
            statement = getStatementWhenIdOrValueFieldsAreSpecified(spec, parameters);
        } else if (spec.hasClassName) {
            statement = "select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName";
            parameters.put(CLASS_NAME, spec.className);
            parameters.put(TEMPLATE_NAME, getTemplateName(spec.className));
        }

        Query query = this.queryManager.createQuery(statement, Query.HQL);
        query.setWiki(spec.wiki);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }
        query.addFilter(this.viewableValueFilter);

        return query;
    }

    private void addFieldToQuery(String fieldName, String fieldAlias, boolean hasClassName, List<String> selectClause,
        List<String> fromClause, List<String> whereClause, Map<String, Object> parameters)
    {
        if (fieldName.startsWith(DOC_PREFIX) || fieldName.startsWith(OBJ_PREFIX)) {
            selectClause.add(fieldName);
        } else if (!hasClassName) {
            selectClause.add(DOC_PREFIX + fieldName);
        } else {
            selectClause.add(fieldAlias + ".value");
            fromClause.add("StringProperty as " + fieldAlias);
            whereClause.add(String.format("obj.id = %1$s.id.id and %1$s.id.name = :%1$s", fieldAlias));
            parameters.put(fieldAlias, fieldName);
        }
    }

    private String getStatementWhenIdOrValueFieldsAreSpecified(DBListQuerySpec spec, Map<String, Object> parameters)
    {
        // Make sure we always have an id field. Ignore the value field if it duplicates the id field and there's no
        // parent field specified.
        if (!spec.hasIdField || (spec.idField.equals(spec.valueField) && !spec.hasParentField)) {
            // If the parent field is specified then we need to include the value field, even if it duplicates the id
            // field, because otherwise we can't distinguish between the value and the parent.
            spec.idField = spec.valueField;
            spec.hasIdField = true;
            spec.valueField = spec.hasParentField ? spec.valueField : "";
            spec.hasValueField = spec.hasParentField;
        } else if (!spec.hasValueField && spec.hasParentField) {
            // Same as above, we need to include the value field when the parent field is specified.
            spec.valueField = spec.idField;
            spec.hasValueField = true;
        }

        return getStatementWhenIdFieldIsSpecified(spec, parameters);
    }

    private String getStatementWhenIdFieldIsSpecified(DBListQuerySpec spec, Map<String, Object> parameters)
    {
        List<String> selectClause = new ArrayList<>();
        List<String> fromClause = new ArrayList<>();
        List<String> whereClause = new ArrayList<>();

        // We need to select the document in order to be able to check access rights on the results. At the same time we
        // want to skip this column when filtering the Database List property values.
        selectClause.add("doc.fullName as unfilterable0");
        fromClause.add("XWikiDocument as doc");

        // We need to join the objects table if the class name is specified or if one of the selected columns is an
        // object property.
        if (spec.hasClassName || spec.idField.startsWith(OBJ_PREFIX) || spec.valueField.startsWith(OBJ_PREFIX)
            || spec.parentField.startsWith(OBJ_PREFIX)) {
            fromClause.add("BaseObject as obj");
            whereClause.add("doc.fullName = obj.name");
            if (spec.hasClassName) {
                whereClause.add("obj.className = :className and doc.fullName <> :templateName");
                parameters.put(CLASS_NAME, spec.className);
                parameters.put(TEMPLATE_NAME, getTemplateName(spec.className));
            }
        }

        addFieldToQuery(spec.idField, "idProp", spec.hasClassName, selectClause, fromClause, whereClause, parameters);

        if (spec.hasValueField) {
            addFieldToQuery(spec.valueField, "valueProp", spec.hasClassName, selectClause, fromClause, whereClause,
                parameters);

            // We cannot include the parent field if there's no value field because we would confuse it with the value
            // field (the second column in the result set is reserved for the value).
            if (spec.hasParentField) {
                addFieldToQuery(spec.parentField, "parentProp", spec.hasClassName, selectClause, fromClause,
                    whereClause, parameters);
            }
        }

        StringBuilder statementBuilder =
            new StringBuilder("select distinct ").append(StringUtils.join(selectClause, COLUMN_SEPARATOR))
                .append(" from ").append(StringUtils.join(fromClause, COLUMN_SEPARATOR));
        if (whereClause.size() > 0) {
            statementBuilder.append(" where ").append(StringUtils.join(whereClause, " and "));
        }

        return statementBuilder.toString();
    }

    private String getTemplateName(String className)
    {
        return StringUtils.removeEnd(className, "Class") + "Template";
    }
}
