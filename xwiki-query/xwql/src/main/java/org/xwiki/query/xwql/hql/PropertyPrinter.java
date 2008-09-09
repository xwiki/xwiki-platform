package org.xwiki.query.xwql.hql;

import org.xwiki.query.xwql.QueryContext.PropertyInfo;

public class PropertyPrinter
{
    void print(PropertyInfo prop, Printer printer) throws Exception {
        String className = getPropertyStoreClassName(prop.object.className, prop.name, printer);
        if (className != null) {
            prop.alias = printer.getContext().getAliasGenerator().generate(prop.object.alias + "_" + prop.name);
            printer.from.append(", ")
                .append(className).append( " as ").append(prop.alias);
            printer.where.append(" and ")
                .append(prop.alias).append(".id.id=").append(prop.object.alias).append(".id").append(" and ")
                .append(prop.alias).append(".id.name").append("='").append(prop.name).append("'");
        }
    }

    protected String getPropertyStoreClassName(String clas, String prop, Printer printer) throws Exception {
        if (printer.getAccessBridge() == null) {
            return null;
        }
        return printer.getAccessBridge().getPropertyType(clas, prop);
    }
}
