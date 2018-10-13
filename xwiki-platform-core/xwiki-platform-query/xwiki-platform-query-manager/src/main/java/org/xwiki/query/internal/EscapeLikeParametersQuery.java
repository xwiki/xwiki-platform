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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.query.Query;
import org.xwiki.query.WrappingQuery;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

/**
 * Wraps a {@link Query} to perform modifications on it in order to modify the parameters and statements to change
 * the default escape character and escape parameters. See {@link EscapeLikeParametersFilter} for more details.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class EscapeLikeParametersQuery extends WrappingQuery
{
    private static final String ESCAPED_REPLACEMENT = "!$1";

    private List<String> modifiedNamedParameters;

    private List<Integer> modifiedPositionalParameters;

    /**
     * @param wrappedQuery the query to wrap and for which we're changing some behavior
     */
    public EscapeLikeParametersQuery(Query wrappedQuery)
    {
        super(wrappedQuery);
    }

    @Override
    public Map<String, Object> getNamedParameters()
    {
        return convertParameters(modifiedNamedParameters, super.getNamedParameters());
    }

    @Override
    public Map<Integer, Object> getPositionalParameters()
    {
        return convertParameters(modifiedPositionalParameters, super.getPositionalParameters());
    }

    private <T> Map<T, Object> convertParameters(List<T> modifiedParameters, Map<T, Object> parametersToEscape)
    {
        if (modifiedParameters != null) {
            // Escape entries from the Map where needed
            Map<T, Object> escapedMap = new LinkedHashMap<>();
            for (Map.Entry<T, Object> entry : parametersToEscape.entrySet()) {
                // TODO: Also handle Arrays and collections in the future
                if (modifiedParameters.contains(entry.getKey()) && entry.getValue() instanceof DefaultQueryParameter) {
                    // Join the parameter parts and escape the literal parts
                    DefaultQueryParameter queryParameter = (DefaultQueryParameter) entry.getValue();
                    StringBuffer buffer = new StringBuffer();
                    for (ParameterPart part : queryParameter.getParts()) {
                        if (part instanceof LiteralParameterPart) {
                            // SQL92 only specifies "%", "_" and the escape character itself.
                            // However some DBs also support "[specifier]" and "[^specifier]" so by escaping "[" we're
                            // playing it safe. See http://bit.ly/2ongxm6
                            // Now the problem is that most databases don't accept escaping any character and they only
                            // support escaping the special characters such as "%", "_" and the escape character itself.
                            // See https://jira.xwiki.org/browse/XWIKI-14217 and
                            // https://groups.google.com/d/msg/h2-database/jT0O3rNgpSw/hU_JKXRkZNoJ
                            // Thus we don't escape '[' FTM meaning that the query could fail on Sybase and SQL Server
                            buffer.append(part.getValue().replaceAll("([%_!])", ESCAPED_REPLACEMENT));
                        } else if (part instanceof LikeParameterPart) {
                            // Escape the escape character
                            buffer.append(part.getValue().replaceAll("([!])", ESCAPED_REPLACEMENT));
                        } else {
                            buffer.append(part.getValue());
                        }
                    }
                    escapedMap.put(entry.getKey(), buffer.toString());
                } else {
                    escapedMap.put(entry.getKey(), entry.getValue());
                }
            }
            return escapedMap;
        } else {
            return parametersToEscape;
        }
    }

    @Override
    public String getStatement()
    {
        String statement;
        if (getLanguage().equals(Query.HQL)) {
            try {
                statement = modifyStatement(super.getStatement());
            } catch (JSQLParserException e) {
                throw new RuntimeException(String.format("Invalid HQL query [%s]", super.getStatement()), e);
            }
        } else {
            statement = super.getStatement();
        }
        return statement;
    }

    /**
     * Handle the case of MySQL: in MySQL a '\' character is a special escape character. In addition we often
     * use '\' in Entity References. For example to find nested pages in a page with a dot would result in
     * something like "LIKE '.%.a\.b.%'" which wouldn't work on MySQL. Thus we need to replace the default
     * escape character with another one. To be safe we verify that the statement doesn't already specify an ESCAPE
     * term.
     */
    private String modifyStatement(String statementString) throws JSQLParserException
    {
        Statement statement = CCJSqlParserUtil.parse(statementString);
        if (statement instanceof Select) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                Expression where = plainSelect.getWhere();
                where.accept(new XWikiExpressionVisitor());
            }
        }

        return statement.toString();
    }

    private class XWikiExpressionVisitor extends ExpressionVisitorAdapter
    {
        @Override
        public void visit(LikeExpression expr)
        {
            if (expr.getEscape() == null) {
                expr.setEscape("!");
                expr.accept(new XWikiLikeExpressionVisitor());
            }
        }
    }

    private class XWikiLikeExpressionVisitor extends ExpressionVisitorAdapter
    {
        @Override
        public void visit(JdbcParameter parameter)
        {
            if (modifiedPositionalParameters == null) {
                modifiedPositionalParameters = new ArrayList<>();
            }
            // Remove one to the index since we're using a JPQL parser and JPQL starts at 1
            // but HQL positional parameters start a 0.
            modifiedPositionalParameters.add(parameter.getIndex() - 1);
        }

        @Override
        public void visit(JdbcNamedParameter parameter)
        {
            if (modifiedNamedParameters == null) {
                modifiedNamedParameters = new ArrayList<>();
            }
            modifiedNamedParameters.add(parameter.getName());
        }
    }
}
