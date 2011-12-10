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
package com.xpn.xwiki.render.macro;

import java.io.IOException;
import java.io.Writer;

import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.macro.table.Table;

/*
 * Macro for defining and displaying tables. The rows of the table are devided by newlins and the columns are divided by
 * pipe symbols "|". The first line of the table is rendered as column headers. {table} A|B|C 1|2|3 {table} @author
 * stephan @team sonicteam
 * 
 * @version $Id$
 */
public class TableMacro extends BaseLocaleMacro
{
    @Override
    public String getLocaleKey()
    {
        return "macro.table";
    }

    @Override
    public void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException
    {
        String content = params.getContent();

        if (null == content) {
            throw new IllegalArgumentException("TableMacro: missing table content");
        }

        // We need to check for \\ at the end of a line and preserve the white space otherwise we will fail to render
        // the table properly
        if (content.endsWith("\\\\ \n")) {
            content = content.trim() + " ";
        } else {
            content = content.trim();
        }
        content = content + "\n";

        Table table = TableBuilder.build(content);
        table.calc(); // calculate macros like =SUM(A1:A3)
        table.appendTo(writer);
        return;
    }
}
