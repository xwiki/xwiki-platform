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
package org.xwiki.query.xwql.internal.hql;

import org.xwiki.query.internal.jpql.analysis.DepthFirstAdapter;
import org.xwiki.query.internal.jpql.node.ABrConditionalPrimary;
import org.xwiki.query.internal.jpql.node.ACollectionMemberExpression;
import org.xwiki.query.internal.jpql.node.AConditionalFactor;
import org.xwiki.query.internal.jpql.node.AFromClause;
import org.xwiki.query.internal.jpql.node.ARangeVariableDeclaration;
import org.xwiki.query.internal.jpql.node.ASelectStatement;
import org.xwiki.query.internal.jpql.node.ASingleConditionalExpression;
import org.xwiki.query.internal.jpql.node.ASingleConditionalTerm;
import org.xwiki.query.internal.jpql.node.AWhereClause;
import org.xwiki.query.internal.jpql.node.Node;
import org.xwiki.query.internal.jpql.node.TLbr;
import org.xwiki.query.internal.jpql.node.TRbr;
import org.xwiki.query.internal.jpql.node.Token;
import org.xwiki.query.xwql.internal.QueryContext;

public class TreePrinter extends DepthFirstAdapter
{
    protected StringBuilder builder = new StringBuilder();

    private Printer printer;

    public TreePrinter(Printer printer)
    {
        this.printer = printer;
    }

    protected Printer getPrinter()
    {
        return printer;
    }

    protected QueryContext getContext()
    {
        return getPrinter().context;
    }

    @Override
    public String toString()
    {
        return builder.toString();
    }

    @Override
    public void defaultCase(Node node)
    {
        if (node instanceof Token) {
            Token token = (Token) node;
            builder.append(' ')
                .append(token.getText());
        }
    }

    @Override
    public void inASelectStatement(ASelectStatement node)
    {
        super.inASelectStatement(node);
        if (node.getWhereClause() == null) {
            // needed for #outAWhereClause
            node.setWhereClause(new AWhereClause());
        } else if (printer.where.length() > 0) {
            AWhereClause where = (AWhereClause) node.getWhereClause();
            // where := ( where )
            where.setConditionalExpression(
                new ASingleConditionalExpression(
                    new ASingleConditionalTerm(
                        new AConditionalFactor(null,
                            new ABrConditionalPrimary(new TLbr(),
                                where.getConditionalExpression(),
                                new TRbr())))));
        }
    }

    @Override
    public void caseARangeVariableDeclaration(ARangeVariableDeclaration node)
    {
        String from = node.getAbstractSchemaName().toString().trim();
        String alias = node.getVariable().toString().trim();

        if (getContext().getDocument(alias) != null) {
            from = "XWikiDocument";
        } else if (getContext().getObject(alias) != null) {
            from = "BaseObject";
        } else if (from.equals("Space")) {
            from = "XWikiSpace";
        }
        builder.append(' ').append(from)
            .append(" as ").append(alias);
    }

    @Override
    public void outAFromClause(AFromClause node)
    {
        String from = getPrinter().from.toString();
        if (from.length() > 0) {
            builder.append(' ').append(from);
        }
    }

    @Override
    public void outAWhereClause(AWhereClause node)
    {
        if (getPrinter().where.length() > 0) {
            if (node.getWhere() == null) {
                builder.append(" WHERE 1=1 ");
            }
            builder.append(getPrinter().where.toString());
        }
    }

    @Override
    public void caseACollectionMemberExpression(ACollectionMemberExpression node)
    {
        // "member of" fails on HQL, so use "="
        // this works only for a property of DBStringListProperty (relational storage)
        builder.append(" = ");
        node.getPath().apply(this);
        builder.append(" ");
    }
}
