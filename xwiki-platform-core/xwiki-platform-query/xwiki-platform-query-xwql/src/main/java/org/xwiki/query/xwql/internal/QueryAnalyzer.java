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
package org.xwiki.query.xwql.internal;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.internal.jpql.analysis.DepthFirstAdapter;
import org.xwiki.query.internal.jpql.node.AAbstractSchemaName;
import org.xwiki.query.internal.jpql.node.APath;
import org.xwiki.query.internal.jpql.node.ARangeVariableDeclaration;
import org.xwiki.query.internal.jpql.node.ASelectStatement;
import org.xwiki.query.internal.jpql.node.AXObjectDecl;
import org.xwiki.query.internal.jpql.node.AXPath;

/**
 * @version $Id$
 * @since 2.4M2
 */
public class QueryAnalyzer extends DepthFirstAdapter
{
    public QueryAnalyzer(QueryContext context)
    {
        this.context = context;
    }

    QueryContext context;

    @Override
    public void outASelectStatement(ASelectStatement node)
    {
        // needed for update information after FromClause
        node.getSelectClause().apply(this);
        super.outASelectStatement(node);
    }

    String documentFromName = "Document";

    @Override
    public void caseAAbstractSchemaName(AAbstractSchemaName node)
    {
        String from = node.toString().trim();
        String alias = ((ARangeVariableDeclaration) node.parent()).getVariable().toString().trim();
        if (documentFromName.equals(from)) {
            context.addDocument(alias);
        }
    }

    @Override
    public void caseAXObjectDecl(AXObjectDecl node)
    {
        String path[] = splitPath(node.getId().toString());
        if (path.length != 2) {
            throw new InvalidQueryException("docalias.object('classname') expected, but got:" + node.toString());
        }
        String docalias = path[0];
        String method = path[1];
        String className = unquote(node.getXClassName().toString().trim());

        String alias = node.parent().parent() instanceof ARangeVariableDeclaration
            // used in from clause
            ? ((ARangeVariableDeclaration) node.parent().parent()).getVariable().toString().trim()
            // used in where clause. unnamed.
            : null;

        if ("object".equals(method)) {
            context.addObject(docalias, className, alias, node);
        } else {
            throw new InvalidQueryException("document's method + [" + method + "] is unsupported");
        }
    }

    @Override
    public void caseAPath(APath node)
    {
        String path[] = splitPath(node.toString());
        QueryContext.ObjectInfo obj = context.getObject(path[0]);
        if (path.length >= 2 && obj != null) {
            obj.addProperty(path[1], node);
        }
    }

    @Override
    public void outAXPath(AXPath node)
    {
        QueryContext.ObjectInfo obj = context.getObject(node.getXObjectDecl());
        String path[] = splitPath(node.getProperty().toString());
        obj.addProperty(path[0], node);
        super.outAXPath(node);
    }

    public static String[] splitPath(String str)
    {
        return StringUtils.split(str.trim(), ".");
    }

    public static String unquote(String str)
    {
        str = str.trim();
        if (str.startsWith("'") && str.endsWith("'")
            || str.startsWith("\"") && str.endsWith("\""))
        {
            str = str.substring(1, str.length() - 1);
        }
        return str;
    }
}
