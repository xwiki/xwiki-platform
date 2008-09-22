package org.xwiki.query.xwql.hql;

import org.xwiki.query.jpql.analysis.DepthFirstAdapter;
import org.xwiki.query.jpql.node.*;
import org.xwiki.query.xwql.QueryContext;

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

    protected QueryContext getContext() {
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
        if (node.getWhereClause()==null) {
            // needed for #outAWhereClause
            node.setWhereClause(new AWhereClause());
        } else if (printer.where.length()>0){
            AWhereClause where = (AWhereClause) node.getWhereClause();
            // where := ( where )
            where.setConditionalExpression(
                new ASingleConditionalExpression(
                    new ASingleConditionalTerm(
                        new AConditionalFactor(null,
                            new ABrConditionalPrimary(new TLbr(), 
                                where.getConditionalExpression(),
                                new TRbr() )))));
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
        }
        builder.append(' ').append(from)
            .append(" as ").append(alias);
    }

    @Override
    public void outAFromClause(AFromClause node)
    {
        String from = getPrinter().from.toString();
        if (from.length()>0) {
            builder.append(' ').append( from );
        }
    }

    @Override
    public void outAWhereClause(AWhereClause node)
    {
        if (getPrinter().where.length()>0) {
            if (node.getWhere()==null) {
                builder.append(" WHERE 1=1 ");
            }
            builder.append(getPrinter().where.toString());
        }
    }
}
