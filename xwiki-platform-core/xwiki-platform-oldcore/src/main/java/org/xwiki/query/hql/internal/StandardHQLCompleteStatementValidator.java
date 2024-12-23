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
package org.xwiki.query.hql.internal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.ValidationError;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;

/**
 * A HQL validator which always return {@code true} or {@code false}, it's intended to be used as last resort. It's
 * parsing the query and check if:
 * <ul>
 * <li>It's a SELECT query.</li>
 * <li>It only SELECT allowed properties.</li>
 * <li>It only uses allowed functions.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 17.0.0RC1
 * @since 16.10.2
 * @since 15.10.16
 * @since 16.4.6
 */
@Component
@Singleton
@Named("standard")
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class StandardHQLCompleteStatementValidator implements HQLCompleteStatementValidator
{
    private static final String ID = "id";

    private static final String FIELD_ID = ID;

    private static final String DOCUMENT_FIELD_FULLNAME = "fullName";

    private static final String DOCUMENT_FIELD_NAME = "name";

    private static final String DOCUMENT_FIELD_SPACE = "space";

    private static final String DOCUMENT_FIELD_LANGUAGE = "language";

    private static final String DOCUMENT_FIELD_DEFAULTLANGUAGE = "defaultLanguage";

    private static final String DOCUMENT_FIELD_TRANSLATION = "translation";

    private static final String DOCUMENT_FIELD_HIDDEN = "hidden";

    private static final String SPACE_FIELD_REFERENCE = "reference";

    private static final String SPACE_FIELD_NAME = DOCUMENT_FIELD_NAME;

    private static final String SPACE_FIELD_PARENT = "parent";

    private static final String SPACE_FIELD_HIDDEN = DOCUMENT_FIELD_HIDDEN;

    private static final String ATTACHMENT_FIELD_FILENAME = "filename";

    private static final String DELETEDATTACHMENT_FIELD_ID = FIELD_ID;

    private static final String DELETEDATTACHMENT_FIELD_FILENAME = ATTACHMENT_FIELD_FILENAME;

    private static final String DELETEDOCUMENT_FIELD_ID = FIELD_ID;

    private static final String DELETEDOCUMENT_FIELD_FULLNAME = DOCUMENT_FIELD_FULLNAME;

    private static final String DELETEDOCUMENT_FIELD_LANGUAGE = DOCUMENT_FIELD_LANGUAGE;

    private static final String FUNCTION_COUNT = "count";

    // Based on
    // https://docs.jboss.org/hibernate/orm/7.0/userguide/html_single/Hibernate_User_Guide.html#query-language
    private static final Set<String> ALLOWED_FUNCTIONS = Set.of(
        // Types and typecasts
        "type", "treat", "cast", "str",

        // Functions for working with null values
        "coalesce", "ifnull", "nullif",

        // Functions for working with dates and times
        "extract", "format", "trunc", "truncate",

        // Functions for working with strings
        "upper", "lower", "length", "concat", "locate", "position", "substring", "trim", "overlay", "pad", "left",
        "right", "replace", "repeat", "collate",

        // Numeric functions
        "abs", "sign", "mod", "sqrt", "exp", "power", "ln", "round", /* "trunc", "truncate", */ "floor", "ceiling",
        "log10", "log", "pi", "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "sinh", "cosh", "tanh", "degrees",
        "radians", "least", "greatest",

        // Functions for dealing with collections
        "size", "element", "index", "key", "value", "entry", "elements", "indices",

        // Functions for working with ids and versions
        ID, "version", "naturalid", "fk",

        // Functions for dealing with arrays
        "array", "array_list", "array_agg", "array_position", "array_positions", "array_positions_list", "array_length",
        "array_concat", "array_prepend", "array_append", "array_contains", "array_contains_nullable", "array_includes",
        "array_includes_nullable", "array_intersects", "array_intersects_nullable", "array_get", "array_set",
        "array_remove", "array_remove_index", "array_slice", "array_replace", "array_trim", "array_fill",
        "array_fill_list", "array_to_string", "unnest",

        // Functions for dealing with JSON
        "json_object", "json_array", "json_value", "json_exists", "json_query", "json_arrayagg", "json_objectagg",
        "json_set", "json_remove()", "json_mergepatch", "json_array_append", "json_array_insert", "json_table",

        // Functions for dealing with XML
        "xmlelement", "xmlcomment", "xmlforest", "xmlconcat", "xmlpi", "xmlquery", "xmlexists", "xmlagg", "xmltable",

        // in
        "in",

        // contains
        "contains",

        // intersects
        "intersects",

        // Aggregate functions
        FUNCTION_COUNT, "avg", "min", "max", "sum", "var_pop", "var_samp", "stddev_pop", "stddev_samp", "any", "some",
        "every", "all",

        // filter
        "filter");

    private static final Map<String, Set<String>> ALLOWED_FIELDS;

    static {
        ALLOWED_FIELDS = new HashMap<>();

        Set<String> allowedDocFields = new HashSet<>();
        ALLOWED_FIELDS.put("XWikiDocument", allowedDocFields);
        allowedDocFields.add(DOCUMENT_FIELD_FULLNAME);
        allowedDocFields.add(DOCUMENT_FIELD_NAME);
        allowedDocFields.add(DOCUMENT_FIELD_SPACE);
        allowedDocFields.add(DOCUMENT_FIELD_LANGUAGE);
        allowedDocFields.add(DOCUMENT_FIELD_DEFAULTLANGUAGE);
        allowedDocFields.add(DOCUMENT_FIELD_TRANSLATION);
        allowedDocFields.add(DOCUMENT_FIELD_HIDDEN);

        Set<String> allowedDeletedDocumentFields = new HashSet<>();
        ALLOWED_FIELDS.put("XWikiDeletedDocument", allowedDeletedDocumentFields);
        allowedDeletedDocumentFields.add(DELETEDOCUMENT_FIELD_ID);
        allowedDeletedDocumentFields.add(DELETEDOCUMENT_FIELD_FULLNAME);
        allowedDeletedDocumentFields.add(DELETEDOCUMENT_FIELD_LANGUAGE);

        Set<String> allowedSpaceFields = new HashSet<>();
        ALLOWED_FIELDS.put("XWikiSpace", allowedSpaceFields);
        allowedSpaceFields.add(SPACE_FIELD_REFERENCE);
        allowedSpaceFields.add(SPACE_FIELD_NAME);
        allowedSpaceFields.add(SPACE_FIELD_PARENT);
        allowedSpaceFields.add(SPACE_FIELD_HIDDEN);

        ALLOWED_FIELDS.put("XWikiAttachment", Collections.singleton(ATTACHMENT_FIELD_FILENAME));

        Set<String> allowedDeletedAttachmentFields = new HashSet<>();
        ALLOWED_FIELDS.put("DeletedAttachment", allowedDeletedAttachmentFields);
        allowedDeletedAttachmentFields.add(DELETEDATTACHMENT_FIELD_ID);
        allowedDeletedAttachmentFields.add(DELETEDATTACHMENT_FIELD_FILENAME);

    }

    @Override
    public Optional<Boolean> isSafe(String statementString)
    {
        FeaturesAllowed allowedFeatures = new FeaturesAllowed("xwiki");

        // Allow SELECT related features
        allowedFeatures.add(FeaturesAllowed.SELECT.getFeatures());

        // Allow JDBC related features
        allowedFeatures.add(FeaturesAllowed.JDBC.getFeatures());

        // Allow expressions
        allowedFeatures.add(FeaturesAllowed.EXPRESSIONS.getFeatures());

        // Parse the statement and make sure it contains SELECT related features
        Validation validation = new Validation(List.of(allowedFeatures), statementString);
        List<ValidationError> errors = validation.validate();

        // Check some other custom rules
        if (errors.isEmpty()) {
            Statements statements = validation.getParsedStatements();

            Statement statement = statements.getStatements().get(0);
            if (statement instanceof Select && isSelectSafe((Select) statement)) {
                return Optional.of(true);
            }
        }

        return Optional.of(false);
    }

    private boolean isSelectSafe(Select select)
    {
        SelectBody selectBody = select.getSelectBody();

        if (selectBody instanceof PlainSelect) {
            return isNodeSafe(((PlainSelect) selectBody).getASTNode());
        }

        return false;
    }

    private boolean isPlainSelectSafe(PlainSelect plainSelect)
    {
        Map<String, String> tables = getTables(plainSelect);

        // Make sure only allowed columns are used in SELECT
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            if (!isSelectItemAllowed(selectItem, tables)) {
                return false;
            }
        }

        return true;
    }

    private boolean isNodeSafe(Node node)
    {
        if (node instanceof SimpleNode) {
            // Check if the node is a function
            Object value = ((SimpleNode) node).jjtGetValue();
            if (value instanceof Function) {
                // Check if the function is allowed
                if (!isFunctionSafe((Function) value)) {
                    return false;
                }
            } else if (value instanceof PlainSelect) {
                // Check if the select is safe
                if (!isPlainSelectSafe((PlainSelect) value)) {
                    return false;
                }
            }
        }

        // Check children nodes
        for (int i = 0; i < node.jjtGetNumChildren(); ++i) {
            if (!isNodeSafe(node.jjtGetChild(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean isFunctionSafe(Function function)
    {
        return ALLOWED_FUNCTIONS.contains(function.getName().toLowerCase());
    }

    private Map<String, String> getTables(PlainSelect plainSelect)
    {
        Map<String, String> tables = new HashMap<>();

        // Add from item
        addFromItem(plainSelect.getFromItem(), tables);

        // Add joins
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                addFromItem(join.getRightItem(), tables);
            }
        }

        return tables;
    }

    private void addFromItem(FromItem item, Map<String, String> tables)
    {
        if (item instanceof Table) {
            String tableName = ((Table) item).getName();
            tables.put(item.getAlias() != null ? item.getAlias().getName() : tableName, tableName);
        }
    }

    /**
     * @param selectItem the {@link SelectItem} to check
     * @return true if the passed {@link SelectItem} is allowed
     */
    private boolean isSelectItemAllowed(SelectItem selectItem, Map<String, String> tables)
    {
        if (selectItem instanceof SelectExpressionItem) {
            return isSelectExpressionAllowed(((SelectExpressionItem) selectItem).getExpression(), tables);
        }

        // TODO: we could support more select items

        return false;
    }

    private boolean isAllowedAllTableColumns(ExpressionList parameters, Map<String, String> tables)
    {
        Expression expression = parameters.getExpressions().get(0);
        return expression instanceof AllTableColumns
            && isTableAllowed(getTableName(((AllTableColumns) expression).getTable(), tables));
    }

    private boolean isAllowedAllColumns(ExpressionList parameters, Map<String, String> tables)
    {
        return parameters.getExpressions().get(0) instanceof AllColumns && tables.size() == 1
            && isTableAllowed(tables.values().iterator().next());
    }

    private boolean isAllowedCountFunction(Function function, ExpressionList parameters, Map<String, String> tables)
    {
        return parameters.getExpressions().size() == 1 && function.getName().equalsIgnoreCase(FUNCTION_COUNT)
            && (isAllowedAllColumns(parameters, tables) || isAllowedAllTableColumns(parameters, tables));
    }

    private boolean isSelectExpressionAllowed(Expression expression, Map<String, String> tables)
    {
        boolean safe = false;

        if (expression instanceof Column) {
            if (isColumnAllowed(((Column) expression), tables)) {
                safe = true;
            }
        } else if (expression instanceof Function) {
            safe = isSelectFunctionSafe(((Function) expression), tables);
        }

        return safe;
    }

    private boolean isSelectFunctionSafe(Function function, Map<String, String> tables)
    {
        ExpressionList parameters = function.getParameters();
        if (isAllowedCountFunction(function, parameters, tables)) {
            // count(*)
            // count(table.*)
            // TODO: add support for more than "count" maybe
            return true;
        } else {
            // Validate that allowed expressions are used as function parameters
            for (Expression parameter : parameters.getExpressions()) {
                if (!isSelectExpressionAllowed(parameter, tables)) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * @param column the {@link Column} to check
     * @return true if the passed {@link Column} is allowed
     */
    private boolean isColumnAllowed(Column column, Map<String, String> tables)
    {
        Set<String> fields = ALLOWED_FIELDS.get(getTableName(column.getTable(), tables));
        return fields != null && fields.contains(column.getColumnName());
    }

    /**
     * @param tableName the name of the table
     * @return true if the table has at least one allowed field
     */
    private boolean isTableAllowed(String tableName)
    {
        return ALLOWED_FIELDS.containsKey(tableName);
    }

    private String getTableName(Table table, Map<String, String> tables)
    {
        String tableName = tables.values().iterator().next();

        if (table != null && StringUtils.isNotEmpty(table.getFullyQualifiedName())) {
            tableName = tables.get(table.getFullyQualifiedName());
        }

        return tableName;
    }
}
