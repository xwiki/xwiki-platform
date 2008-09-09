package org.xwiki.query.xwql.hql;

import org.xwiki.query.xwql.QueryContext.ObjectInfo;

public class ObjectPrinter
{
    void print(ObjectInfo obj, Printer printer) {
        if (obj.alias==null) {
            // unnamed object
            obj.alias = printer.getContext().getAliasGenerator().generate("_o");
            printer.from.append(", ").append("BaseObject as ").append(obj.alias);
        }
        printer.where.append(" and ")
            .append(obj.docAlias).append(".fullName=")
            .append(obj.alias).append(".name")
            .append(" and ")
            .append(obj.alias).append(".className=")
            .append("'").append(obj.className).append("'");
    }
}
