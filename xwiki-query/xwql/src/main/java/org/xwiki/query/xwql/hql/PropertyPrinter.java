package org.xwiki.query.xwql.hql;

import org.xwiki.query.jpql.node.APath;
import org.xwiki.query.jpql.node.PPath;
import org.xwiki.query.jpql.node.TId;
import org.xwiki.query.xwql.QueryContext.PropertyInfo;

public class PropertyPrinter
{
    void print(PropertyInfo prop, Printer printer) throws Exception {
        if (prop.isCustomMapped()) {
            // just replace object alias to customMappingAlias
            for (PPath p : prop.locations) {
                p.replaceBy(new APath(new TId(prop.object.customMappingAlias + "." + prop.name)));
            }
        } else {
            // main case
            String className = prop.getType();
            if (className != null) {
                prop.alias = printer.getContext().getAliasGenerator().generate(prop.object.alias + "_" + prop.name);
                printer.from.append(", ")
                    .append(className).append( " as ").append(prop.alias);
                printer.where.append(" and ")
                    .append(prop.alias).append(".id.id=").append(prop.object.alias).append(".id").append(" and ")
                    .append(prop.alias).append(".id.name").append("='").append(prop.name).append("'");
                // rewrite nodes
                for (PPath p : prop.locations) {
                    p.replaceBy(new APath(new TId(prop.alias + "." + prop.getValueField())));
                }
            }
        }
    }
}
