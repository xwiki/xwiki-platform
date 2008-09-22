package org.xwiki.query.xwql.hql;

import org.xwiki.query.xwql.QueryContext;
import org.xwiki.query.xwql.QueryContext.ObjectInfo;
import org.xwiki.query.xwql.QueryContext.PropertyInfo;

import org.xwiki.bridge.DocumentAccessBridge;

public class Printer
{
    QueryContext context;
    XWQLtoHQLTranslator parent;

    ObjectPrinter objectPrinter = new ObjectPrinter();
    PropertyPrinter propertyPrinter = new PropertyPrinter();

    StringBuilder from = new StringBuilder();
    StringBuilder where = new StringBuilder();

    public Printer(QueryContext context, XWQLtoHQLTranslator parent)
    {
        this.context = context;
        this.parent = parent;
    }

    QueryContext getContext()
    {
        return context;
    }

    DocumentAccessBridge getAccessBridge()
    {
        return parent.getDocumentAccessBridge();
    }

    public ObjectPrinter getObjectPrinter()
    {
        return objectPrinter;
    }

    public PropertyPrinter getPropertyPrinter()
    {
        return propertyPrinter;
    }

    String print() throws Exception {
        for (ObjectInfo obj : context.getObjects()) {
            getObjectPrinter().print(obj, this);
            for (PropertyInfo prop : obj.properties.values()) {
                getPropertyPrinter().print(prop, this);
            }
        }
        TreePrinter treePrinter = new TreePrinter(this);
        context.getTree().apply(treePrinter);
        return treePrinter.toString();
    }
}
