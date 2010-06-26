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

import org.xwiki.query.xwql.internal.QueryContext.ObjectInfo;

public class ObjectPrinter
{
    void print(ObjectInfo obj, Printer printer) throws Exception
    {
        if (obj.alias == null) {
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
