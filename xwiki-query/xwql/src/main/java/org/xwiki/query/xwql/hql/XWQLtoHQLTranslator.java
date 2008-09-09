package org.xwiki.query.xwql.hql;

import org.xwiki.query.jpql.JPQLParser;
import org.xwiki.query.jpql.node.Start;
import org.xwiki.query.xwql.QueryAnalizer;
import org.xwiki.query.xwql.QueryContext;
import org.xwiki.query.xwql.QueryTranslator;
import org.xwiki.query.Query;

import org.xwiki.bridge.DocumentAccessBridge;

public class XWQLtoHQLTranslator implements QueryTranslator
{
    protected DocumentAccessBridge documentAccessBridge;

    public String translate(String input) throws Exception
    {
        input = input.trim();
        String lcInput = input.toLowerCase();
        String addition = "select doc.fullName from Document as doc ";
        if (lcInput.startsWith("where") | lcInput.startsWith("order") || lcInput.length()==0) {
            input = addition + input;
        } else if (lcInput.startsWith("from")) {
            input = addition + "," + input.substring(4);
        }
        JPQLParser parser = new JPQLParser();
        Start tree = parser.parse(input);
        QueryContext context = new QueryContext(tree);
        // analize query and store info in context
        tree.apply(new QueryAnalizer(context));

        Printer printer = getPrinter(context);
        return printer.print();
    }

    public String getOutputLanguage()
    {
        return Query.HQL;
    }

    protected Printer getPrinter(QueryContext context)
    {
        return new Printer(context, this);
    }

    public DocumentAccessBridge getDocumentAccessBridge() {
        return documentAccessBridge;
    }
}
