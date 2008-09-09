package org.xwiki.query.xwql;

import org.apache.commons.lang.StringUtils;
import org.xwiki.query.jpql.analysis.DepthFirstAdapter;
import org.xwiki.query.jpql.node.AAbstractSchemaName;
import org.xwiki.query.jpql.node.APath;
import org.xwiki.query.jpql.node.ARangeVariableDeclaration;
import org.xwiki.query.jpql.node.ASelectStatement;
import org.xwiki.query.jpql.node.AXObjectDecl;
import org.xwiki.query.jpql.node.AXPath;
import org.xwiki.query.xwql.QueryContext.ObjectInfo;

public class QueryAnalizer extends DepthFirstAdapter
{
    public QueryAnalizer(QueryContext context)
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
        if (path.length!=2) {
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
            throw new InvalidQueryException("document's method + ["+method+"] is unsupported");
        }
    }

    @Override
    public void caseAPath(APath node)
    {
        String path[] = splitPath(node.toString());
        ObjectInfo obj = context.getObject(path[0]);
        if (path.length>=2 && obj!=null) {
            obj.addProperty(path[1], node);
        }
    }

    @Override
    public void outAXPath(AXPath node)
    {
        ObjectInfo obj = context.nodeToObject.get(node.getXObjectDecl());
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
            || str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length()-1);
        }
        return str;
    }
}
