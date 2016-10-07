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
package org.xwiki.index.tree.internal.nestedpages.query;

import java.util.List;

import org.xwiki.query.QueryFilter;

/**
 * Base class for nested pages query filters.
 * 
 * @version $Id$
 * @since 8.3RC1
 * @since 7.4.5
 */
public abstract class AbstractNestedPageFilter implements QueryFilter
{
    @Override
    public List<?> filterResults(@SuppressWarnings("rawtypes") List results)
    {
        return results;
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        int unionIndex = statement.indexOf("union all");
        if (unionIndex < 0) {
            return filterNestedPagesStatement(statement);
        } else {
            int nestedPagesStatementStart = statement.indexOf("(select") + 1;
            int nestedPagesStatementEnd = statement.substring(0, unionIndex).lastIndexOf(')');
            int terminalPagesStatementStart = statement.indexOf('(', unionIndex) + 1;
            int terminalPagesStatementEnd =
                statement.substring(0, statement.lastIndexOf(") xwikiPage")).lastIndexOf(')');
            return statement.substring(0, nestedPagesStatementStart)
                + filterNestedPagesStatement(statement.substring(nestedPagesStatementStart, nestedPagesStatementEnd))
                + statement.substring(nestedPagesStatementEnd, terminalPagesStatementStart)
                + filterTerminalPagesStatement(
                    statement.substring(terminalPagesStatementStart, terminalPagesStatementEnd))
                + statement.substring(terminalPagesStatementEnd);
        }
    }

    protected String filterNestedPagesStatement(String statement)
    {
        return statement;
    }

    protected String filterTerminalPagesStatement(String statement)
    {
        return statement;
    }

    protected String insertWhereConstraint(String statement, String constraint)
    {
        int insertionPoint = statement.lastIndexOf("order by ");
        if (insertionPoint < 0) {
            insertionPoint = statement.length();
        }
        return statement.substring(0, insertionPoint) + (statement.lastIndexOf("where ") < 0 ? " where " : " and ")
            + constraint + statement.substring(insertionPoint);
    }
}
