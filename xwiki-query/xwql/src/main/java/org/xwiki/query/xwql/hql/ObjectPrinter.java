package org.xwiki.query.xwql.hql;

import org.xwiki.query.xwql.QueryContext.ObjectInfo;

public class ObjectPrinter
{
    void print(ObjectInfo obj, Printer printer) throws Exception {
        if (obj.alias==null) {
            // unnamed object
            obj.alias = printer.getContext().getAliasGenerator().generate("_o");
            printer.from.append(", BaseObject as ").append(obj.alias);
        }
        // join with the document
        printer.where.append(" and ")
            .append(obj.docAlias).append(".fullName=").append(obj.alias).append(".name");
        // className constraint
        if (obj.isCustomMapped()) {
            obj.customMappingAlias = printer.getContext().getAliasGenerator().generate(obj.alias + "CM");
            printer.from.append(", ")
                .append(obj.className).append(" as ").append(obj.customMappingAlias);
            printer.where.append(" and ")
                .append(obj.alias).append(".id=").append(obj.customMappingAlias).append(".id");
        } else {
            // main case
            printer.where.append(" and ")
                .append(obj.alias).append(".className=").append("'").append(obj.className).append("'");
        }
    }
}
