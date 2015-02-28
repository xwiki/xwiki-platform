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
package com.xpn.xwiki.plugin.charts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.radeox.macro.table.Table;
import org.radeox.macro.table.TableBuilder;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.source.TableDataSource;
import com.xpn.xwiki.render.PreTagSubstitution;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.web.Utils;

public class RadeoxHelper
{
    private XWikiRenderer radeoxRenderer;

    private XWikiContext context;

    private XWikiDocument document;

    private static final String TABLE = "{table}";

    public RadeoxHelper(XWikiDocument document, XWikiContext context) throws XWikiException
    {
        this.context = context;
        this.radeoxRenderer = context.getWiki().getRenderingEngine().getRenderer("wiki");
        this.document = document;
    }

    public String getPreRadeoxContent()
    {
        try {
            XWikiRenderingEngine engine =
                Utils.getComponentManager()
                    .getInstance(XWikiRenderingEngine.class, CustomXWikiRenderingEngine.ROLEHINT);

            return engine.renderDocument(document, context);
        } catch (Exception e) {
            return document.getContent(); // this should not happen very often ... i hope
        }
    }

    /**
     * @return The string content of all the the tables in the document
     */
    public String[] getTableStrings()
    {
        ArrayList tables = new ArrayList();
        String content = getPreRadeoxContent();// document.getContent();

        // Remove the content that is inside "{pre}"
        content = (new PreTagSubstitution(context.getUtil(), true)).substitute(content);

        int index = Integer.MIN_VALUE;
        int lastIndex = Integer.MIN_VALUE;
        boolean opened = false;
        while (index < content.length()) {
            lastIndex = index;
            index = content.indexOf(TABLE, index + TABLE.length());
            if (index == -1) {
                break;
            }
            if (opened) {
                tables.add(content.substring(lastIndex + TABLE.length(), index).trim());
            }
            opened = !opened;
        }
        return (String[]) tables.toArray(new String[tables.size()]);
    }

    /**
     * @return All the radeox tables in the document
     */
    public Table[] getTables()
    {
        String[] tableStrings = getTableStrings();
        Table[] tables = new Table[tableStrings.length];
        for (int i = 0; i < tableStrings.length; i++) {
            tables[i] = buildTable(tableStrings[i]);
        }
        return tables;
    }

    /**
     * @return The string content of the given table, or null, when no such table exists
     */
    public String getTableString(int idx)
    {
        String content = getPreRadeoxContent(); // document.getContent();

        // Remove the content that is inside "{pre}"
        content = (new PreTagSubstitution(context.getUtil(), true)).substitute(content);

        int index = Integer.MIN_VALUE;
        int lastIndex = Integer.MIN_VALUE;
        int i = -1;
        boolean opened = false;
        while (index < content.length() && i < idx) {
            lastIndex = index;
            index = content.indexOf(TABLE, index + TABLE.length());
            if (index == -1) {
                break;
            }
            if (opened) {
                i++;
            }
            opened = !opened;
        }
        if (i == idx) {
            return content.substring(lastIndex + TABLE.length(), index).trim();
        } else {
            return null;
        }
    }

    /**
     * @return The radeox table coresponding to the given index, or null, when no such table exists
     */
    public Table getTable(int idx)
    {
        String tableString = getTableString(idx);
        if (tableString != null) {
            return buildTable(tableString);
        } else {
            return null;
        }
    }

    /**
     * @return The HTML representation of the given table-content
     */
    public String getRenderedTable(int idx)
    {
        String tableString = getTableString(idx);
        if (tableString != null) {
            return radeoxRenderer.render("{table}\n" + tableString + "\n{table}", null, null, context);
        } else {
            return null;
        }
    }

    private Table buildTable(String content)
    {
        content = content.trim() + "\n";
        Table table = TableBuilder.build(content);
        table.calc();
        return table;
    }

    public int getTableColumnCount(Table t)
    {
        try {
            return TableDataSource.getTableColumnCount(t);
        } catch (DataSourceException e) {
            return 0;
        }

    }

    public int getTableRowCount(Table t)
    {
        try {
            return TableDataSource.getTableRowCount(t);
        } catch (DataSourceException e) {
            return 0;
        }
    }

    public String getCell(Table table, int columnIndex, int rowIndex)
    {
        try {
            return table.getXY(columnIndex, rowIndex).toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        } catch (Exception ex) {
            return null;
        }
    }

    public static String buildMacro(String name, Map params)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{" + name);
        if (!params.isEmpty()) {
            sb.append(":");
            Iterator it = params.keySet().iterator();
            while (it.hasNext()) {
                String paramName = (String) it.next();
                String paramValue = (String) params.get(paramName);
                sb.append(paramName + "=" + paramValue);
                if (it.hasNext()) {
                    sb.append("|");
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public char getColumn(int columnIndex)
    {
        return (char) ('A' + columnIndex);
    }
}
