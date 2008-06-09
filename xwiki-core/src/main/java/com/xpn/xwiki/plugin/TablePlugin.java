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
 *
 */

package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class TablePlugin extends XWikiDefaultPlugin {

    // Default Parameters
    public String m_table_border = "1";
    public String m_table_id = "xwikitableid";
    public String m_table_class = "xwikitableclass";
    public String m_cell_padding = "1";
    public String m_cell_spacing = "1";
    public String m_header_bg = "#DDDDDD";
    public String m_data_bg = "#CCCCCC";
    public String m_data_align = "center";
    public String m_header_align = "center";
    public String m_valign = "center";


    public TablePlugin (String name, String className, XWikiContext context) {
        super(name, className, context);
        init(context);
    }

    public Map getParams(XWikiContext context) {
        Map params = (Map)context.get("TablePluginParams");
        if (params==null) {
            params = new HashMap();
            context.put("TablePluginParams", params);
            params.put("table_border", m_table_border);
            params.put("table_id", m_table_id);
            params.put("table_class", m_table_class);
            params.put("cell_padding", m_cell_padding);
            params.put("cell_spacing", m_cell_spacing);
            params.put("header_bg", m_header_bg);
            params.put("data_bg", m_data_bg);
            params.put("data_align", m_data_align);
            params.put("header_align", m_header_align);
            params.put("valign", m_valign);
            params.put("inside_table", "0");
            params.put("current_table", new Vector());
        }
        return params;
    }

    public void init(XWikiContext context) {

        try {

            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument("Plugins","TablePlugin", context);
            BaseObject pluginconf = (BaseObject) doc.getxWikiObject();

            if (pluginconf!=null) {
                if (pluginconf.get("tableborder") != null)
                    m_table_border = pluginconf.get("tableborder").toString();

                if (pluginconf.get("tableid") != null)
                    m_table_id = pluginconf.get("tableid").toString();

                if (pluginconf.get("tableclass") != null)
                    m_table_class = pluginconf.get("tableclass").toString();

                if (pluginconf.get("cellspacing") != null)
                    m_cell_spacing = pluginconf.get("cellspacing").toString();

                if (pluginconf.get("cellpadding") != null)
                    m_cell_padding = pluginconf.get("cellpadding").toString();

                if (pluginconf.get("headerbg") != null)
                    m_header_bg = pluginconf.get("headerbg").toString();

                if (pluginconf.get("databg") != null)
                    m_data_bg = pluginconf.get("databg").toString();

                if (pluginconf.get("headeralign") != null)
                    m_header_align = pluginconf.get("headeralign").toString();

                if (pluginconf.get("dataalign") != null)
                    m_data_align = pluginconf.get("dataalign").toString();

                if (pluginconf.get("valign") != null)
                    m_valign = pluginconf.get("valign").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String commonTagsHandler(String line, XWikiContext context) {
        return line;
    }

    public String startRenderingHandler(String line, XWikiContext context) {
        return line;
    }

    public String outsidePREHandler(String line, XWikiContext context) {
        Util util = context.getUtil();

        if (util.match("/%TABLE{(.*)}%/",line)) {
            override(util.substitute("s/%TABLE{(.*)}%/$1/go",line),context);
            return "";
        }

        Map params = getParams(context);
        if(util.match("/^(\\s*)\\|.*\\|\\s*$/",line)) {
            processTR(line,context);
            params.put("inside_table", "1");
            return "";
        } else {
            if(params.get("inside_table").equals("1")) {
                params.put("inside_table", "0");
                return emitTable(params) + line;
            }
         }
        return line;
    }

    public String insidePREHandler(String line, XWikiContext context) {
        return line;
    }

    public String endRenderingHandler(String line, XWikiContext context) {
        Map params = getParams(context);
        if (params.get("inside_table").equals("1")) {
            params.put("inside_table", "0");
            line = emitTable(params) + line;
        }
        return line;
    }


    public void override (String text,
                          XWikiContext context) {

        // Reinit params
        context.remove("TablePluginParams");
        Map params = getParams(context);

        if (extractNameValuePair("tableborder",text,context) != null)
            params.put("table_border", extractNameValuePair("tableborder",text,context).toString());

        if (extractNameValuePair("tableid",text,context) != null)
            params.put("table_id", extractNameValuePair("tableid",text,context).toString());

        if (extractNameValuePair("tableclass",text,context) != null)
            params.put("table_class", extractNameValuePair("tableclass",text,context).toString());

        if (extractNameValuePair("cellspacing",text,context) != null)
            params.put("cell_spacing", extractNameValuePair("cellspacing",text,context).toString());

        if (extractNameValuePair("cellpadding",text,context) != null)
            params.put("cell_padding", extractNameValuePair("cellpadding",text,context).toString());

        if (extractNameValuePair("headerbg",text,context) != null)
            params.put("header_bg", extractNameValuePair("headerbg",text,context).toString());

        if (extractNameValuePair("databg",text,context) != null)
            params.put("data_bg", extractNameValuePair("databg",text,context).toString());

        if (extractNameValuePair("headeralign",text,context) != null)
            params.put("header_align", extractNameValuePair("headeralign",text,context).toString());

        if (extractNameValuePair("dataalign",text,context) != null)
            params.put("data_align", extractNameValuePair("dataalign",text,context).toString());

        if (extractNameValuePair("valign",text,context) != null)
            params.put("valign", extractNameValuePair("valign",text,context).toString());

    }

    public String extractNameValuePair(String name,
                                       String text,
                                       XWikiContext context) {
        Util util = context.getUtil();
        String pattern = "/(.*)" + name + "\\s*=\\s*\"([^\"]*)\"(.*)/";
        if (util.match(pattern,text)) {
            text = util.substitute("s" + pattern + "$2/go",text);
            return text;
        }
        return null;
    }

    public void processTR (String line, XWikiContext context) {

        Util util = context.getUtil();
        Vector row = null;
        row = util.split("/\\|/",line);
        row.removeElementAt(0);
        Vector current_table = (Vector) getParams(context).get("current_table");
        current_table.addElement(row);
    }


    public String emitTable (Map params)  {
        Vector current_table = (Vector) params.get("current_table");
        StringBuffer text = new StringBuffer();
        text.append("<table border=\"" + params.get("table_border")
                + "\" id=\"" + params.get("table_id")
                + "\" class=\"" + params.get("table_class")
                + "\" cellspacing=\"" + params.get("cell_spacing")
                + "\" cellpadding=\"" + params.get("cell_padding") + "\">\n");
        for (int i = 0;i<current_table.size();i++) {
            Vector row = (Vector) current_table.elementAt(i);
            text.append("<tr>");
            String bg_color = (String) params.get("data_bg");
            if (i==0)
                bg_color= (String) params.get("header_bg");

            for (int j=0;j<row.size();j++) {
                String cell = (String) row.elementAt(j);
                text.append("<td bgcolor=\"" + bg_color + "\">" + cell + "</td>\n");
            }
            text.append("</tr>\n");
        }
        text.append("</table>\n");
        current_table = new Vector();
        params.put("current_table", current_table);
        return text.toString();
    }
}







