/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 21:00:05
 */
package com.xpn.xwiki.render;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiMacrosMappingRenderer implements XWikiRenderer {
    private static final Log log = LogFactory.getLog(XWikiMacrosMappingRenderer.class);

    protected HashMap macros_libraries = new HashMap();
    protected HashMap macros_mappings = new HashMap();

    public XWikiMacrosMappingRenderer(XWiki xwiki, XWikiContext context) {
        loadPreferences(xwiki, context);
    }

    public void loadPreferences(XWiki xwiki, XWikiContext context) {
       String[] macrolanguages = StringUtils.split(xwiki.getXWikiPreference("macros_languages", "velocity,groovy", context), ", ");
        for (int i=0;i<macrolanguages.length;i++) {
            String language = macrolanguages[i];
            macros_libraries.put(language, xwiki.getXWikiPreference("macros_" + language, "XWiki." + language.substring(0,1).toUpperCase() + language.substring(1) + "Macros", context));
        }

        String macrosmapping = xwiki.getXWikiPreference("macros_mapping", "", context);
        String[] mappings = StringUtils.split(macrosmapping, "\r\n");
        for (int i=0;i<mappings.length;i++) {
            try {
            XWikiVirtualMacro macro = new XWikiVirtualMacro(mappings[i]);
            macros_mappings.put(macro.getName(), macro);
            } catch (Exception e) {
                log.error("Error reading macro mapping " + mappings[i], e);
            }
        }
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc, XWikiContext context) {
        content = convertSingleLines(content, context);
        content = convertMultiLines(content, context);
        return content;
    }

    private String convertSingleLines(String content, XWikiContext context) {
        StringBuffer result = new StringBuffer();
        String regexp = "\\{(\\w+)(:(.+))?\\}";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(content);
        int current = 0;
        while (m.find()) {
           result.append(content.substring(current, m.start()));
           current = m.end();
           String macroname = m.group(1);
           String params = m.group(3);
           String allcontent = m.group(0);

           XWikiVirtualMacro macro = (XWikiVirtualMacro) macros_mappings.get(macroname);
           if ((macro!=null)&&(macro.isSingleLine()))
            result.append(context.getWiki().getRenderingEngine().convertSingleLine(macroname, params, allcontent, macro, context));
           else
            result.append(allcontent);
        }
        if (current==0)
         return content;

        result.append(content.substring(current));
        return result.toString();
    }

    private String convertMultiLines(String content, XWikiContext context) {
        StringBuffer result = new StringBuffer();
        String regexp = "\\{(\\w+)(:(.+))?\\}(.+?)\\{\\1\\}";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(content);
        int current = 0;
        while (m.find()) {
           result.append(content.substring(current, m.start()));
           current = m.end();
           String macroname = m.group(1);
           String params = m.group(3);
           String data = m.group(4);
           String allcontent = m.group(0);

           XWikiVirtualMacro macro = (XWikiVirtualMacro) macros_mappings.get(macroname);
           if ((macro!=null)&&(macro.isMultiLine()))
            result.append(context.getWiki().getRenderingEngine().convertMultiLine(macroname, params, data, allcontent, macro, context));
           else
            result.append(allcontent);
        }
        if (current==0)
         return content;

        result.append(content.substring(current));
        return result.toString();
    }

    public void flushCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String convertMultiLine(String macroname, String params, String data, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        return allcontent;
    }

    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        return allcontent;
    }
}

