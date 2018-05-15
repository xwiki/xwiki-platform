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

import org.xwiki.query.internal.jpql.node.APath;
import org.xwiki.query.internal.jpql.node.PPath;
import org.xwiki.query.internal.jpql.node.PSelectExpression;
import org.xwiki.query.internal.jpql.node.TId;
import org.xwiki.query.xwql.internal.QueryContext.PropertyInfo;

public class PropertyPrinter
{
    void print(PropertyInfo prop, Printer printer) throws Exception
    {
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
                printer.from.append(", ").append(className).append(" as ").append(prop.alias);
                printer.where.append(" and ").append(prop.alias).append(".id.id=").append(prop.object.alias)
                    .append(".id").append(" and ").append(prop.alias).append(".id.name").append("='").append(prop.name)
                    .append("'");
                // rewrite nodes
                for (PPath p : prop.locations) {
                    String s = prop.alias + "." + prop.getValueField();
                    if (className.endsWith("DBStringListProperty") && p.parent() instanceof PSelectExpression) {
                        s = "elements(" + s + ")";
                    }
                    p.replaceBy(new APath(new TId(s)));
                }
            }
        }
    }
}
